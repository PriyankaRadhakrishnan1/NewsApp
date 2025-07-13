package com.example.shared.db

import app.cash.sqldelight.db.SqlDriver


actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        // TODO: Add iOS driver if/when needed
        throw NotImplementedError("iOS driver not implemented yet")
    }
}