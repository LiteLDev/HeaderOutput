package com.liteldev.headeroutput

import com.liteldev.headeroutput.config.GeneratorConfig
import com.liteldev.headeroutput.data.TypeData
import com.liteldev.headeroutput.entity.*
import com.liteldev.headeroutput.entity.BaseType.TypeKind
import io.github.oshai.kotlinlogging.KotlinLogging

object TypeManager {
    private val logger = KotlinLogging.logger { }
    private val typeMap = hashMapOf<String, BaseType>()

    val nestingMap = hashMapOf<String, BaseType>()
    val template = hashMapOf<String, String>()

    private val classTypeNames = hashSetOf<String>()

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

    fun constructNotExistedType() {
        val typeDataSet = HeaderOutput.typeDataMap.values
        val waitingForConstruct = mutableMapOf<String, TypeKind>()
        typeDataSet.forEach {
            it.collectReferencedTypes().forEach { (name, kind) ->
                if (!hasType(name)) {
                    waitingForConstruct[name] = kind
                }
            }
        }
        classTypeNames.addAll(HeaderOutput.classNameList)
        classTypeNames.addAll(HeaderOutput.structNameList)
        waitingForConstruct.forEach { (name, kind) ->
            createDummyClass(name, kind)
        }
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
        // copy to avoid ConcurrentModificationException due to enum is being added in `collectSelfReferencedType`
        typeMap.values.toMutableSet().forEach { type ->
            type.collectSelfReferencedType()
        }
    }

    fun initInclusionList() {
        typeMap.forEach { (_, type) ->
            type.initIncludeAndForwardDeclareList()
        }
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
        logger.warn { "These class has no nesting relationship\n${notNestingType.map { it.name }}" }
        // generate a dummy class for each not nesting class
        notNestingType.forEach {
            val parentName = it.name.substringBeforeLast("::")
            if (!hasType(parentName)) {
                createDummyClass(parentName)
            }
            val parentType = getType(parentName) ?: return@forEach
            parentType.innerTypes.add(it)
            it.outerType = parentType
        }
    }

    /**
     * @param name: the type's name to be created, must be an inner type
     */
    private fun createDummyClass(name: String, type: TypeKind? = null): BaseType? {
        assert(!hasType(name)) { "type $name already exists" }

        if (GeneratorConfig.isExcludedFromGeneration(name)) {
            return null
        }

        val dummyClass = if (type == null) {
            if (typeMap.any { (n, t) -> n.startsWith("$name::") && t.isNamespace() }) {
                NamespaceType(name, TypeData.empty())
            } else if (!classTypeNames.contains(name) && classTypeNames.none { name.startsWith("$it::") }) {
                NamespaceType(name, TypeData.empty())
            } else {
                ClassType(name, TypeData.empty(), template.contains(name))
            }
        } else when (type) {
            TypeKind.CLASS -> ClassType(name, TypeData.empty(), template.contains(name))
            TypeKind.STRUCT -> StructType(name, TypeData.empty(), template.contains(name))
            TypeKind.ENUM -> EnumType(name)
            TypeKind.NAMESPACE -> NamespaceType(name, TypeData.empty())
            else -> throw IllegalArgumentException("type $type is not supported")
        }

        if (name.contains("::")) {
            val parentName = name.substringBeforeLast("::")
            if (!hasType(parentName)) {
                createDummyClass(parentName)
            }

            val parentType = getType(parentName) ?: return null
            parentType.innerTypes.add(dummyClass)
            dummyClass.outerType = parentType
        } else {
            nestingMap[name] = dummyClass
        }

        addType(name, dummyClass)
        return dummyClass
    }
}
