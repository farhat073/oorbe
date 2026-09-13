package com.oorbitt.launcher.home.di

import com.oorbitt.launcher.home.widget.WidgetHostManager
import org.koin.dsl.module

val homeModule = module {
    single { WidgetHostManager(get()) }
    single { com.oorbitt.launcher.ui.util.DragManager() }
}
