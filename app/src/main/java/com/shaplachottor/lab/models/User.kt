package com.shaplachottor.lab.models

data class User(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val photoUrl: String? = null,
    val progress: Int = 0,
    val phaseProgress: Map<String, Int> = emptyMap(),
    val unlockedPhases: List<String> = emptyList(),
    val completedPhases: List<String> = emptyList(),
    val unlockedFeatures: AdvancedFeatures = AdvancedFeatures()
)
