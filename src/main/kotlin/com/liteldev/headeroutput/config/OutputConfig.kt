package com.liteldev.headeroutput.config

import kotlinx.serialization.Serializable

@Serializable
data class OutputConfig(
    val config: Config,
    val exclusion: Exclusion,
    val sort: Sort
) {
    @Serializable
    data class Config(val rootPath: String, val enableRelativePath: Boolean)
}
