package com.example.replybubble.data.repository

import com.example.replybubble.data.local.dao.AnalysisSessionDao
import com.example.replybubble.data.local.dao.ContactProfileDao
import com.example.replybubble.data.local.dao.ReplySuggestionDao
import com.example.replybubble.data.local.dao.StyleTrainingSampleDao
import com.example.replybubble.domain.repository.MaintenanceRepository
import com.example.replybubble.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MaintenanceRepositoryImpl @Inject constructor(
    private val contactProfileDao: ContactProfileDao,
    private val analysisSessionDao: AnalysisSessionDao,
    private val replySuggestionDao: ReplySuggestionDao,
    private val styleTrainingSampleDao: StyleTrainingSampleDao,
    private val settingsRepository: SettingsRepository,
) : MaintenanceRepository {
    override suspend fun clearAllData() {
        replySuggestionDao.clearAll()
        analysisSessionDao.clearAll()
        contactProfileDao.clearAll()
        styleTrainingSampleDao.clearAll()
        settingsRepository.resetSettings()
    }
}
