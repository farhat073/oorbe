package com.oorbitt.launcher.model

data class OrbSpaceStats(
    val id: Long = 0,
    val packageName: String,
    val scrollCount: Int = 0,
    val shortFormScrollCount: Int = 0,
    val timeSpentMs: Long = 0,
    val date: Long = System.currentTimeMillis() // Start of day timestamp
)
