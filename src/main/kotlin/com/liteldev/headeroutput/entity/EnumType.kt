package com.liteldev.headeroutput.entity

import com.liteldev.headeroutput.data.TypeData

class EnumType(name: String) : BaseType(name, TypeKind.ENUM, TypeData.empty()) {
    override fun generateTypeDefine(): String {
        return "enum class $simpleName {};\n"
    }

    override fun generateTypeDeclare(): String {
        val baseDeclaration = "enum class $simpleName;"
        if (outerType == null || outerType is ClassType) {
            return baseDeclaration
        }
        if (outerType?.type == TypeKind.NAMESPACE) {
            return "namespace ${outerType?.name} { $baseDeclaration }"
        }
        error("unreachable")
    }

}
