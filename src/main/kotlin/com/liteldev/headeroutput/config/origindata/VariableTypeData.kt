package com.liteldev.headeroutput.config.origindata

import com.liteldev.headeroutput.entity.VarSymbolType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VariableTypeData(
    @SerialName("name") var Name: String?,
    @SerialName("kind") val Type: VarSymbolType,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VariableTypeData

        if (Name != other.Name) return false
        return Type == other.Type
    }

    override fun hashCode(): Int {
        var result = Name?.hashCode() ?: 0
        result = 31 * result + Type.value
        return result
    }
}