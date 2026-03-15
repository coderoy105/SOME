package com.example.replybubble.domain.repository

interface MaintenanceRepository {
    suspend fun clearAllData()
}
