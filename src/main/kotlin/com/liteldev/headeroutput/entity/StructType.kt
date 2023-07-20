package com.liteldev.headeroutput.entity

import com.liteldev.headeroutput.TypeManager
import com.liteldev.headeroutput.data.TypeData

class StructType(
    name: String, typeData: TypeData, isTemplate: Boolean = false
) : ClassType(name, typeData, isTemplate) {

    init {
        type = TypeKind.STRUCT
    }

    override fun generateTypeDefine(): String {
        return buildString {
            TypeManager.template[name]?.let(this::appendLine)
            appendLine("struct $simpleName {")
            if (innerTypes.isNotEmpty()) {
                appendLine("public:")
                append(generateInnerTypeDefine().replace("\n", "\n    "))
            }
            append(genAntiReconstruction())
            append(genPublic())
            append(genProtected())
            append(genPrivate())
            appendLine("};")
        }
    }
}
