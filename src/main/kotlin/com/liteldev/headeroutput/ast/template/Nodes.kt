package com.liteldev.headeroutput.ast.template

import com.liteldev.headeroutput.TypeManager

enum class NodeType {
    TYPE,
    BOOLEAN,
    INTEGER,
    FLOAT,
    VARIABLE,
}

interface Flattenable {
    fun flatten()
}

sealed class Node

data class IntegerNode(val value: Int) : Node()

data class FloatNode(val value: Float) : Node()

data class BooleanNode(val value: Boolean) : Node()

data class FunctionNode(val returnType: Node, val params: List<Node>, val parent: TypeNode? = null) : Node(),
    Flattenable {
    override fun flatten() {
        if (returnType is Flattenable) {
            returnType.flatten()
        }
        parent?.flatten()
        params.filterIsInstance<Flattenable>().forEach { it.flatten() }
    }
}

data object VariableDummyNode : Node()

sealed class Specifier

data object ConstSpecifier : Specifier()

data object VolatileSpecifier : Specifier()

data object PointerSpecifier : Specifier()

data object LReferenceSpecifier : Specifier()

data object RReferenceSpecifier : Specifier()

sealed class CallSpecifier : Specifier()

data object CCDeclSpecifier : CallSpecifier()

data object CCStdCallSpecifier : CallSpecifier()

data class TypeNode(
    val name: String,
    val preSpecifiers: List<Specifier> = emptyList(),
    val postSpecifiers: List<Specifier> = emptyList(),
    val templateParams: List<Node> = emptyList(),
) : Node(), Flattenable {
    private fun mapToNodeTypes(): List<NodeType> {
        return templateParams.map {
            when (it) {
                is TypeNode -> NodeType.TYPE
                is IntegerNode -> NodeType.INTEGER
                is FloatNode -> NodeType.FLOAT
                is BooleanNode -> NodeType.BOOLEAN
                is FunctionNode -> NodeType.TYPE
                is VariableDummyNode -> NodeType.VARIABLE
            }
        }
    }

    override fun flatten() {
        if (templateParams.isEmpty()) {
            return
        }
        val existedList = TypeManager.template[name]?.toMutableList()
        if (existedList == null) {
            TypeManager.template[name] = mapToNodeTypes()
        } else {
            existedList.removeIf { it == NodeType.VARIABLE }
            val currentList = mapToNodeTypes()
            if (existedList != currentList) {
                if (existedList.size > currentList.size) {
                    val newList: MutableList<NodeType> = arrayListOf()
                    newList.addAll(currentList)
                    if (currentList.isEmpty()) {
                        newList.add(existedList[0])
                    }
                    newList.add(NodeType.VARIABLE)
                    TypeManager.template[name] = newList
                } else if (existedList.isEmpty() && currentList.isNotEmpty() && currentList[0] != NodeType.VARIABLE) {
                    existedList.add(currentList[0])
                    existedList.add(NodeType.VARIABLE)
                    TypeManager.template[name] = existedList
                } else if (existedList.size < currentList.size) {
                    existedList.add(NodeType.VARIABLE)
                    TypeManager.template[name] = existedList
                }
            }
        }
        templateParams.filterIsInstance<Flattenable>().forEach { it.flatten() }
    }
}
