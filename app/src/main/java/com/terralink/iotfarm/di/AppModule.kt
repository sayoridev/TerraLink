package com.terralink.iotfarm.di

import android.content.Context
import androidx.room.Room
import com.terralink.iotfarm.data.local.AppDatabase
import com.terralink.iotfarm.data.local.TelemetryDao
import com.terralink.iotfarm.data.network.TcpSocketManager
import com.terralink.iotfarm.data.repository.FarmRepositoryImpl
import com.terralink.iotfarm.domain.repository.FarmRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideTelemetryDao(database: AppDatabase): TelemetryDao {
        return database.telemetryDao()
    }

    @Provides
    @Singleton
    fun provideFarmRepository(
        tcpSocketManager: TcpSocketManager,
        telemetryDao: TelemetryDao
    ): FarmRepository {
        return FarmRepositoryImpl(tcpSocketManager, telemetryDao)
    }
}
