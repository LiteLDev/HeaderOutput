package com.liteldev.headeroutput.generate

import com.liteldev.headeroutput.HeaderOutput
import java.io.File

object NamespaceGenerator {

    fun generate() {
        HeaderOutput.namespaceMap.forEach { (name, namespaceType) ->
            val file = File(HeaderOutput.GENERATE_PATH, namespaceType.getPath())
            file.writeText(
                """
/**
 * @file  $name.hpp
 *
 */
#pragma once
#define AUTO_GENERATED
#include "${namespaceType.getGlobalRelativePath()}"
${namespaceType.getRelativeInclusions()}
#define BEFORE_EXTRA
${namespaceType.beforeExtra}
#undef BEFORE_EXTRA

${namespaceType.comment}
namespace $name {

#define AFTER_EXTRA
${namespaceType.afterExtra}
#undef AFTER_EXTRA
${namespaceType.genPublic()}
};
""".trimIndent()
            )
        }
    }
}