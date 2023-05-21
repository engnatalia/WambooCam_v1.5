package harmony.valley.wamboocam.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import harmony.valley.wamboocam.db.CompressionDao
import harmony.valley.wamboocam.db.AppDataBase
import harmony.valley.wamboocam.repository.CompressRepository
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class DatabaseModule {
    @Provides
    fun provideChannelDao(appDatabase: AppDataBase): CompressionDao {
        return appDatabase.compressionDao()
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext appContext: Context): AppDataBase {
        return Room.databaseBuilder(
            appContext,
            AppDataBase::class.java,
            "compression_details"
        ).build()
    }

    @Provides
    @Singleton
    fun provideRepository(compressionDao: CompressionDao) = CompressRepository(compressionDao)
}