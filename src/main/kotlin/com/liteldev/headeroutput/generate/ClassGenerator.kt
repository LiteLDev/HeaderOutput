package com.liteldev.headeroutput.generate

import com.liteldev.headeroutput.HeaderOutput
import java.io.File

object ClassGenerator {

    fun generate() {
        HeaderOutput.classMap.forEach { (name, classType) ->
            val hpp = File(HeaderOutput.GENERATE_PATH, classType.getPath())
            hpp.writeText(
                """
/**
 * @file  $name.hpp
 *
 */
#pragma once
#define AUTO_GENERATED
#include "${classType.getGlobalHeaderPath()}"
${classType.getRelativeInclusions()}
#define BEFORE_EXTRA
${classType.beforeExtra}
#undef BEFORE_EXTRA

${classType.comment}
class $name ${run { if (classType.parent != null) ": public ${classType.parent!!.name} " else "" }}{

#define AFTER_EXTRA
${classType.afterExtra}
#undef AFTER_EXTRA
""".trimIndent()
            )
            hpp.appendText(classType.genAntiReconstruction())
            hpp.appendText(classType.genPublic())
            hpp.appendText(classType.genProtected())
            hpp.appendText(classType.genPrivate())
            hpp.appendText(classType.genProtected(genFunc = false))
            hpp.appendText(classType.genPrivate(genFunc = false))
            hpp.appendText("};")

        }
    }
}