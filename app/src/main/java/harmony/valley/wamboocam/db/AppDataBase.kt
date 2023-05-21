package harmony.valley.wamboocam.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import harmony.valley.wamboocam.models.CompressData

@Database(entities = [CompressData::class], version = 1, exportSchema = false)
abstract class AppDataBase : RoomDatabase() {

    abstract fun compressionDao(): CompressionDao

    companion object {
        @Volatile
        var instance: AppDataBase? = null
        private const val DATABASE_NAME = "User"

        fun getInstance(context: Context): AppDataBase? {
            if (instance == null) {
                synchronized(AppDataBase::class.java)
                {
                    if (instance == null) {
                        instance = Room.databaseBuilder(
                            context, AppDataBase::class.java,
                            DATABASE_NAME
                        )
                            .fallbackToDestructiveMigration()
                            .build()
                    }
                }
            }

            return instance
        }

    }
}
