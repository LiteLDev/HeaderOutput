package com.liteldev.headeroutput

import com.liteldev.headeroutput.config.origindata.TypeData
import com.liteldev.headeroutput.entity.BaseType
import com.liteldev.headeroutput.entity.ClassType
import com.liteldev.headeroutput.entity.NamespaceType
import com.liteldev.headeroutput.entity.StructType

object TypeManager {
    private val typeMap = hashMapOf<String, BaseType>()
    private val nestingMap = hashMapOf<String, BaseType>()

    lateinit var classNameList: List<String>
    lateinit var structNameList: List<String>

    fun addType(fullName: String, type: BaseType) {
        typeMap[fullName] = type
    }

    fun getType(fullName: String): BaseType? {
        return typeMap[fullName]
    }

    fun hasType(fullName: String): Boolean {
        return typeMap.containsKey(fullName)
    }

    fun getAllTypes(): List<BaseType> {
        return typeMap.values.toList()
    }

    fun initInclusionList() {
        typeMap.forEach { (_, type) ->
            type.initIncludeList()
        }
    }

    fun generateNestingMap() {
        typeMap.forEach { (fullName, type) ->
            val parent = type.typeData.parentTypes?.firstOrNull()
            if (parent != null) {
                nestingMap[fullName] = typeMap[parent]!!
            }
        }
    }

}

fun BaseType.isClass(): Boolean = this is ClassType && !this.isStruct()

fun BaseType.isStruct() = this is StructType
fun BaseType.isNamespace() = this is NamespaceType

fun isNameSpace(typeName: String, typeData: TypeData): Boolean {
    if (typeData.privateTypes != null
        || typeData.privateStaticTypes != null
        || typeData.protectedTypes != null
        || typeData.protectedStaticTypes != null
        || typeData.publicStaticTypes != null
        || typeData.virtual != null
        || typeData.vtblEntry != null
    ) {
        return false
    }
    val isClassOrStruct = isStruct(typeName) || isClass(typeName)
    return (typeData.publicTypes?.find { it.isPtrCall() } == null && !isClassOrStruct)
}

fun isStruct(typeName: String): Boolean {
    return TypeManager.structNameList.contains(typeName)
}

fun isClass(typeName: String): Boolean {
    return TypeManager.classNameList.contains(typeName)
}
