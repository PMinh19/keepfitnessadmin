package com.example.keepyfitnessadmin.model

data class Workout(
    val exerciseName: String = "", // Khớp với Firestore
    val duration: Long = 0L,
    val caloriesBurned: Int = 0, // Khớp với Firestore
    val date: Long = 0L,
    val exerciseId: Long = 0L, // Đổi từ String sang Long để khớp với Firestore
    val count: Int = 0,
    val completed: Boolean = false,
    val id: String = "",
    val targetCount: Int = 0
)