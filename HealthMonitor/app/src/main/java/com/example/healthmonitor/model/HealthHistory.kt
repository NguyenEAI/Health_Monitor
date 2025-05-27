package com.example.healthmonitor.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HealthHistory(
    val id: String = "",
    val heartRate: Int = 0,
    val spO2: Int = 0,
    val temperature: Double = 0.0,
    val timestamp: Long = 0
) {
    fun getFormattedDate(): String {
        val date = Date(timestamp)
        val format = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        return format.format(date)
    }

    fun getFormattedDateOnly(): String {
        val date = Date(timestamp)
        val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return format.format(date)
    }
} 