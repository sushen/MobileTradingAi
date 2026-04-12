package com.tradingai.app.models

data class Investment(
    val id: String = "",
    val userId: String = "",
    val planName: String = "",
    val amount: Double = 0.0,
    val status: String = "pending",
    val timestamp: Long = System.currentTimeMillis()
)

data class InvestmentPlan(
    val name: String,
    val minCapital: String,
    val roi: String,
    val risk: String
)
