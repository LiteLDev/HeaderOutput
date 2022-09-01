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
    var beforeExtra: String = "",
    var afterExtra: String = "",
    var comment: String = "",
    var memberComments: MutableMap<Int, String> = mutableMapOf(),
) {

    abstract fun getPath(): String

    abstract fun readOldAddition()

    abstract fun readComments()

    protected fun readComments(flag: String) {
        val regex = Regex("/\\*\\*\n([\\S\\s]+)\\*/\n$flag", RegexOption.MULTILINE)
        var origin = File(HeaderOutput.OLD_PATH, getPath()).readText().replace("\r\n", "\n")
        origin = origin.substring("#undef BEFORE_EXTRA\n", "\n#define AFTER_EXTRA") +
                origin.substringAfter("#undef AFTER_EXTRA\n")
        comment = regex.find(origin)?.groupValues?.get(0)?.substring("", "\n$flag") ?: ""
        val classBody = origin.substring("$flag $name ", "\n};")
        var inComment = false
        val comment = StringBuilder()
        var hash: Int? = null
        classBody.lines().forEach {
            when {
                it.contains("/*") -> inComment = true
                it.contains("*/") -> {
                    inComment = false
                    hash?.let { h ->
                        memberComments[h] = comment.toString()
                        hash = null
                    }
                    comment.clear()
                }

                inComment -> {
                    when {
                        it.contains("@hash") -> hash = it.substringAfter("@hash ", "").trim().toIntOrNull()
                        it.contains("@vftbl") -> {}
                        it.contains("@symbol") -> {}
                        else -> comment.append(it).append("\n")
                    }
                }
            }
        }
    }

    fun getCommentOf(member: MemberTypeData): String {
        return this.memberComments[member.hashCode()] ?: ""
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