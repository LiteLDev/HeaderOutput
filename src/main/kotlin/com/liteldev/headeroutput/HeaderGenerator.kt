package com.liteldev.headeroutput

import com.liteldev.headeroutput.config.GeneratorConfig
import com.liteldev.headeroutput.entity.BaseType
import java.io.File

object HeaderGenerator {

    const val HEADER_SUFFIX = "h"
    private const val PREDEFINE_FILE_NAME = "_HeaderOutputPredefine.$HEADER_SUFFIX"

    private val HEADER_TEMPLATE = """
#pragma once

#include "$PREDEFINE_FILE_NAME"


""".trimIndent()

    fun generate() {
        TypeManager.nestingMap.forEach { (_, baseType) ->
            generate(baseType)
        }
        createPredefineFile()
    }

    private fun generate(type: BaseType) {
        when {
            type.isNamespace() -> generateNamespace(type)
            else -> {
                val sb = StringBuilder()
                var parentPath = GeneratorConfig.generatePath
                if (type.outerType != null) {
                    parentPath += type.outerType!!.getPath().removeSuffix(".$HEADER_SUFFIX")
                    sb.appendLine("namespace ${type.outerType!!.name} {")
                    sb.appendLine()
                    sb.appendLine(type.generateTypeDefine())
                    sb.appendLine("};")
                } else {
                    sb.appendLine(type.generateTypeDefine())
                }

                val targetFile = File(parentPath, type.getPath())
                targetFile.writeText(HEADER_TEMPLATE + sb.toString())
            }
        }
    }

    private fun generateNamespace(type: BaseType) {
        assert(type.isNamespace()) { "${type.name} is not namespace" }

        val file = File(GeneratorConfig.generatePath, type.getPath())
        file.writeText(HEADER_TEMPLATE + type.generateTypeDefine())

        if (type.innerTypes.isNotEmpty()) {
            File(GeneratorConfig.generatePath, type.getPath().removeSuffix(".$HEADER_SUFFIX")).also { it.mkdirs() }
            type.innerTypes.forEach { innerType ->
                generate(innerType)
            }
        }
    }

    private fun createPredefineFile() {
        val file = File(GeneratorConfig.generatePath, PREDEFINE_FILE_NAME)
        file.writeText(
            """
#pragma once

#define MCAPI __declspec(dllimport)

#include <map>
#include <memory>
#include <set>
#include <string>
#include <string_view>
#include <unordered_map>
#include <unordered_set>
#include <vector>

""".trimIndent()
        )
    }
}
