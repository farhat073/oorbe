package com.oorbitt.launcher.search.di

import com.oorbitt.launcher.search.SearchAggregator
import com.oorbitt.launcher.search.SearchProvider
import com.oorbitt.launcher.search.provider.AppSearchProvider
import com.oorbitt.launcher.search.provider.CalculatorProvider
import com.oorbitt.launcher.search.provider.ContactsProvider
import com.oorbitt.launcher.search.provider.SettingsShortcutProvider
import com.oorbitt.launcher.search.provider.WebSearchFallbackProvider
import com.oorbitt.launcher.search.provider.UnitConverterProvider
import com.oorbitt.launcher.search.provider.WhatsAppProvider
import org.koin.dsl.module

import com.oorbitt.launcher.search.provider.FilesProvider

val searchModule = module {
    single { AppSearchProvider(get()) }
    single { CalculatorProvider() }
    single { ContactsProvider(get()) }
    single { FilesProvider(get()) }
    single { SettingsShortcutProvider() }
    single { UnitConverterProvider() }
    single { WebSearchFallbackProvider() }
    single { WhatsAppProvider(get()) }

    single {
        SearchAggregator(
            providers = listOf(
                get<AppSearchProvider>(),
                get<ContactsProvider>(),
                get<FilesProvider>(),
                get<WhatsAppProvider>(),
                get<CalculatorProvider>(),
                get<UnitConverterProvider>(),
                get<SettingsShortcutProvider>(),
                get<WebSearchFallbackProvider>()
            )
        )
    }
}
