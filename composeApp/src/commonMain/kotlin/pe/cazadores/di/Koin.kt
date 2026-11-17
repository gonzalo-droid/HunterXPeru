package pe.cazadores.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import pe.cazadores.core.config.AppEnvironment
import pe.cazadores.data.local.AppDatabase
import pe.cazadores.data.local.SettingsDataSource
import pe.cazadores.data.remote.ApiJson
import pe.cazadores.data.remote.RecompensasApi
import pe.cazadores.data.remote.createHttpClient
import pe.cazadores.data.repository.BundledCatalogSource
import pe.cazadores.data.repository.CatalogRepositoryImpl
import pe.cazadores.data.repository.CriminalRepositoryImpl
import pe.cazadores.data.repository.FavoritesRepositoryImpl
import pe.cazadores.data.repository.SettingsRepositoryImpl
import pe.cazadores.domain.repository.CatalogRepository
import pe.cazadores.domain.repository.CriminalRepository
import pe.cazadores.domain.repository.FavoritesRepository
import pe.cazadores.domain.repository.SettingsRepository
import pe.cazadores.domain.repository.TimeProvider
import pe.cazadores.domain.usecase.GetCatalogsUseCase
import pe.cazadores.domain.usecase.GetCriminalDetailUseCase
import pe.cazadores.domain.usecase.GetHomeStatsUseCase
import pe.cazadores.domain.usecase.GetTopWantedUseCase
import pe.cazadores.domain.usecase.IsFavoriteUseCase
import pe.cazadores.domain.usecase.ObserveFavoritesUseCase
import pe.cazadores.domain.usecase.SearchCriminalsUseCase
import pe.cazadores.domain.usecase.ToggleFavoriteUseCase
import pe.cazadores.resources.Res
import pe.cazadores.ui.app.AppViewModel
import pe.cazadores.ui.screens.detail.DetailViewModel
import pe.cazadores.ui.screens.saved.SavedViewModel
import pe.cazadores.ui.screens.settings.SettingsViewModel
import pe.cazadores.ui.screens.home.HomeViewModel
import pe.cazadores.ui.screens.search.SearchViewModel
import kotlin.time.Clock

/** Provee: AppEnvironment, HttpClientEngine, AppDatabase, DataStore y NetworkMonitor. */
expect val platformModule: Module

val APP_SCOPE = named("appScope")

val dataModule = module {
    single(APP_SCOPE) { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single { ApiJson }
    single<TimeProvider> { TimeProvider { Clock.System.now().toEpochMilliseconds() } }
    single {
        val env = get<AppEnvironment>()
        createHttpClient(engine = get(), userAgent = env.userAgent, debug = env.isDebug)
    }
    single { RecompensasApi(get()) }
    single { get<AppDatabase>().favoriteDao() }
    single { get<AppDatabase>().cacheDao() }
    single { SettingsDataSource(get()) }
    @OptIn(ExperimentalResourceApi::class)
    single { BundledCatalogSource { name -> Res.readBytes("files/catalogs/$name.json").decodeToString() } }

    single<CriminalRepository> { CriminalRepositoryImpl(get(), get(), get(), get(), get()) }
    single<FavoritesRepository> { FavoritesRepositoryImpl(get(), get(), get()) }
    single<CatalogRepository> { CatalogRepositoryImpl(get(), get(), get(), get(), get()) }
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
}

val domainModule = module {
    factory { GetTopWantedUseCase(get()) }
    factory { SearchCriminalsUseCase(get()) }
    factory { GetCriminalDetailUseCase(get(), get()) }
    factory { GetHomeStatsUseCase(get()) }
    factory { GetCatalogsUseCase(get()) }
    factory { ObserveFavoritesUseCase(get()) }
    factory { IsFavoriteUseCase(get()) }
    factory { ToggleFavoriteUseCase(get(), get(), get(APP_SCOPE)) }
}

val uiModule = module {
    viewModelOf(::SearchViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::SavedViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::AppViewModel)
    viewModel { (hash: String) -> DetailViewModel(hash, get(), get(), get(), get()) }
}

fun appModules(): List<Module> = listOf(platformModule, dataModule, domainModule, uiModule)

/** Arranque único de Koin. Android lo llama desde `Application`; iOS desde `iOSApp.swift`. */
fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)
        modules(appModules())
    }
}

/** Arranque desde Swift: recibe el puente de anuncios implementado en iosApp. */
fun doInitKoinIos(extraModules: List<Module>) = initKoin { modules(extraModules) }
