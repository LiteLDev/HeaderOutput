package com.liteldev.headeroutput.config.output


import kotlinx.serialization.Serializable

@Serializable
data class Sort(
    val parent: List<Parent>,
    val regex: List<Regex>
) {
    @Serializable
    data class Parent(
        val src: String,
        val dst: String
    )

    @Serializable
    data class Regex(
        val regex: String,
        val dst: String,
        val override: Boolean = false
    )
}