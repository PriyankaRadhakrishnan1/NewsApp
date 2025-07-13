package com.example.newsapp

import android.app.Application
import com.example.newsapp.di.AppModule

class NewsApplication : Application(){
    override fun onCreate() {
        super.onCreate()
        AppModule.init(this) //this is application context.
    }
}