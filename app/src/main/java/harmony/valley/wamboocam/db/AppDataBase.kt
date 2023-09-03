/**
 * Copyright (c) 2023 Natalia Molinero Mingorance
 * All rights reserved.
 */


package harmony.valley.wamboocam.db


import androidx.room.Database
import androidx.room.RoomDatabase
import harmony.valley.wamboocam.models.CompressData

@Database(entities = [CompressData::class], version = 1, exportSchema = false)
abstract class AppDataBase : RoomDatabase() {

    abstract fun compressionDao(): CompressionDao

    companion object
}
