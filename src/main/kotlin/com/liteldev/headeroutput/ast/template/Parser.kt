package com.liteldev.headeroutput.ast.template


class Parser(private val template: String) {
    private var cursor = 0

    private fun postParseFunctionType(returnType: TypeNode, parent: TypeNode? = null): FunctionNode {
        val params = mutableListOf<Node>()
        if (!consume(")")) {
            params.add(parseTypeOrBooleanOrNumber())
            while (consume(",")) {
                consumeBlank()
                params.add(parseTypeOrBooleanOrNumber())
            }
            consumeBlank()
            if (!consume(")")) {
                expect(")")
            }
        }
        parseSpecifier()
        consumeBlank()
        return FunctionNode(returnType, params, parent)
    }

    private fun preParseFunctionType(returnType: TypeNode): Node {
        consumeBlank()
        if (!consume("(")) {
            return returnType
        }
        val currentPos = cursor
        consumeBlank()
        val parent = runCatching { parseType(false) }.onFailure { cursor = currentPos }.getOrNull()
        if (parent is TypeNode) {
            if (consume(")(")) {
                consumeBlank()
                return postParseFunctionType(returnType, parent)
            } else {
                cursor = currentPos
            }
        }
        consumeBlank()
        val specifier = parseSpecifier()
        consumeBlank()
        if (specifier.isNotEmpty()) {
            if (!consume(")(")) {
                cursor = currentPos
            } else {
                consumeBlank()
                return postParseFunctionType(returnType, TypeNode("*"))
            }
        }
        consumeBlank()
        return postParseFunctionType(returnType)
    }

    private fun parseType(requireType: Boolean = true): Node? {
        consumeBlank()
        val preSpecifiers = parseSpecifier()
        consumeBlank()
        if (consume("class") || consume("struct") || !requireType) {
            val name = parseTypeName(!requireType) ?: expect("type name")
            val children = mutableListOf<Node>()
            consumeBlank()
            if (consume("<")) {
                consumeBlank()
                if (!consume(">")) {
                    children.add(parse() ?: expectTypeOrBooleanOrNumber())
                    while (consume(",")) {
                        consumeBlank()
                        children.add(parse() ?: expectTypeOrBooleanOrNumber())
                        consumeBlank()
                    }
                    if (!consume(">")) {
                        expect(">")
                    }
                }
                if (children.isEmpty()) {
                    children.add(VariableDummyNode)
                }
            }
            val postSpecifiers = parseSpecifier()
            val returnType = TypeNode(name, preSpecifiers, postSpecifiers, children)
            return preParseFunctionType(returnType)
        } else if (consume("enum", true)) {
            val name = parseTypeName() ?: expect("type name")
            val postSpecifiers = parseSpecifier()
            val returnType = TypeNode(name, preSpecifiers, postSpecifiers)
            return preParseFunctionType(returnType)
        } else {
            var primitiveType = ""
            while (true) {
                consumeBlank()
                primitiveType += when {
                    consume("unsigned", true) -> "unsigned "
                    consume("signed", true) -> "signed "
                    consume("long", true) -> "long "
                    consume("short", true) -> "short "
                    consume("int", true) -> "int "
                    consume("char", true) -> "char "
                    consume("float", true) -> "float "
                    consume("double", true) -> "double "
                    consume("bool", true) -> "bool "
                    consume("__int64", true) -> "__int64 "
                    consume("void", true) -> "void "
                    consume("wchar_t", true) -> "wchar_t "
                    else -> break
                }
            }
            primitiveType.trim()
            if (primitiveType.isEmpty()) {
                if (preSpecifiers.isNotEmpty()) {
                    expect("primitive type")
                }
                return null
            }
            val postSpecifiers = parseSpecifier()
            val returnType = TypeNode(primitiveType, preSpecifiers, postSpecifiers)
            return preParseFunctionType(returnType)
        }
    }

