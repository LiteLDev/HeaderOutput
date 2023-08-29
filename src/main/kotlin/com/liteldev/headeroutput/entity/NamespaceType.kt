package com.liteldev.headeroutput.entity

import com.liteldev.headeroutput.data.TypeData
import com.liteldev.headeroutput.getTopLevelFileType
import com.liteldev.headeroutput.isEnum
import com.liteldev.headeroutput.isNamespace
import com.liteldev.headeroutput.relativePathTo

class NamespaceType(
    name: String, typeData: TypeData
) : BaseType(name, TypeKind.NAMESPACE, typeData) {

    fun genPublic() = buildString {
        if (typeData.publicTypes?.isEmpty() != false) {
            return@buildString
        }
        appendLine("// NOLINTBEGIN")
        typeData.publicTypes?.sortedBy { it.name }?.forEach {
            appendLine(it.genFuncString(namespace = true))
        }
        appendLine("// NOLINTEND")
        appendLine()
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
        allReferences.filter {
            outerType?.isNamespace() == true
                    || it.name.contains("::")
                    || ((it as? ClassType)?.isTemplateClass == true)
                    || it.isEnum()
        }.forEach {
            if (it.isEnum() || it.outerType is ClassType || (it as? ClassType)?.isTemplateClass == true) {
                this.path.relativePathTo(it.path).let(includeList::add)
            } else {
                it.generateTypeDeclare().let(forwardDeclareList::add)
            }
        }
        includeList.remove(this.path.relativePathTo(this.path))
        includeList.remove("")
    }
}
