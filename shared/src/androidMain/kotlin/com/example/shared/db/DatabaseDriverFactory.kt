package com.example.shared.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.shared.NewsAppDatabase

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
      return  AndroidSqliteDriver(NewsAppDatabase.Schema, context, "DATABASE_NAME")
    }
}