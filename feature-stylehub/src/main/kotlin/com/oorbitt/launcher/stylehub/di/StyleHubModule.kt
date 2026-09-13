package com.oorbitt.launcher.stylehub.di

import com.oorbitt.launcher.stylehub.data.StyleHubRepository
import org.koin.dsl.module

val styleHubModule = module {
    single { StyleHubRepository(get()) }
}
