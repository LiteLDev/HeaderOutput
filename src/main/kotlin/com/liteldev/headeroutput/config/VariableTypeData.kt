package com.liteldev.headeroutput.config

import com.liteldev.headeroutput.entity.VarSymbolType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VariableTypeData (
    @SerialName("name") var Name: String?,
    @SerialName("kind") val Type: VarSymbolType,
)