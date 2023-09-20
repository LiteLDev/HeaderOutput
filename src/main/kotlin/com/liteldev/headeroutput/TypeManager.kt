package com.liteldev.headeroutput

import com.liteldev.headeroutput.ast.template.NodeType
import com.liteldev.headeroutput.config.GeneratorConfig
import com.liteldev.headeroutput.data.TypeData
import com.liteldev.headeroutput.entity.*
import com.liteldev.headeroutput.entity.BaseType.TypeKind
import io.github.oshai.kotlinlogging.KotlinLogging

object TypeManager {
    private val logger = KotlinLogging.logger { }
    private val typeMap = hashMapOf<String, BaseType>()

    val typeDataMap = hashMapOf<String, TypeData>()
    private val typeKinds by lazy {
        logger.info { "Collecting type kinds" }
        val map = hashMapOf<String, TypeKind>()
        val typeDataSet = typeDataMap.values
        typeDataSet.forEach {
            map.putAll(it.collectReferencedTypes())
        }
        map
    }
    val nestingMap = hashMapOf<String, BaseType>()
    val template = hashMapOf<String, List<NodeType>>()
    val classTypeNames by lazy {
        logger.info { "Collecting class type names" }
        val set = mutableSetOf<String>()
        set.addAll(HeaderOutput.classNameList)
        set.addAll(HeaderOutput.structNameList)
        typeDataMap.values.map { it.parentTypes }.flatten().let(set::addAll)
        typeKinds.keys.let(set::addAll)
        set
    }

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
        logger.info { "Constructing not existed types" }
        typeKinds.forEach { (name, kind) ->
            if (!hasType(name)) {
                createDummyClass(name, kind)
            }
        }
    }

    fun initParents() {
        logger.info { "Initializing parents" }
        typeMap.values.filterIsInstance<ClassType>().forEach { type ->
            type.typeData.parentTypes.getOrNull(0)?.run { typeMap[this] }
                ?.let { type.parents.add(it); type.referenceTypes.add(it) }
            /*
            fixme: Fix in header generator: recursive parent
            type.typeData.parentTypes?.forEach { parent ->
                getType(parent)?.let { type.parents.add(it) }
            }
             */
        }
    }

    fun initReferences() {
        logger.info { "Initializing references" }
        typeMap.values.forEach(BaseType::collectSelfReferencedType)
    }

    fun initInclusionList() {
        logger.info { "Initializing inclusion list" }
        typeMap.forEach { (_, type) ->
            type.initIncludeAndForwardDeclareList()
        }
    }

    fun initNestingMap() {
        logger.info { "Initializing nesting map" }
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
        logger.debug { "These class has no nesting relationship" }
        logger.debug { notNestingType.joinToString { it.name } }
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
        require(!hasType(name)) { "type $name already exists" }

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
            TypeKind.CLASS -> ClassType(name, TypeData.empty(), isTemplateClass = template.contains(name))
            TypeKind.STRUCT -> ClassType(
                name,
                TypeData.empty(),
                isStruct = true,
                isTemplateClass = template.contains(name)
            )

            TypeKind.ENUM -> EnumType(name)
            TypeKind.UNION -> UnionType(name)
            TypeKind.NAMESPACE -> NamespaceType(name, TypeData.empty())
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
