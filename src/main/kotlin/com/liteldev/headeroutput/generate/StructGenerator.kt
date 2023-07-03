package com.liteldev.headeroutput.generate

import com.liteldev.headeroutput.config.GeneratorConfig
import com.liteldev.headeroutput.entity.BaseType
import com.liteldev.headeroutput.entity.StructType
import java.io.File

object StructGenerator : Generator {

    override fun generate(type: BaseType) {
        if (type !is StructType) {
            println("StructGenerator: ${type.name} is not StructType")
            return
        }
        val name = type.name
        val hpp = File(GeneratorConfig.generatePath, type.getPath())
        hpp.writeText(
            """
/**
 * @file  $name.hpp
 *
 */
#pragma once
#define AUTO_GENERATED
#include "${BaseType.GLOBAL_HEADER_PATH}"
${type.getRelativeInclusions()}

struct $name {

""".trimIndent()
        )
        hpp.appendText(type.genAntiReconstruction())
        hpp.appendText(type.genPublic())
        hpp.appendText(type.genProtected())
        hpp.appendText(type.genPrivate())
        hpp.appendText("};")
    }
}
