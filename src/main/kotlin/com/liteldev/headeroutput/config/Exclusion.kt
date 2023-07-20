package com.liteldev.headeroutput.config

import kotlinx.serialization.Serializable

@Serializable
data class Exclusion(
    val generation: Generation,
    val inclusion: Inclusion
) {
    @Serializable
    data class Generation(
        val regex: List<String>
    )

    @Serializable
    data class Inclusion(
        val regex: List<String>
    )
}
