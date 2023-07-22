package com.liteldev.headeroutput

import com.liteldev.headeroutput.config.GeneratorConfig
import com.liteldev.headeroutput.entity.BaseType
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File

object HeaderGenerator {

    const val HEADER_SUFFIX = "h"
    private const val PREDEFINE_FILE_NAME = "_HeaderOutputPredefine.$HEADER_SUFFIX"
    private const val HEADER_TEMPLATE = "#pragma once\n\n"
    private val logger = KotlinLogging.logger { }

    fun generate() {
        File(GeneratorConfig.generatePath).mkdirs()
        createPredefineFile()
        TypeManager.nestingMap.forEach { (_, baseType) ->
            generate(baseType)
        }
    }

    private fun generate(type: BaseType) {
        when {
            type.isNamespace() -> generateNamespace(type)
            else -> {
                val sb = StringBuilder()
                if (type.outerType != null) {
                    sb.appendLine("namespace ${type.outerType!!.name} {")
                    sb.appendLine()
                    sb.appendLine(type.generateTypeDefine())
                    sb.appendLine("};")
                } else {
                    sb.appendLine(type.generateTypeDefine())
                }
                val file = File(GeneratorConfig.generatePath, type.path)
                file.parentFile.mkdirs()
                file.writeText(
                    buildString {
                        append(HEADER_TEMPLATE)
                        append("#include \"${type.path.relativePathTo(PREDEFINE_FILE_NAME)}\"\n\n")
                        append(generateIncludesAndForwardDeclare(type))
                        append(sb.toString())
                    }
                )
            }
        }
    }

    private fun generateNamespace(type: BaseType) {
        assert(type.isNamespace()) { "${type.name} is not namespace" }

        val file = File(GeneratorConfig.generatePath, type.path)
        file.parentFile.mkdirs()
        file.writeText(
            buildString {
                append(HEADER_TEMPLATE)
                append("#include \"${type.path.relativePathTo(PREDEFINE_FILE_NAME)}\"\n\n")
                append(generateIncludesAndForwardDeclare(type))
                append(type.generateTypeDefine())
            }
        )

        if (type.innerTypes.isNotEmpty()) {
            type.innerTypes.forEach { innerType ->
                generate(innerType)
            }
        }
    }

    private fun generateIncludesAndForwardDeclare(type: BaseType): String {
        return buildString {
            if (type.includeList.isNotEmpty()) {
                appendLine("// auto generated inclusion list")
                appendLine(type.includeList.sorted()
                    .joinToString("\n") { "#include \"$it\"" })
                appendLine()
            }
            if (type.forwardDeclareList.isNotEmpty()) {
                appendLine("// auto generated forward declare list")
                appendLine("// clang-format off")
                appendLine(type.forwardDeclareList.sorted()
                    .joinToString("\n") { it })
                appendLine("// clang-format on\n")
            }
        }
    }

    private fun createPredefineFile() {
        val file = File(GeneratorConfig.generatePath, PREDEFINE_FILE_NAME)
        file.writeText(File(GeneratorConfig.predefineHeaderPath).readText())
    }
}
