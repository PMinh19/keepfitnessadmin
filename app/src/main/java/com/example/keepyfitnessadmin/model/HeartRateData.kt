package com.example.keepyfitnessadmin.model

data class HeartRateData(
    val id: String = "",
    val bpm: Int = 0,
    val status: String = "",
    val suggestion: String = "",
    val timestamp: Long = 0L,
    val duration: Long = 0L
)