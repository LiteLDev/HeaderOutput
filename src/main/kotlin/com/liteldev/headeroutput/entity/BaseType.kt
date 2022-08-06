package com.liteldev.headeroutput.entity

import com.liteldev.headeroutput.HeaderOutput
import com.liteldev.headeroutput.config.MemberTypeData
import com.liteldev.headeroutput.config.TypeData
import com.liteldev.headeroutput.parent
import com.liteldev.headeroutput.relativePath

abstract class BaseType(
    var name: String,
    var typeData: TypeData,
    val includeList: MutableSet<BaseType> = mutableSetOf(),
    var beforeAddition: String = "",
    var afterAddition: String = "",
) {

    abstract fun getPath(): String

    abstract fun readOldAddition()

    private fun readIncludeClassFromMembers(list: List<MemberTypeData>): Set<String> {
        val retList = mutableSetOf<String>()
        list.forEach { memberType ->
            memberType.params.forEach { param ->
                Regex("(\\w+)::").findAll(param).forEach {
                    retList.add(it.groupValues[1])
                }
            }
            Regex("(\\w+)::").findAll(memberType.returnType ?: "").forEach {
                retList.add(it.groupValues[1])
            }
        }
        return retList.filter { inclusion ->
            HeaderOutput.generatorConfig.exclusion.inclusion.regex.find {
                inclusion.matches(
                    Regex(it)
                )
            } == null
        }
            .toSet()
    }

    private fun readList(list: List<MemberTypeData>) = readIncludeClassFromMembers(list).filter {
        val ret =
            HeaderOutput.classMap.contains(it) || HeaderOutput.structMap.contains(it) || HeaderOutput.namespaceMap.contains(
                it
            )
        if (!ret) {
            HeaderOutput.notExistBaseType.add(it)
        }
        ret
    }.map { HeaderOutput.classMap[it] ?: HeaderOutput.structMap[it] ?: HeaderOutput.namespaceMap[it]!! }
        .let(includeList::addAll)

    fun getGlobalRelativePath() = getPath().parent().relativePath("../Global.h")

    fun getRelativeInclusions(): String {
        val include = StringBuilder()
        includeList.forEach {
            include.appendLine("#include \"${(getPath().parent()).relativePath(it.getPath())}\"")
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
}