package com.liteldev.headeroutput.ast.template

fun parse(node: String): Node {
    val trimNode = node.trim()
    if (trimNode == "true" || trimNode == "false") {
        return BooleanNode(trimNode.toBoolean())
    }
    if (!trimNode.all { it.isDigit() || it == '.' })
        return TypeNode(trimNode)
    return runCatching {
        if (trimNode.contains(".")) {
            FloatNode(trimNode.toFloat())
        } else {
            IntegerNode(trimNode.toInt())
        }
    }.onFailure { TypeNode(trimNode) }.getOrThrow()
}

fun parseType(type: String): TypeNode? {
    val stack = mutableListOf<TypeNode>()
    var temp = ""
    println(type)
    fun addChildren(node: Node) {
        if (stack.isNotEmpty()) {
            val parent = stack.removeLast()
            stack.add(parent.copy(children = parent.children + node))
        }
    }

    for (char in type) {
        when (char) {
            '<' -> {
                stack.add(TypeNode(temp.trim()))
                temp = ""
            }

            '>' -> {
                if (temp.isBlank()) {
                    if (stack.size > 1) {
                        val current = stack.removeLast()
                        val parent = stack.removeLast()
                        stack.add(parent.copy(children = parent.children + current))
                    }
                    continue
                }
                addChildren(parse(temp))
                temp = ""

                if (stack.size > 1) {
                    val current = stack.removeLast()
                    val parent = stack.removeLast()
                    stack.add(parent.copy(children = parent.children + current))
                }
            }

            ',' -> {
                if (temp.isBlank()) {
                    continue
                }

                addChildren(parse(temp))
                temp = ""
            }

            else -> temp += char
        }
    }
    return if (stack.isNotEmpty()) stack.removeLast() else null
}
