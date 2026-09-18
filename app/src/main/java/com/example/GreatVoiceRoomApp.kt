package com.example

import android.app.Application
import com.example.di.AppContainer
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class GreatVoiceRoomApp : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val app = FirebaseApp.initializeApp(this)
                if (app == null) {
                    val options = FirebaseOptions.Builder()
                        .setApplicationId("1:625968572161:android:a3fa714759b88186d1dd00")
                        .setProjectId("great-voice-chat")
                        .setApiKey("AIzaSyCXgaUX8keVnOhoBPLAWPc40rChnBQBKKA")
                        .setDatabaseUrl("https://great-voice-chat-default-rtdb.firebaseio.com")
                        .setStorageBucket("great-voice-chat.firebasestorage.app")
                        .build()
                    FirebaseApp.initializeApp(this, options)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        container = AppContainer(this)
    }
}
