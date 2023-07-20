package com.liteldev.headeroutput.entity

import com.liteldev.headeroutput.data.TypeData

class EnumType(name: String) : BaseType(name, TypeKind.ENUM, TypeData.empty()) {
    override fun generateTypeDefine(): String {
        return "enum class $simpleName {};\n"
    }

}
