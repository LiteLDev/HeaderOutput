package com.liteldev.headeroutput.entity

import com.liteldev.headeroutput.HeaderOutput
import com.liteldev.headeroutput.config.MemberTypeData
import com.liteldev.headeroutput.config.TypeData
import com.liteldev.headeroutput.parent
import com.liteldev.headeroutput.relativePath
import com.liteldev.headeroutput.substring
import java.io.File

abstract class BaseType(
    var name: String,
    var typeData: TypeData,
    val includeList: MutableSet<BaseType> = mutableSetOf(),
    var beforeAddition: String = "",
    var afterAddition: String = "",
    var comment: String = "",
    var memberComments: MutableMap<String, String> = mutableMapOf(),
) {

    abstract fun getPath(): String

    abstract fun readOldAddition()

    abstract fun readComments()

    protected fun readComments(flag: String) {
        val regex = Regex("/\\*\\*\n([\\S\\s]+)\\*/\n$flag", RegexOption.MULTILINE)
        var origin = File(HeaderOutput.OLD_PATH, getPath()).readText().replace("\r\n", "\n")
        origin = origin.substring("#undef BEFORE_EXTRA\n", "\n#define AFTER_EXTRA") +
                origin.substring("#undef AFTER_EXTRA\n")
        comment = regex.find(origin)?.groupValues?.get(0)?.substring("", "\n$flag") ?: ""
        val classBody = origin.substring("$flag $name ", "\n};") ?: ""
        var inComment = false
        var lastComment = ""
        var symbol = ""
        classBody.split("\n").forEach {
            if (it.contains("/**")) {
                inComment = true
            }
            if (inComment) {
                lastComment += it + "\n"
                if (it.contains("@symbol")) {
                    symbol = it.substring("@symbol ", "").trim();
                }
            }
            if (it.contains("*/")) {
                inComment = false
                if (symbol.isNotEmpty()) {
                    memberComments[symbol] = lastComment
                    symbol = ""
                    lastComment = ""
                }
            }
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
            Regex("(\\w+)::").findAll(memberType.valType?.Name ?: "").forEach {
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