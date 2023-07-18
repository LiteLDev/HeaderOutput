package com.liteldev.headeroutput

import com.liteldev.headeroutput.config.GeneratorConfig
import com.liteldev.headeroutput.entity.BaseType
import java.io.File

object HeaderGenerator {

    const val HEADER_SUFFIX = "h"
    private const val PREDEFINE_FILE_NAME = "_HeaderOutputPredefine.$HEADER_SUFFIX"

    private val HEADER_TEMPLATE = """
#pragma once


""".trimIndent()

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
                val file = File(GeneratorConfig.generatePath, type.getPath())
                file.writeText(
                    HEADER_TEMPLATE + """
#include "${type.getPath().relativePathTo(PREDEFINE_FILE_NAME)}"

// auto generated inclusion list
${type.includeList.sorted().joinToString("\n") { "#include \"$it\"" }}


                """.trimIndent() + sb.toString()
                )
            }
        }
    }

    private fun generateNamespace(type: BaseType) {
        assert(type.isNamespace()) { "${type.name} is not namespace" }

        val file = File(GeneratorConfig.generatePath, type.getPath())
        file.writeText(
            HEADER_TEMPLATE + """
#include "${type.getPath().relativePathTo(PREDEFINE_FILE_NAME)}"

// auto generated inclusion list
${type.includeList.sorted().joinToString("\n") { "#include \"$it\"" }}


                """.trimIndent() + type.generateTypeDefine()
        )

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

#include <algorithm>     // STL general algorithms
#include <array>         // STL array container
#include <bitset>        // STL bitset container
#include <cctype>        // Character handling functions
#include <cerrno>        // C Error codes
#include <clocale>       // C localization library
#include <cmath>         // Common mathematics functions
#include <chrono>        // C++11 Time library
#include <complex>       // Complex number type
#include <cstdio>        // C Standard Input/Output library
#include <cstdlib>       // General purpose utilities: program control, dynamic memory allocation, random numbers, sort and search
#include <cstring>       // C string handling
#include <ctime>         // C Time library
#include <cwchar>        // Wide character type
#include <cwctype>       // Wide character classification
#include <deque>         // STL double ended queue container
#include <exception>     // Exception handling classes
#include <forward_list>  // STL forward list container
#include <fstream>       // File stream classes
#include <functional>    // STL Function objects
#include <iomanip>       // Input/Output manipulators
#include <ios>           // Base input/output stream classes
#include <iosfwd>        // Input/Output forward declarations
#include <iostream>      // Standard Input/Output stream objects
#include <istream>       // Basic input stream classes
#include <limits>        // Numeric limits
#include <list>          // STL linear list container
#include <map>           // STL map container
#include <memory>        // STL unique_ptr, shared_ptr, weak_ptr
#include <optional>      // STL optional type
#include <ostream>       // Basic output stream classes
#include <queue>         // STL queue and priority_queue container
#include <set>           // STL set and multiset container
#include <sstream>       // String stream classes
#include <stack>         // STL stack container
#include <stdexcept>     // Standard exception objects
#include <streambuf>     // Stream buffer classes
#include <string>        // String class
#include <string_view>   // STL string_view type
#include <unordered_map> // STL unordered map container
#include <unordered_set> // STL unordered set container
#include <utility>       // STL utility components
#include <vector>        // STL dynamic array container

""".trimIndent()
        )
    }
}
