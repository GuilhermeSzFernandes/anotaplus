package com.guilherme.anotaplus.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Converters {
    @TypeConverter
    fun fromEntryType(value: EntryType): String = value.name

    @TypeConverter
    fun toEntryType(value: String): EntryType = EntryType.valueOf(value)
}

// v2 -> v3: adiciona remoteId (String?) em entries/categories, pro backup
// na nuvem. Migração real (não destrutiva) porque, diferente das versões
// anteriores, agora tem gente com dado de verdade guardado no app — seria
// irônico apagar o histórico local bem na hora de adicionar backup.
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE entries ADD COLUMN remoteId TEXT")
        db.execSQL("ALTER TABLE categories ADD COLUMN remoteId TEXT")
    }
}

// v3 -> v4: adiciona titulo (String?) em entries, pra tela "bloco de
// notas" (ManualIdeiaActivity) — Gasto e Ideia via Captura Rápida
// continuam gravando null nessa coluna.
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE entries ADD COLUMN titulo TEXT")
    }
}

// v4 -> v5: adiciona limite (Double?) em categories, pra feature de
// orçamento mensal por categoria.
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE categories ADD COLUMN limite REAL")
    }
}

// v5 -> v6: adiciona o tipo RECEBIMENTO (feito só em código, enum não é
// coluna própria) e a tabela carteiras (VR, Cartão X...), com a coluna
// carteira em entries pra casar por nome, mesmo padrão de categoria.
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE entries ADD COLUMN carteira TEXT")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS carteiras (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, nome TEXT NOT NULL, remoteId TEXT)"
        )
    }
}

@Database(entities = [Entry::class, Category::class, Carteira::class], version = 6, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
    abstract fun categoryDao(): CategoryDao
    abstract fun carteiraDao(): CarteiraDao

    companion object {
        private val CATEGORIAS_PADRAO = listOf("Mercado", "Transporte", "Lazer", "Contas", "Outros")

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "anotaplus.db"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                val dao = getInstance(context).categoryDao()
                                CATEGORIAS_PADRAO.forEach { dao.insert(Category(nome = it)) }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
