package com.liteldev.headeroutput.entity

import com.liteldev.headeroutput.HeaderOutput
import com.liteldev.headeroutput.TypeManager
import com.liteldev.headeroutput.config.GeneratorConfig
import com.liteldev.headeroutput.config.origindata.MemberTypeData
import com.liteldev.headeroutput.config.origindata.TypeData
import com.liteldev.headeroutput.parent
import com.liteldev.headeroutput.relativePath

abstract class BaseType(
    var name: String,
    var typeData: TypeData,
) {
    private val includeList: MutableSet<BaseType> = mutableSetOf()

    fun getPath(): String = "./$name.hpp"

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

    open fun initIncludeList() {
        typeData.virtual?.let(::readList)
        typeData.publicTypes?.let(::readList)
        typeData.publicStaticTypes?.let(::readList)
        typeData.protectedTypes?.let(::readList)
        typeData.protectedStaticTypes?.let(::readList)
        typeData.privateTypes?.let(::readList)
        typeData.privateStaticTypes?.let(::readList)
        includeList.removeIf { it === this }
    }

    companion object {
        const val GLOBAL_HEADER_PATH = "llapi/Global.h"
    }
}
