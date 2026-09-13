package com.oorbitt.launcher.security.di

import com.oorbitt.launcher.security.AuthManager
import com.oorbitt.launcher.security.AuthManagerImpl
import com.oorbitt.launcher.security.EncryptionEngine
import com.oorbitt.launcher.security.EncryptionEngineImpl
import org.koin.dsl.module

val securityModule = module {
    single { AuthManagerImpl() }
    single<AuthManager> { get<AuthManagerImpl>() }
    single<EncryptionEngine> { EncryptionEngineImpl() }
}
