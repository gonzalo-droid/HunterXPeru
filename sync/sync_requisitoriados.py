# /// script
# requires-python = ">=3.12,<3.14"
# dependencies = [
#   "requests>=2.32",
#   "pillow>=11",
#   "google-cloud-storage>=2.18",
# ]
# ///
"""
Sincronización semanal: API del Programa de Recompensas → Firebase Storage.

Corre en una Mac en Perú (la API no responde a servidores fuera del país, ver docs/DECISIONES.md D15).
Publica en el bucket:
  data/manifest.json          todos los perfiles, sin fotos (la app compara `dataHash`)
  data/thumbs-v<N>.zip        todas las miniaturas, para instalaciones nuevas
  thumbs/<hash>-<sha>.jpg     miniatura por perfil (nombre con hash de contenido: caché inmutable)
  photos/<hash>-<sha>.jpg     foto de la ficha

Uso:
  uv run sync/sync_requisitoriados.py --dry-run --out /tmp/cz-sync     # sin subir nada
  uv run sync/sync_requisitoriados.py --bucket <proyecto>.firebasestorage.app
"""

from __future__ import annotations

import argparse
import base64
import hashlib
import io
import json
import logging
import os
import subprocess
import sys
import time
import zipfile
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path

import requests
from PIL import Image, ImageOps

API = "https://sispasvehapp.mininter.gob.pe/api-recompensas"
USER_AGENT = "CazadoresPeru/2.0.0 (sync)"
PAGE_SIZE = 200
DETAIL_PAUSE = 4.0          # segundos entre fichas: con ~3 por segundo el servidor bloqueó la IP tras ~200
MAX_CONSECUTIVE_FAILURES = 3
MAX_REMOVED_RATIO = 0.30    # más bajas que esto en una semana huele a falla de la API, no a capturas
THUMB_SIZE = 240
PHOTO_MAX = 720
SCHEMA = 1
# versionCode mínimo que puede leer el padrón (app: PadronStore.updateRequired). Subirlo junto con SCHEMA cuando el
# formato cambie de forma incompatible: los builds anteriores muestran "Actualiza la app" en vez de quedarse con datos viejos.
MIN_APP_VERSION_CODE = 0

DEFAULT_STATE = Path.home() / "Library/Application Support/CazadoresSync"
DEFAULT_CREDENTIALS = Path.home() / ".config/cazadores-sync/service-account.json"

log = logging.getLogger("sync")


class SyncAborted(Exception):
    """La sincronización se detuvo sin publicar nada; la versión anterior sigue vigente."""


# ---------------------------------------------------------------- API

session = requests.Session()
session.headers.update({"User-Agent": USER_AGENT, "Accept": "application/json"})


def _request(method: str, path: str, **kwargs) -> requests.Response:
    last: Exception | None = None
    for attempt in range(4):
        try:
            response = session.request(method, f"{API}/{path}", timeout=(20, 120), **kwargs)
            if response.status_code < 500:
                return response
            last = RuntimeError(f"HTTP {response.status_code} en {path}")
        except requests.RequestException as error:
            last = error
        time.sleep(5 * 2**attempt)
    raise SyncAborted(f"La API no respondió ({path}): {last}")


def fetch_list() -> list[dict]:
    """Todos los perfiles. La paginación de la petición es 1-indexed (page 0 → HTTP 500)."""
    profiles: list[dict] = []
    page = 1
    while True:
        body = {
            "pageInfo": {"page": page, "size": PAGE_SIZE, "sortBy": "idRequisitoriado", "direction": "DESC"},
            "search": {"tipoFilter": "F"},
        }
        response = _request("POST", "requisitoriados/pageandfilter", json=body)
        response.raise_for_status()
        data = response.json()
        profiles.extend(data.get("content") or [])
        log.info("Lista: página %s/%s (%s perfiles)", page, data.get("totalPages"), len(profiles))
        if data.get("last", True) or not data.get("content"):
            total = data.get("totalElements", len(profiles))
            if len(profiles) != total:
                raise SyncAborted(f"La API anunció {total} perfiles pero entregó {len(profiles)}")
            return profiles
        page += 1


def fetch_detail(profile_hash: str) -> dict | None:
    response = _request("GET", f"requisitoriados/{profile_hash}")
    if response.status_code == 404:
        return None
    response.raise_for_status()
    return response.json()



# ---------------------------------------------------------------- imágenes


def decode_photo(raw: str | None) -> bytes | None:
    if not raw:
        return None
    payload = raw.split("base64,", 1)[-1]
    payload = "".join(payload.split())
    payload += "=" * (-len(payload) % 4)
    try:
        data = base64.b64decode(payload)
    except ValueError:
        return None
    return data or None


