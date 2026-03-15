package com.example.replybubble.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateTimeFormatter {
    private val sessionFormat = SimpleDateFormat("MM.dd a h:mm", Locale.KOREAN)

    fun formatSessionTime(timeMillis: Long): String {
        return sessionFormat.format(Date(timeMillis))
    }
}
