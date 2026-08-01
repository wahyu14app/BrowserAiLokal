package com.example

import java.util.UUID

data class AiAction(
    val id: String = UUID.randomUUID().toString(),
    val condition: String,
    val action: String
)

data class AiProfile(
    val id: String,
    val name: String,
    val urlMatch: String,
    val customInstructions: String,
    val actions: List<AiAction> = emptyList(),
    val isInteractiveMode: Boolean = true,
    val autoLoopIntervalSeconds: Int = 5
)
