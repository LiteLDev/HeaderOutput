package com.liteldev.headeroutput.generate

import com.liteldev.headeroutput.config.GeneratorConfig
import com.liteldev.headeroutput.entity.BaseType
import com.liteldev.headeroutput.entity.NamespaceType
import java.io.File

object NamespaceGenerator : Generator {

    override fun generate(type: BaseType) {
        if (type !is NamespaceType) {
            println("NamespaceGenerator: ${type.name} is not NamespaceType")
            return
        }
        val name = type.name
        val file = File(GeneratorConfig.generatePath, type.getPath())
        file.writeText(
            """
/**
 * @file  $name.hpp
 *
 */
#pragma once
#define AUTO_GENERATED
#include "${BaseType.GLOBAL_HEADER_PATH}"
${type.getRelativeInclusions()}

namespace $name {

${type.genPublic()}
};
""".trimIndent()
        )
    }
}
