package pe.cazadores.testing

/** Respuestas reales de la API (2026-11-16) recortadas: fotos reemplazadas por Base64 corto. */
object ApiFixtures {
    val SEARCH_PAGE_1 = """{"content": [{"id": 649, "idRequisitoriado": 6229, "hashRequisitoriado": "D1321FADB4B9D70B17504891006156FC", "nombreCompleto": "nemesio agustin huayta", "foto": "/9j/4AAQSkZJRg==", "montoRecompensa": 20000.0, "montoRecompensaSpace": "20 000", "delito": "VIOLACIÓN SEXUAL"}, {"id": 648, "idRequisitoriado": 5990, "hashRequisitoriado": "F2644BFD6BFE7D8E7231B21560374032", "nombreCompleto": "segundo euclides atachi felix", "foto": "/9j/4AAQSkZJRg==", "montoRecompensa": 50000.0, "montoRecompensaSpace": "50 000", "delito": "HOMICIDIO CALIFICADO - ASESINATO"}], "pageable": {"sort": {"sorted": true, "unsorted": false, "empty": false}, "pageSize": 3, "pageNumber": 0, "offset": 0, "paged": true, "unpaged": false}, "totalElements": 649, "last": false, "totalPages": 217, "sort": {"sorted": true, "unsorted": false, "empty": false}, "numberOfElements": 3, "first": true, "size": 3, "number": 0, "empty": false}"""

    val DETAIL = """{"idRequisitoriado": 6229, "hashRequisitoriado": "D1321FADB4B9D70B17504891006156FC", "apellidoPaterno": "agustin", "apellidoMaterno": "huayta", "nombres": "nemesio", "sexo": 1, "montoRecompensa": 20000.0, "montoRecompensaSpace": "20 000", "foto": "data:image/jpeg;base64,/9j/4AAQSkZJRg==", "delitos": ["VIOLACIÓN SEXUAL"], "departamento": "UCAYALI", "provincia": "CORONEL PORTILLO"}"""

    val TOP5 = """[{"id": 442, "idRequisitoriado": 4271, "hashRequisitoriado": "AC7B3EF3F2799D5C44A5724E77703FF1", "nombreCompleto": "gonzalo eduardo monteverde bussalleu", "foto": null, "montoRecompensa": 500000.0, "montoRecompensaSpace": "500 000", "delito": "LAVADO DE ACTIVOS"}]"""

    const val NOT_FOUND = """{"timestamp":"2026-11-16T22:25:07.012+00:00","message":"Datos no válidos."}"""
}
