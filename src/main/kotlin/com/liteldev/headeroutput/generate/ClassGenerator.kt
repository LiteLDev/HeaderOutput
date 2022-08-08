package com.liteldev.headeroutput.generate

import com.liteldev.headeroutput.HeaderOutput
import java.io.File

object ClassGenerator {

    fun generate() {
        HeaderOutput.classMap.forEach { (name, classType) ->
            val hpp = File(HeaderOutput.GENERATE_PATH, classType.getPath())
            hpp.writeText(
                """
// This Header is auto generated by BDSLiteLoader Toolchain
#pragma once
#define AUTO_GENERATED
#include "${classType.getGlobalRelativePath()}"
${classType.getRelativeInclusions()}
#define BEFORE_EXTRA
${classType.beforeAddition}
#undef BEFORE_EXTRA

class $name ${run { if (classType.parent != null) ": public ${classType.parent!!.name} " else "" }}{

#define AFTER_EXTRA
${classType.afterAddition}
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