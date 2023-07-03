package com.liteldev.headeroutput.config

import kotlinx.serialization.Serializable

@Serializable
data class GeneratorConfigData(
    var exclusion: Exclusion
) {
    @Serializable
    data class Exclusion(
        var generation: Generation, var inclusion: Inclusion
    ) {
        @Serializable
        data class Generation(var regex: MutableList<String>)

        @Serializable
        data class Inclusion(var regex: MutableList<String>)
    }
}
