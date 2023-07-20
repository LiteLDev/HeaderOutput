package com.liteldev.headeroutput.config

import kotlinx.serialization.Serializable

@Serializable
data class OutputConfig(
    val exclusion: Exclusion,
    val sort: Sort
)
