package com.liteldev.headeroutput.entity

import com.liteldev.headeroutput.config.MemberTypeData
import com.liteldev.headeroutput.config.TypeData

class StructType(
    name: String, typeData: TypeData,
) : ClassType(name, typeData) {
    override fun getPath(): String {
        return "./$name.hpp"
    }

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
            sb.appendLine("#ifndef DISABLE_CONSTRUCTOR_PREVENTION_${name.uppercase()}")
            sb.appendLine("public:")
            if (genOperator) {
                sb.appendLine("    struct $name& operator=(struct $name const &) = delete;")
            }
            if (genMoveConstructor) {
                sb.appendLine("    $name(struct $name const &) = delete;")
            }
            if (genEmptyParamConstructor) {
                sb.appendLine("    $name() = delete;")
            }
            sb.appendLine("#endif")
        }
        sb.appendLine()
        return sb.toString()
    }

}