def to_jpeg(data: bytes, max_side: int, quality: int) -> bytes | None:
    try:
        image = Image.open(io.BytesIO(data))
        image = ImageOps.exif_transpose(image).convert("RGB")
    except Exception:  # noqa: BLE001 — una foto rota no debe tumbar la sincronización
        return None
    image.thumbnail((max_side, max_side), Image.Resampling.LANCZOS)
    out = io.BytesIO()
    image.save(out, "JPEG", quality=quality, optimize=True, progressive=True)
    return out.getvalue()


def short_sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()[:12]


# ---------------------------------------------------------------- estado


@dataclass
class Store:
    root: Path

    @property
    def state_file(self) -> Path:
        return self.root / "state.json"

    @property
    def files(self) -> Path:
        return self.root / "files"

    def load(self) -> dict:
        if self.state_file.exists():
            return json.loads(self.state_file.read_text())
        return {"version": 0, "dataHash": None, "profiles": {}}

    def save(self, state: dict) -> None:
        tmp = self.state_file.with_suffix(".tmp")
        tmp.write_text(json.dumps(state, ensure_ascii=False, indent=1))
        tmp.replace(self.state_file)

    @property
    def pending_file(self) -> Path:
        return self.root / "pending.json"

    def load_pending(self) -> dict:
        return json.loads(self.pending_file.read_text()) if self.pending_file.exists() else {}

    def save_pending(self, pending: dict) -> None:
        tmp = self.pending_file.with_suffix(".tmp")
        tmp.write_text(json.dumps(pending, ensure_ascii=False))
        tmp.replace(self.pending_file)

    def clear_pending(self) -> None:
        self.pending_file.unlink(missing_ok=True)

    def write_file(self, key: str, data: bytes) -> None:
        path = self.files / key
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)

    def read_file(self, key: str) -> bytes:
        return (self.files / key).read_bytes()

    def has_file(self, key: str) -> bool:
        return (self.files / key).exists()


def list_signature(item: dict) -> str:
    """Si cambia, se vuelve a pedir la ficha (recompensa, delito, nombre o foto distintos)."""
    raw = "|".join(
        str(item.get(k)) for k in ("nombreCompleto", "montoRecompensa", "delito", "idRequisitoriado")
    ) + "|" + hashlib.sha1((item.get("foto") or "").encode()).hexdigest()
    return hashlib.sha1(raw.encode()).hexdigest()


# ---------------------------------------------------------------- sincronización


def make_record(store: Store, item: dict, detail: dict) -> dict:
    h = item["hashRequisitoriado"]
    record = {
        "id": item.get("idRequisitoriado"),
        "hash": h,
        "nombreCompleto": item.get("nombreCompleto") or "",
        "apellidoPaterno": detail.get("apellidoPaterno") or "",
        "apellidoMaterno": detail.get("apellidoMaterno") or "",
        "nombres": detail.get("nombres") or "",
        "sexo": detail.get("sexo"),
        "montoRecompensa": item.get("montoRecompensa") or detail.get("montoRecompensa") or 0,
        "delito": item.get("delito") or "",
        "delitos": detail.get("delitos") or ([item["delito"]] if item.get("delito") else []),
        "departamento": detail.get("departamento"),
        "provincia": detail.get("provincia"),
        "thumb": None,
        "photo": None,
    }
    raw = decode_photo(detail.get("foto")) or decode_photo(item.get("foto"))
    if raw:
        photo = to_jpeg(raw, PHOTO_MAX, 82)
        thumb = to_jpeg(raw, THUMB_SIZE, 72)
        if photo and thumb:
            record["photo"] = f"photos/{h}-{short_sha(photo)}.jpg"
            record["thumb"] = f"thumbs/{h}-{short_sha(thumb)}.jpg"
            for key, data in ((record["photo"], photo), (record["thumb"], thumb)):
                if not store.has_file(key):
                    store.write_file(key, data)
    return record