    private fun parseTypeName(allowEndWithColon: Boolean = false): String? {
        consumeBlank()
        // first char can't be digit
        if (cursor < template.length && template[cursor].isDigit()) {
            return null
        }
        val sb = StringBuilder()
        while (cursor < template.length) {
            val char = template[cursor]
            if (char.isNormalLetter() || char == ':') {
                sb.append(char)
                cursor++
            } else {
                break
            }
        }
        if (sb.isEmpty()) {
            return null
        }
        // : can't be the last char and can't be the first char
        if (!allowEndWithColon && (sb.last() == ':' || sb.first() == ':')) {
            return null
        }
        // 所有 : 都必须连续成对出现
        if (!checkColonPairs(sb.toString())) {
            return null
        }
        return sb.toString()
    }

    fun parseVariableName(): String? {
        consumeBlank()
        // first char can't be digit
        if (cursor < template.length && template[cursor].isDigit()) {
            return null
        }
        val sb = StringBuilder()
        while (cursor < template.length) {
            val char = template[cursor]
            if (char.isNormalLetter()) {
                sb.append(char)
                cursor++
            } else {
                break
            }
        }
        if (sb.isEmpty()) {
            return null
        }
        return sb.toString()
    }

    private fun parseBoolean(): BooleanNode? {
        consumeBlank()
        return if (consume("true", true)) {
            BooleanNode(true)
        } else if (consume("false", true)) {
            BooleanNode(false)
        } else {
            null
        }
    }

    private fun parseNumber(): Node? {
        consumeBlank()
        val sb = StringBuilder()
        while (cursor < template.length) {
            val char = template[cursor]
            if (char.isDigit() || char == '.' || char == '-') {
                sb.append(char)
                cursor++
            } else {
                break
            }
        }
        if (sb.isEmpty()) {
            return null
        }
        return runCatching {
            if (sb.contains(".")) {
                FloatNode(sb.toString().toFloat())
            } else {
                IntegerNode(sb.toString().toInt())
            }
        }.onFailure { TypeNode(sb.toString()) }.getOrThrow()
    }

    fun parseSpecifier(): List<Specifier> {
        val specifiers = mutableListOf<Specifier>()
        while (true) {
            when {
                consume("const", true) -> specifiers.add(ConstSpecifier)
                consume("volatile", true) -> specifiers.add(VolatileSpecifier)
                consume("*") -> specifiers.add(PointerSpecifier)
                consume("&&") -> specifiers.add(RReferenceSpecifier)
                consume("&") -> specifiers.add(LReferenceSpecifier)
                consume("__cdecl", true) -> specifiers.add(CCDeclSpecifier)
                consume("__stdcall", true) -> specifiers.add(CCStdCallSpecifier)
                else -> break
            }
        }
        return specifiers
    }

    fun parse(): Node? {
        return parseType() ?: parseBoolean() ?: parseNumber()
    }

    private fun consume(str: String, nextIsNotLetter: Boolean = false): Boolean {
        consumeBlank()
        if (template.substring(cursor).startsWith(str)) {
            cursor += str.length
            if (nextIsNotLetter) {
                if (cursor < template.length && template[cursor].isLetterOrDigit()) {
                    cursor -= str.length
                    return false
                }
            }
            return true
        }
        return false
    }

    private fun consumeBlank() {
        while (cursor < template.length) {
            if (template[cursor].isWhitespace()) {
                cursor++
            } else {
                break
            }
        }
    }

    private fun Char.isNormalLetter(): Boolean {
        // true for unicode can be used in variable name
        return this !in "<>,\n\t\r?*&:;{}()[]=+-/\"'`~!@#$%^|.\\ "
    }

    private fun checkColonPairs(input: String): Boolean {
        var colonCount = 0
        for (char in input) {
            if (char == ':') {
                colonCount++
            } else {
                if (colonCount % 2 != 0) return false
                colonCount = 0
            }
        }
        return colonCount % 2 == 0
    }

    private fun parseTypeOrBooleanOrNumber(): Node {
        return parseType() ?: parseBoolean() ?: parseNumber() ?: expectTypeOrBooleanOrNumber()
    }

    private fun printError(error: String): Nothing =
        throw ParseException("Parse error: $error\n$template\n${" ".repeat(cursor)}^")

    private fun expectTypeOrBooleanOrNumber(): Nothing =
        printError("expect type or boolean or number")

    private fun expect(str: String): Nothing =
        printError("expect $str")
}
