package com.liteldev.headeroutput.entity

import com.liteldev.headeroutput.HeaderGenerator.HEADER_SUFFIX
import com.liteldev.headeroutput.TypeManager
import com.liteldev.headeroutput.config.origindata.MemberTypeData
import com.liteldev.headeroutput.config.origindata.TypeData
import com.liteldev.headeroutput.getTopLevelFileType
import com.liteldev.headeroutput.removeTypeSpecifier
import java.util.*

abstract class BaseType(
    var name: String,
    var typeData: TypeData,
) {
    lateinit var includeList: MutableSet<String>

    var outerType: BaseType? = null
    val innerTypes: MutableSet<BaseType> = mutableSetOf()
    val referenceTypes: MutableSet<BaseType> = mutableSetOf()

    val simpleName = name.substringAfterLast("::")
    val fullEscapeName = name.replace("::", "_")
    val fullEscapeNameUpper = fullEscapeName.uppercase(Locale.getDefault())

    open fun getPath(): String {
        if (outerType == null) {
            return "./$simpleName.$HEADER_SUFFIX"
        }
        return "./" + getTopLevelFileType().name.replace("::", "/") + "." + HEADER_SUFFIX
    }

    fun constructInnerTypeList(outerType: BaseType? = null) {
        this.outerType = outerType
        TypeManager.getAllTypes().filter {
            if (!it.name.startsWith(this.name + "::"))
                false
            else
                !it.name.substring(this.name.length + 2).contains("::")
        }.forEach {
            innerTypes.add(it)
            it.constructInnerTypeList(this)
        }
    }

    fun collectAllReferencedType(): Set<BaseType> {
        val retList = mutableSetOf<BaseType>()
        retList.addAll(referenceTypes)
        innerTypes.forEach {
            retList.addAll(it.collectAllReferencedType())
        }
        return retList
    }

    abstract fun generateTypeDefine(): String

    abstract fun initIncludeList()

    fun generateInnerTypeDefine(): String {
        val sb = StringBuilder("\n")
        innerTypes.forEach {
            sb.appendLine(it.generateTypeDefine())
        }
        return if (sb.isNotBlank()) sb.toString() else ""
    }

    fun collectSelfReferencedType() {
        val referencedTypeNames = mutableSetOf<String>()
        val list = mutableListOf<MemberTypeData>()
        typeData.virtual?.let(list::addAll)
        typeData.publicTypes?.let(list::addAll)
        typeData.publicStaticTypes?.let(list::addAll)
        typeData.protectedTypes?.let(list::addAll)
        typeData.protectedStaticTypes?.let(list::addAll)
        typeData.privateTypes?.let(list::addAll)
        typeData.privateStaticTypes?.let(list::addAll)
        list.forEach { memberType ->
            memberType.params?.forEach { param ->
                referencedTypeNames.add(removeTypeSpecifier(param.Name ?: ""))
            }
            memberType.valType.Name?.let {
                referencedTypeNames.add(removeTypeSpecifier(it))
            }
        }
        referencedTypeNames.forEach {
            TypeManager.getType(it)?.let { type ->
                referenceTypes.add(type)
            }
        }
    }


    companion object {
        const val GLOBAL_HEADER_PATH = "llapi/Global.h"
    }
}
