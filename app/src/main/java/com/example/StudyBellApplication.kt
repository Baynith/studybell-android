package com.example

import android.app.Application
import com.example.ads.AdMobManager
import com.example.alarm.NotificationHelper
import com.example.data.db.AppDatabase
import com.example.data.repository.StudyBellRepository

class StudyBellApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val repository: StudyBellRepository by lazy { StudyBellRepository(database) }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
        AdMobManager.initialize(this)
    }
}
