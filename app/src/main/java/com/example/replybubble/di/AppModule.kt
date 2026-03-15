package com.example.replybubble.di

import android.content.Context
import androidx.room.Room
import com.example.replybubble.correction.OpenRouterTextCorrectionEngine
import com.example.replybubble.correction.TextCorrectionEngine
import com.example.replybubble.data.local.AppDatabase
import com.example.replybubble.data.local.dao.AnalysisSessionDao
import com.example.replybubble.data.local.dao.ContactProfileDao
import com.example.replybubble.data.local.dao.ReplySuggestionDao
import com.example.replybubble.data.local.dao.StyleTrainingSampleDao
import com.example.replybubble.data.repository.MaintenanceRepositoryImpl
import com.example.replybubble.data.repository.ProfileRepositoryImpl
import com.example.replybubble.data.repository.SessionRepositoryImpl
import com.example.replybubble.data.repository.SettingsRepositoryImpl
import com.example.replybubble.data.repository.StyleTrainingRepositoryImpl
import com.example.replybubble.domain.repository.MaintenanceRepository
import com.example.replybubble.domain.repository.ProfileRepository
import com.example.replybubble.domain.repository.SessionRepository
import com.example.replybubble.domain.repository.SettingsRepository
import com.example.replybubble.domain.repository.StyleTrainingRepository
import com.example.replybubble.ocr.MlKitOcrProcessor
import com.example.replybubble.ocr.OcrProcessor
import com.example.replybubble.recommendation.BuiltInRecommendationEngine
import com.example.replybubble.recommendation.OpenRouterRecommendationEngine
import com.example.replybubble.recommendation.RecommendationEngine
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "replybubble.db",
        )
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigration(true)
            .build()
    }

    @Provides
    fun provideContactProfileDao(database: AppDatabase): ContactProfileDao = database.contactProfileDao()

    @Provides
    fun provideAnalysisSessionDao(database: AppDatabase): AnalysisSessionDao = database.analysisSessionDao()

    @Provides
    fun provideReplySuggestionDao(database: AppDatabase): ReplySuggestionDao = database.replySuggestionDao()

    @Provides
    fun provideStyleTrainingSampleDao(database: AppDatabase): StyleTrainingSampleDao = database.styleTrainingSampleDao()
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

    @Binds
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    abstract fun bindStyleTrainingRepository(impl: StyleTrainingRepositoryImpl): StyleTrainingRepository

    @Binds
    abstract fun bindMaintenanceRepository(impl: MaintenanceRepositoryImpl): MaintenanceRepository

    @Binds
    abstract fun bindOcrProcessor(impl: MlKitOcrProcessor): OcrProcessor

    @Binds
    abstract fun bindRecommendationEngine(impl: OpenRouterRecommendationEngine): RecommendationEngine

    @Binds
    abstract fun bindTextCorrectionEngine(impl: OpenRouterTextCorrectionEngine): TextCorrectionEngine
}
