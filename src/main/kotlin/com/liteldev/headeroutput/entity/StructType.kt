package com.liteldev.headeroutput.entity

import com.liteldev.headeroutput.config.origindata.MemberTypeData
import com.liteldev.headeroutput.config.origindata.TypeData

class StructType(
    name: String, typeData: TypeData,
) : ClassType(name, typeData) {

    override fun genAntiReconstruction(): String {
        val public = arrayListOf<MemberTypeData>()
        typeData.publicTypes?.filter { it.isConstructor() || (it.isOperator() && it.name == "operator=") }
            ?.let(public::addAll)
        val genOperator = public.find {
            it.isOperator() && it.params?.run {
                size == 1 && this[0].Name == "struct $name const &"
            } == true && it.valType.Name == "struct $name &"
        } == null
        val genEmptyParamConstructor = public.find { it.name == name && it.params?.isEmpty() ?: true } == null
        val genMoveConstructor = public.find {
            it.name == name && it.params?.run {
                size == 1 && this[0].Name == "struct $name const &"
            } == true
        } == null
        val sb = StringBuilder()
        if (genOperator || genEmptyParamConstructor || genMoveConstructor) {
            sb.appendLine()
            sb.appendLine("#ifndef DISABLE_CONSTRUCTOR_PREVENTION_${fullEscapeNameUpper}")
            sb.appendLine("public:")
            if (genOperator) {
                sb.appendLine("    $simpleName& operator=($simpleName const &) = delete;")
            }
            if (genMoveConstructor) {
                sb.appendLine("    $simpleName($simpleName const &) = delete;")
            }
            if (genEmptyParamConstructor) {
                sb.appendLine("    $simpleName() = delete;")
            }
            sb.appendLine("#endif")
        }
        sb.appendLine()
        return sb.toString()
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
