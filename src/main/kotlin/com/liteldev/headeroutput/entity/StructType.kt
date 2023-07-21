package com.liteldev.headeroutput.entity

import com.liteldev.headeroutput.data.TypeData

class StructType(
    name: String, typeData: TypeData, isTemplate: Boolean = false
) : ClassType(name, typeData, isTemplate) {
    init {
        type = TypeKind.STRUCT
    }
}
