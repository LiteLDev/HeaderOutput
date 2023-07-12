package com.liteldev.headeroutput.entity

import com.liteldev.headeroutput.HeaderGenerator.HEADER_SUFFIX
import com.liteldev.headeroutput.HeaderOutput
import com.liteldev.headeroutput.TypeManager
import com.liteldev.headeroutput.config.GeneratorConfig
import com.liteldev.headeroutput.config.origindata.MemberTypeData
import com.liteldev.headeroutput.config.origindata.TypeData
import com.liteldev.headeroutput.parent
import com.liteldev.headeroutput.relativePath
import java.util.*

abstract class BaseType(
    var name: String,
    var typeData: TypeData,
) {
    private val includeList: MutableSet<BaseType> = mutableSetOf()

    var outerType: BaseType? = null
    val innerTypes: MutableSet<BaseType> = mutableSetOf()
    val referenceTypes: MutableSet<BaseType> = mutableSetOf()

    val simpleName = name.substringAfterLast("::")
    val fullEscapeName = name.replace("::", "_")
    val fullEscapeNameUpper = fullEscapeName.uppercase(Locale.getDefault())

    open fun getPath(): String = "$simpleName.$HEADER_SUFFIX"

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

    private fun readIncludeClassFromMembers(list: List<MemberTypeData>): Set<String> {
        val retList = mutableSetOf<String>()
        list.forEach { memberType ->
            memberType.params?.forEach { param ->
                param.Name?.let { it ->
                    Regex("(\\w+)::").findAll(it).forEach {
                        retList.add(it.groupValues[1])
                    }
                }
            }
            Regex("(\\w+)::").findAll(memberType.valType.Name ?: "").forEach {
                retList.add(it.groupValues[1])
            }
        }
        return retList.filter { inclusion ->
            GeneratorConfig.inclusionExcludeRegexList.find {
                inclusion.matches(
                    Regex(it)
                )
            } == null
        }.toSet()
    }

    private fun readList(list: List<MemberTypeData>) = readIncludeClassFromMembers(list).filter {
        TypeManager.hasType(it).apply {
            if (!this) {
                HeaderOutput.notExistBaseType.add(it)
            }
        }
    }.mapNotNull { TypeManager.getType(it) }.let(includeList::addAll)

    fun getRelativeInclusions(): String {
        val include = StringBuilder()
        includeList.forEach {
            include.appendLine("""#include "${(getPath().parent()).relativePath(it.getPath())}"""")
        }
        return include.toString()
    }

//    fun collectAllReferencedType(): Set<BaseType> {
//        val retList = mutableSetOf<BaseType>()
//        retList.addAll(includeList)
//        innerTypeList.forEach {
//            retList.addAll(it.collectAllReferencedType())
//        }
//        return retList
//    }

    abstract fun generateTypeDefine(): String

    fun generateInnerTypeDefine(): String {
        val sb = StringBuilder("\n")
        innerTypes.forEach {
            sb.appendLine(it.generateTypeDefine())
        }
        return if (sb.isNotBlank()) sb.toString() else ""
    }

    fun initIncludeList() {
        typeData.virtual?.let(::readList)
        typeData.publicTypes?.let(::readList)
        typeData.publicStaticTypes?.let(::readList)
        typeData.protectedTypes?.let(::readList)
        typeData.protectedStaticTypes?.let(::readList)
        typeData.privateTypes?.let(::readList)
        typeData.privateStaticTypes?.let(::readList)
        includeList.removeIf { it === this }
    }

//    private fun collectSelfReferencedType() {
//        val referencedTypeNames = mutableSetOf<String>()
//        val list = mutableListOf<MemberTypeData>()
//        typeData.virtual?.let(list::addAll)
//        typeData.publicTypes?.let(list::addAll)
//        typeData.publicStaticTypes?.let(list::addAll)
//        typeData.protectedTypes?.let(list::addAll)
//        typeData.protectedStaticTypes?.let(list::addAll)
//        typeData.privateTypes?.let(list::addAll)
//        typeData.privateStaticTypes?.let(list::addAll)
//        list.forEach { memberType ->
//            memberType.params?.forEach { param ->
//                param.Name?.let { it ->
//                    Regex("(\\w+::\\w+)").findAll(it).forEach {
//                        referencedTypeNames.add(it.groupValues[1])
//                    }
//                }
//            }
//            Regex("(\\w+)::").findAll(memberType.valType.Name ?: "").forEach {
//                referencedTypeNames.add(it.groupValues[1])
//            }
//        }
//        return referencedTypeNames.filter { inclusion ->
//            GeneratorConfig.inclusionExcludeRegexList.find {
//                inclusion.matches(
//                    Regex(it)
//                )
//            } == null
//        }.toSet()
//
//    }


    companion object {
        const val GLOBAL_HEADER_PATH = "llapi/Global.h"
    }
}
