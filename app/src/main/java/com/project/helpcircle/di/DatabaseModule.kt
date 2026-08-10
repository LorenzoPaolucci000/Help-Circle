package com.project.helpcircle.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Hilt module for Room/SQLCipher providers; populated in the data-layer build step. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule
