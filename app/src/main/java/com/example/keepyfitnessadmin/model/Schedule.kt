package com.example.keepyfitnessadmin.model

data class Schedule(
    val exercise: String = "",
    val time: String = "",
    val days: List<String> = emptyList(),
    val quantity: Int = 0
) {
    // Constructor không tham số cho Firestore
    constructor() : this("", "", emptyList(), 0)
}