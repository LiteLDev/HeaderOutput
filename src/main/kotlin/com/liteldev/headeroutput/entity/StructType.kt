package com.liteldev.headeroutput.entity

import com.liteldev.headeroutput.config.origindata.TypeData

class StructType(
    name: String, typeData: TypeData, isTemplate: Boolean = false
) : ClassType(name, typeData, isTemplate) {

    init {
        type = TypeKind.STRUCT
    }

    override fun generateTypeDefine(): String {
        val sb = StringBuilder("struct $simpleName {\n")
        if (innerTypes.isNotEmpty()) {
            sb.appendLine("public:")
            sb.append(generateInnerTypeDefine().replace("\n", "\n    "))
        }
        sb.append(genAntiReconstruction())
        sb.append(genPublic())
        sb.append(genProtected())
        sb.append(genPrivate())
        sb.appendLine("};")
        return sb.toString()
    }
}
