package com.liteldev.headeroutput.ast.template

import com.liteldev.headeroutput.data.TypeData

sealed class Node

data class IntegerNode(val value: Int) : Node()

data class FloatNode(val value: Float) : Node()

data class BooleanNode(val value: Boolean) : Node()

data class TypeNode(val name: String, val children: List<Node> = emptyList()) : Node() {
    fun genTemplateDeclares(): String? {
        if (children.isEmpty()) {
            return null
        }
        var argCounter = 0
        return "template<${
            children.joinToString(", ") {
                when (it) {
                    is TypeNode -> "typename T${argCounter++}"
                    is IntegerNode -> "int T${argCounter++}"
                    is FloatNode -> "float T${argCounter++}"
                    is BooleanNode -> "bool T${argCounter++}"
                }
            }
        }>"
    }

    fun pureName(): String? {
        return TypeData.typeMatchRegex.find(name)?.groupValues?.get(2)
    }

    fun flatten(): Map<String, String> {
        val map = mutableMapOf<String?, String?>()
        map[pureName()] = genTemplateDeclares()
        children.filterIsInstance<TypeNode>().forEach { map.putAll(it.flatten()) }
        return map.mapNotNull { (k, v) -> if (k == null || v == null) return@mapNotNull null else k to v }.toMap()
    }

}
