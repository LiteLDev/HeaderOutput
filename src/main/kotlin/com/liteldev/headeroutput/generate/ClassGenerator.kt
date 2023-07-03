package com.liteldev.headeroutput.generate

import com.liteldev.headeroutput.config.GeneratorConfig
import com.liteldev.headeroutput.entity.BaseType
import com.liteldev.headeroutput.entity.ClassType
import java.io.File

object ClassGenerator : Generator {

    override fun generate(type: BaseType) {
        if (type !is ClassType) {
            println("ClassGenerator: ${type.name} is not ClassType")
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

class $name ${run { if (type.parent != null) ": public ${type.parent!!.name} " else "" }}{

""".trimIndent()
        )
        hpp.appendText(type.genAntiReconstruction())
        hpp.appendText(type.genPublic())
        hpp.appendText(type.genProtected())
        hpp.appendText(type.genPrivate())
        hpp.appendText(type.genProtected(genFunc = false))
        hpp.appendText(type.genPrivate(genFunc = false))
        hpp.appendText("};\n")
    }
}
