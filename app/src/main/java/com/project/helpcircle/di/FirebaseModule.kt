package com.project.helpcircle.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Hilt module for Firestore/FCM providers; populated in the Firebase integration step. */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule
