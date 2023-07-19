package com.liteldev.headeroutput.entity

import com.liteldev.headeroutput.HeaderGenerator.HEADER_SUFFIX
import com.liteldev.headeroutput.TypeManager
import com.liteldev.headeroutput.config.origindata.TypeData
import com.liteldev.headeroutput.getTopLevelFileType
import java.util.*

abstract class BaseType(
    var name: String,
    type: TypeKind,
    var typeData: TypeData,
) {
    var type: TypeKind = type
        protected set

    var outerType: BaseType? = null

    val innerTypes: MutableSet<BaseType> = mutableSetOf()
    val referenceTypes: MutableSet<BaseType> = mutableSetOf()
    val includeList: MutableSet<String> = mutableSetOf()

    val simpleName = name.substringAfterLast("::")
    val fullEscapeName = name.replace("::", "_")
    val fullUpperEscapeName = fullEscapeName.uppercase(Locale.getDefault())

    abstract fun generateTypeDefine(): String

    open fun initIncludeList() {}

    fun getPath(): String {
        return "./${getTopLevelFileType().name.replace("::", "/")}.$HEADER_SUFFIX"
    }

    fun constructInnerTypeList(outerType: BaseType? = null) {
        this.outerType = outerType
        innerTypes.addAll(
            TypeManager.getAllTypes().filter {
                it.name.startsWith(this.name + "::") && !it.name.substring(this.name.length + 2).contains("::")
            }.onEach {
                it.constructInnerTypeList(this)
            }
        )
    }

    protected fun collectAllReferencedType(): Set<BaseType> =
        referenceTypes + innerTypes.flatMap { it.collectAllReferencedType() }

    fun generateInnerTypeDefine(): String {
        val generatedTypes = innerTypes.joinToString(separator = "\n") { it.generateTypeDefine() }
        return if (generatedTypes.isNotBlank()) "\n$generatedTypes" else ""
    }

    fun collectSelfReferencedType() {
        typeData.collectReferencedTypes().forEach { (name, kind) ->
            if (!TypeManager.hasType(name)) {
                TypeManager.createDummyClass(name, kind)
            }
            TypeManager.getType(name)?.let(referenceTypes::add)
        }
    }

    enum class TypeKind {
        CLASS, STRUCT, ENUM, UNION, NAMESPACE
    }
}
