package com.oorbitt.launcher.iconpack.di

import com.oorbitt.launcher.iconpack.IconPackManager
import com.oorbitt.launcher.ui.util.IconPackProvider
import org.koin.dsl.module

val iconPackModule = module {
    single<IconPackProvider> { IconPackManager(get()) }
}
