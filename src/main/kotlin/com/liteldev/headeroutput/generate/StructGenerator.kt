package com.liteldev.headeroutput.generate

import com.liteldev.headeroutput.HeaderOutput
import java.io.File

object StructGenerator {

    fun generate() {
        HeaderOutput.structMap.forEach { (name, structType) ->
            val hpp = File(HeaderOutput.GENERATE_PATH, structType.getPath())
            hpp.writeText(
                """
/**
 * @file  MC/$name.hpp
 *
 */
#pragma once
#define AUTO_GENERATED
#include "${structType.getGlobalRelativePath()}"
${structType.getRelativeInclusions()}
#define BEFORE_EXTRA
${structType.beforeExtra}
#undef BEFORE_EXTRA

${structType.comment}
struct $name {

#define AFTER_EXTRA
${structType.afterExtra}
#undef AFTER_EXTRA
""".trimIndent()
            )
            hpp.appendText(structType.genAntiReconstruction())
            hpp.appendText(structType.genPublic())
            hpp.appendText(structType.genProtected())
            hpp.appendText(structType.genPrivate())
            hpp.appendText("};")
        }
    }
}