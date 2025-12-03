package com.iconfit.schedule.di

import android.content.Context
import com.iconfit.schedule.data.local.CacheManager
import com.iconfit.schedule.data.remote.ScheduleFetcher
import com.iconfit.schedule.data.repository.ScheduleRepository
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
    fun provideCacheManager(
        @ApplicationContext context: Context
    ): CacheManager = CacheManager(context)

    @Provides
    @Singleton
    fun provideScheduleFetcher(
        @ApplicationContext context: Context
    ): ScheduleFetcher = ScheduleFetcher(context)

    @Provides
    @Singleton
    fun provideScheduleRepository(
        cacheManager: CacheManager,
        scheduleFetcher: ScheduleFetcher
    ): ScheduleRepository = ScheduleRepository(cacheManager, scheduleFetcher)
}