def build(store: Store, state: dict) -> tuple[dict, list[str]]:
    """Devuelve (nuevo estado, claves de archivos a borrar).

    Las fichas se piden de a una y con pausa: con ráfagas, el servidor bloquea la IP unos minutos.
    El avance se guarda en `pending.json`; si la API deja de responder, la próxima ejecución sigue
    desde ahí en vez de empezar de cero.
    """
    listing = fetch_list()
    if not listing:
        raise SyncAborted("La API devolvió 0 perfiles")

    previous: dict = state["profiles"]
    current_hashes = {item["hashRequisitoriado"] for item in listing if item.get("hashRequisitoriado")}
    removed = [h for h in previous if h not in current_hashes]
    if previous and len(removed) / len(previous) > MAX_REMOVED_RATIO:
        raise SyncAborted(
            f"Desaparecieron {len(removed)} de {len(previous)} perfiles: se asume falla de la API"
        )

    pending: dict = store.load_pending()
    to_fetch = [
        item for item in listing
        if not _is_current(previous.get(item["hashRequisitoriado"]), item)
        and not _is_current(pending.get(item["hashRequisitoriado"]), item)
    ]
    log.info(
        "Perfiles: %s · fichas por pedir: %s (ya avanzadas: %s) · bajas: %s",
        len(listing), len(to_fetch), len(pending), len(removed),
    )
    if to_fetch:
        log.info("Tiempo estimado: ~%s min", round(len(to_fetch) * (DETAIL_PAUSE + 1) / 60))

    consecutive_failures = 0
    for index, item in enumerate(to_fetch, 1):
        h = item["hashRequisitoriado"]
        time.sleep(DETAIL_PAUSE)
        try:
            detail = fetch_detail(h) or {}
        except (SyncAborted, requests.RequestException) as error:
            consecutive_failures += 1
            log.warning("Ficha %s sin respuesta: %s", h, error)
            if consecutive_failures >= MAX_CONSECUTIVE_FAILURES:
                store.save_pending(pending)
                raise SyncAborted(
                    f"La API dejó de responder tras {index - 1} fichas; se sigue en la próxima ejecución"
                ) from error
            continue
        consecutive_failures = 0
        pending[h] = {"listSig": list_signature(item), "record": make_record(store, item, detail)}
        if index % 20 == 0:
            store.save_pending(pending)
            log.info("Fichas: %s/%s", index, len(to_fetch))
    store.save_pending(pending)

    profiles: dict = {}
    for item in listing:
        h = item["hashRequisitoriado"]
        for source in (pending, previous):
            if _is_current(source.get(h), item):
                profiles[h] = source[h]
                break
        else:
            # Ficha que falló suelta: se publica con los datos de la lista y se reintenta después.
            profiles[h] = {"listSig": "", "record": make_record(store, item, {})}

    live_keys = _image_keys(profiles)
    stale = sorted(_image_keys(previous) - live_keys)
    return {**state, "profiles": profiles}, stale


def _is_current(entry: dict | None, item: dict) -> bool:
    return bool(entry) and entry["listSig"] == list_signature(item)


def _image_keys(profiles: dict) -> set[str]:
    return {p["record"][k] for p in profiles.values() for k in ("photo", "thumb") if p["record"][k]}


def manifest_for(state: dict, version: int, zip_key: str) -> tuple[dict, str]:
    records = sorted((p["record"] for p in state["profiles"].values()), key=lambda r: r["id"] or 0, reverse=True)
    data_hash = hashlib.sha256(json.dumps(records, sort_keys=True, ensure_ascii=False).encode()).hexdigest()[:16]
    manifest = {
        "schema": SCHEMA,
        "version": version,
        "dataHash": data_hash,
        "generatedAt": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "source": "https://recompensas.pe",
        "count": len(records),
        "thumbsZip": zip_key,
        "minAppVersionCode": MIN_APP_VERSION_CODE,
        "criminals": records,
    }
    return manifest, data_hash


def build_zip(store: Store, state: dict) -> bytes:
    buffer = io.BytesIO()
    with zipfile.ZipFile(buffer, "w", zipfile.ZIP_STORED) as archive:  # JPEG ya viene comprimido
        for profile in state["profiles"].values():
            key = profile["record"]["thumb"]
            if key:
                archive.writestr(key, store.read_file(key))
    return buffer.getvalue()


# ---------------------------------------------------------------- publicación


class Publisher:
    IMMUTABLE = "public, max-age=31536000, immutable"
    SHORT = "public, max-age=300"

    def __init__(self, bucket: str | None, credentials: Path, out: Path | None):
        self.out = out
        self.bucket = None
        if out is None:
            from google.cloud import storage  # solo se carga al publicar de verdad

            client = storage.Client.from_service_account_json(str(credentials))
            self.bucket = client.bucket(bucket)

    def put(self, key: str, data: bytes, content_type: str, cache: str) -> None:
        if self.out is not None:
            path = self.out / key
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(data)
            return
        blob = self.bucket.blob(key)
        blob.cache_control = cache
        blob.upload_from_string(data, content_type=content_type)

    def delete(self, key: str) -> None:
        if self.out is not None:
            (self.out / key).unlink(missing_ok=True)
            return
        blob = self.bucket.blob(key)
        if blob.exists():
            blob.delete()


