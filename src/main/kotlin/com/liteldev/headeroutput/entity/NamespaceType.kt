package com.liteldev.headeroutput.entity

import com.liteldev.headeroutput.data.TypeData
import com.liteldev.headeroutput.relativePathTo

class NamespaceType(
    name: String, typeData: TypeData
) : BaseType(name, TypeKind.NAMESPACE, typeData) {

    fun genPublic(): String {
        val sb = StringBuilder()
        typeData.publicTypes?.sortedBy { it.name }?.forEach {
            sb.appendLine(it.genFuncString(namespace = true))
        }
        sb.appendLine()
        return sb.toString()
    }

    override fun generateTypeDefine(): String {
        val sb = StringBuilder()
        sb.append("namespace $name {\n")
        sb.append(genPublic())
        sb.appendLine("};")
        return sb.toString()
    }

    override fun initIncludeAndForwardDeclareList() {
        // not include self, inner type, and types can forward declare
        val declareRequiredTypes = referenceTypes.filter {
            it.name.contains("::") || (it as? ClassType)?.isTemplateClass == true
        }
        declareRequiredTypes
            .filter { it.outerType is ClassType }
            .map { this.path.relativePathTo(it.path) }
            .let(includeList::addAll)
        includeList.remove(this.path.relativePathTo(this.path))
        includeList.remove("")

        declareRequiredTypes
            .filter { it.outerType !is ClassType }
            .map { it.generateTypeDeclare() }
            .let(forwardDeclareList::addAll)
    }
}
