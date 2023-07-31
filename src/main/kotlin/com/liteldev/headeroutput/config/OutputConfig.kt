package com.liteldev.headeroutput.config

import kotlinx.serialization.Serializable

@Serializable
data class OutputConfig(
    val config: Config,
    val exclusion: Exclusion,
    val sort: Sort,
    val replacement: Replacement
) {
    @Serializable
    data class Config(val rootPath: String, val enableRelativePath: Boolean)

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

    @Serializable
    data class Exclusion(
        val regex: List<String>
    )

    @Serializable
    data class Replacement(
        val regex: List<Regex>
    ) {
        @Serializable
        data class Regex(
            val regex: String,
            val to: String
        )
    }

}