def run(args: argparse.Namespace) -> None:
    store = Store(args.state_dir)
    store.files.mkdir(parents=True, exist_ok=True)
    state = store.load()

    # launchd la lanza dos veces al día (la Mac puede estar apagada el día fijo); solo trabaja
    # si la última publicación tiene más de --min-age-days.
    published = state.get("publishedAt")
    if published and not args.force:
        age = datetime.now(timezone.utc) - datetime.fromisoformat(published)
        if age.total_seconds() < args.min_age_days * 86400:
            log.info("Última publicación hace %.1f días: nada que hacer", age.total_seconds() / 86400)
            return

    new_state, stale = build(store, state)
    _, data_hash = manifest_for(new_state, state["version"], "")
    if data_hash == state.get("dataHash") and not args.force:
        store.clear_pending()
        log.info("Sin cambios desde la versión %s: no se publica nada", state["version"])
        return

    version = state["version"] + 1
    zip_key = f"data/thumbs-v{version}.zip"
    manifest, data_hash = manifest_for(new_state, version, zip_key)

    publisher = Publisher(args.bucket, args.credentials, args.out)
    # Primero las imágenes que el bucket aún no tiene, al final el manifest: la app nunca ve rutas rotas.
    live_keys = _image_keys(new_state["profiles"])
    to_upload = sorted(live_keys - set(state.get("uploaded", [])))
    for index, key in enumerate(to_upload, 1):
        publisher.put(key, store.read_file(key), "image/jpeg", Publisher.IMMUTABLE)
        if index % 100 == 0:
            log.info("Subidas %s/%s imágenes", index, len(to_upload))
    publisher.put(zip_key, build_zip(store, new_state), "application/zip", Publisher.IMMUTABLE)
    publisher.put(
        "data/manifest.json",
        json.dumps(manifest, ensure_ascii=False, separators=(",", ":")).encode(),
        "application/json; charset=utf-8",
        Publisher.SHORT,
    )

    # Bajas y zips viejos después del manifest. Se conserva el zip anterior una semana más
    # por si una app empezó a descargarlo justo antes.
    for key in stale:
        publisher.delete(key)
        (store.files / key).unlink(missing_ok=True)
    older_zip = state.get("previousThumbsZip")
    if older_zip:
        publisher.delete(older_zip)

    new_state.update(
        version=version,
        dataHash=data_hash,
        thumbsZip=zip_key,
        previousThumbsZip=state.get("thumbsZip"),
        publishedAt=manifest["generatedAt"],
        uploaded=sorted(live_keys),
    )
    store.save(new_state)
    store.clear_pending()
    log.info(
        "Publicada versión %s: %s perfiles, %s imágenes nuevas, %s borradas",
        version, manifest["count"], len(to_upload), len(stale),
    )


def notify_failure(message: str) -> None:
    """Aviso de macOS: la tarea corre sin terminal y nadie mira el log."""
    script = f'display notification {json.dumps(message[:180])} with title "Cazadores Perú: sincronización fallida"'
    subprocess.run(["osascript", "-e", script], check=False, capture_output=True)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--bucket", default=os.environ.get("CZ_SYNC_BUCKET"))
    parser.add_argument("--credentials", type=Path, default=Path(os.environ.get("CZ_SYNC_CREDENTIALS", DEFAULT_CREDENTIALS)))
    parser.add_argument("--state-dir", type=Path, default=Path(os.environ.get("CZ_SYNC_STATE", DEFAULT_STATE)))
    parser.add_argument("--dry-run", action="store_true", help="no sube nada; escribe en --out")
    parser.add_argument("--out", type=Path, help="carpeta de salida para --dry-run")
    parser.add_argument("--force", action="store_true", help="sincroniza ya y publica aunque no haya cambios")
    parser.add_argument("--min-age-days", type=float, default=6, help="no sincroniza si la última publicación es más reciente")
    args = parser.parse_args()

    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
    if args.dry_run:
        if not args.out:
            parser.error("--dry-run necesita --out")
    else:
        args.out = None
        if not args.bucket:
            parser.error("falta --bucket (o CZ_SYNC_BUCKET)")
        if not args.credentials.exists():
            parser.error(f"no existe la cuenta de servicio en {args.credentials}")

    try:
        run(args)
    except SyncAborted as error:
        log.error("Sincronización detenida: %s", error)
        notify_failure(str(error))
        return 2
    except Exception as error:  # noqa: BLE001
        log.exception("Error inesperado")
        notify_failure(f"{type(error).__name__}: {error}")
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
