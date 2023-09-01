package com.liteldev.headeroutput.data

import kotlinx.serialization.Serializable

@Serializable
data class VariableTypeData(
    var name: String,
    var kind: VarSymbolKind,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VariableTypeData

        if (name != other.name) return false
        return kind == other.kind
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + kind.value
        return result
    }
}
