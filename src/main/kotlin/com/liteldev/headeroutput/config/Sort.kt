package com.liteldev.headeroutput.config

import kotlinx.serialization.Serializable

@Serializable
data class Sort(
    val parent: List<Parent>,
    val regex: List<Regex>
) {
    @Serializable
    data class Parent(
        val parent: String,
        val dst: String
    )

    @Serializable
    data class Regex(
        val regex: String,
        val dst: String,
        val override: Boolean = false
    )
}
