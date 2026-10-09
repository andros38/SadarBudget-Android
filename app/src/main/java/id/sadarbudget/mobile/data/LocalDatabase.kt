package id.sadarbudget.mobile.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class LocalDatabase(context: Context) : SQLiteOpenHelper(context, "sadarbudget.db", null, 1) {
    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                email TEXT NOT NULL COLLATE NOCASE UNIQUE,
                password_hash TEXT NOT NULL,
                password_salt TEXT NOT NULL,
                profile_photo TEXT,
                password_changed_at TEXT,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE categories (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                name TEXT NOT NULL COLLATE NOCASE,
                type TEXT NOT NULL CHECK(type IN ('income','expense')),
                is_active INTEGER NOT NULL DEFAULT 1,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
                UNIQUE(user_id, type, name)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_categories_user_type_active ON categories(user_id, type, is_active)")
        db.execSQL(
            """
            CREATE TABLE transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                category_id INTEGER,
                category_name_snapshot TEXT NOT NULL,
                type TEXT NOT NULL CHECK(type IN ('income','expense')),
                amount REAL NOT NULL CHECK(amount > 0),
                description TEXT,
                transaction_date TEXT NOT NULL,
                created_at TEXT NOT NULL,
                FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
                FOREIGN KEY(category_id) REFERENCES categories(id) ON DELETE SET NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_transactions_user_date ON transactions(user_id, transaction_date, created_at)")
        db.execSQL("CREATE INDEX idx_transactions_user_type_date ON transactions(user_id, type, transaction_date)")
        db.execSQL("CREATE INDEX idx_transactions_category ON transactions(category_id)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
}
