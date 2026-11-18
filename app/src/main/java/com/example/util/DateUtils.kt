package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {

    private val fullDateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH)
    private val shortDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH)
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.FRENCH)
    private val exportDateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US)

    fun formatDateTime(timestamp: Long): String {
        return fullDateTimeFormat.format(Date(timestamp))
    }

    fun formatDate(timestamp: Long): String {
        return shortDateFormat.format(Date(timestamp))
    }

    fun formatTime(timestamp: Long): String {
        return timeFormat.format(Date(timestamp))
    }

    fun formatForExport(timestamp: Long): String {
        return exportDateFormat.format(Date(timestamp))
    }
}
