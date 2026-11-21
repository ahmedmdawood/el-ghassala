package dev.ahmeddawood.elghassala.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter
    fun transferKindToString(kind: TransferKind): String = kind.name

    @TypeConverter
    fun stringToTransferKind(value: String): TransferKind = TransferKind.valueOf(value)
}

@Database(
    entities = [
        DayEntity::class,
        PersonEntity::class,
        RouteEntity::class,
        TransportEntity::class,
        TransportParticipantEntity::class,
        DriverRefundEntity::class,
        ExpenseEntity::class,
        ExpensePayerEntity::class,
        ExpenseBeneficiaryEntity::class,
        TransferEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dayDao(): DayDao
    abstract fun personDao(): PersonDao
    abstract fun routeDao(): RouteDao
    abstract fun transportDao(): TransportDao
    abstract fun refundDao(): RefundDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun transferDao(): TransferDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "elghassala.db",
            )
                // No destructive fallback: schema bumps require explicit migrations.
                .build()
                .also { instance = it }
        }
    }
}
