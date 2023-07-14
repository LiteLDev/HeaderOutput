package com.liteldev.headeroutput

import com.liteldev.headeroutput.config.origindata.TypeData
import com.liteldev.headeroutput.entity.BaseType
import com.liteldev.headeroutput.entity.ClassType

object TypeManager {
    private val typeMap = hashMapOf<String, BaseType>()

    val nestingMap = hashMapOf<String, BaseType>()
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

    fun initParents() {
        typeMap.values.filter { it.isClass() }.forEach { type ->
            type as ClassType
            type.typeData.parentTypes?.getOrNull(0)?.run { typeMap[this] }?.let { type.parents.add(it) }
            /*
            fixme: Fix in header generator: recursive parent
            type.typeData.parentTypes?.forEach { parent ->
                getType(parent)?.let { type.parents.add(it) }
            }
             */
        }
    }

    fun initReferences() {
        typeMap.values.forEach { type ->
            type.collectSelfReferencedType()
        }
    }

    fun initInclusionList() {
        typeMap.forEach { (_, type) ->
            type.initIncludeList()
        }
    }

    /**
     * @param name: the type's name to be created, must be an inner type
     */
    private fun createDummyClass(name: String): BaseType {
        assert(!hasType(name)) { "type $name already exists" }

        val dummyClass =
            ClassType(name, TypeData(null, null, null, null, null, null, null, null, null, null, null))

        if (name.contains("::")) {
            val parentName = name.substringBeforeLast("::")
            if (!hasType(parentName)) {
                createDummyClass(parentName)
            }

            val parentType = getType(parentName)!!
            parentType.innerTypes.add(dummyClass)
            dummyClass.outerType = parentType
        } else {
            nestingMap[name] = dummyClass
        }

        addType(name, dummyClass)
        return dummyClass
    }

    fun initNestingMap() {
        typeMap.filter { !it.key.contains("::") }
            .forEach { (key, value) ->
                nestingMap[key] = value
                value.constructInnerTypeList()
            }
        // 收集所有形成嵌套关系的类，检查哪些类没有被收集到
        val allNestingType = nestingMap.values.flatMap { it.innerTypes }.toMutableSet()
        allNestingType.addAll(nestingMap.values)
        val allType = typeMap.values.toSet()
        val notNestingType = allType - allNestingType
        println("Warning: these class has no nesting relationship\n${notNestingType.map { it.name }}")
        // generate a dummy class for each not nesting class
        notNestingType.forEach {
            val parentName = it.name.substringBeforeLast("::")
            if (!hasType(parentName)) {
                createDummyClass(parentName)
            }
            val parentType = getType(parentName)!!
            parentType.innerTypes.add(it)
            it.outerType = parentType
        }
    }

}
