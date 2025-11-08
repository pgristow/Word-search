package com.wordsearch

import android.app.Application
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class WordSearchApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Initialize AdMob - TODO: Disabled until proper google-services.json is configured
        // MobileAds.initialize(this) {
        //     Timber.d("AdMob initialized")
        // }
        Timber.d("AdMob disabled - using placeholder google-services.json")

        Timber.d("WordSearchApplication created")
    }
}
