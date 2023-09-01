package com.liteldev.headeroutput.entity

import com.liteldev.headeroutput.data.TypeData

class UnionType(name: String) : BaseType(name, TypeKind.UNION, TypeData.empty()) {
    override fun generateTypeDefine(): String {
        return "union $simpleName {};\n"
    }

    override fun generateTypeDeclare(): String {
        val baseDeclaration = "union $simpleName;"
        if (outerType == null || outerType is ClassType) {
            return baseDeclaration
        }
        if (outerType?.type == TypeKind.NAMESPACE) {
            return "namespace ${outerType?.name} { $baseDeclaration }"
        }
        error("unreachable")
    }

}
