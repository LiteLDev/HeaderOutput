package com.liteldev.headeroutput.entity

import com.liteldev.headeroutput.*
import com.liteldev.headeroutput.HeaderGenerator.HEADER_SUFFIX
import com.liteldev.headeroutput.config.GeneratorConfig
import com.liteldev.headeroutput.data.TypeData
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.*

abstract class BaseType(
    var name: String,
    type: TypeKind,
    var typeData: TypeData,
) {
    var type: TypeKind = type
        protected set

    var outerType: BaseType? = null

    val innerTypes: MutableSet<BaseType> = mutableSetOf()
    val referenceTypes: MutableSet<BaseType> = mutableSetOf()
    val includeList: MutableSet<String> = mutableSetOf()
    val forwardDeclareList: MutableSet<String> = mutableSetOf()

    val simpleName = name.substringAfterLast("::")
    val namespace = name.substringBeforeLast("::", "") + "::"
    val fullEscapeName = name.replace("::", "_")
    val fullUpperEscapeName = fullEscapeName.uppercase(Locale.getDefault())

    // only can use after all types are constructed
    val allReferences: Set<BaseType> by lazy {
        referenceTypes + innerTypes.flatMap { it.allReferences }
    }

    // only can use after all types are constructed
    val allInnerTypes: Set<BaseType> by lazy {
        innerTypes + innerTypes.flatMap { it.allInnerTypes }
    }

    // should be initialized after nested types are constructed and dummy types are created
    val path: String by lazy {
        val root = GeneratorConfig.rootPath
        getTopLevelFileType().run {
            val regexRules = GeneratorConfig.getSortRules().regex
            regexRules.filter { it.override }.find { this.name.matches(it.regex.toRegex()) }?.let {
                return@run "$root/${it.dst}/${this.simpleName}.$HEADER_SUFFIX"
            }
            if (declareMap.containsKey(this.name)) {
                return@run "$root/${declareMap[this.name]!!.toSnakeCase()}/${this.simpleName}.$HEADER_SUFFIX"
            }
            if (this is ClassType) {
                val parentRules = GeneratorConfig.getSortRules().parent
                parentRules.find { this.typeData.parentTypes.contains(it.parent) || this.name == it.parent }
                    ?.let {
                        return@run "$root/${it.dst}/${this.simpleName}.$HEADER_SUFFIX"
                    }
            }
            if (this is NamespaceType) {
                val namespaceRules = GeneratorConfig.getSortRules().namespace
                namespaceRules.find { this.name == it.namespace || this.name.startsWith(it.namespace + "::") }
                    ?.let {
                        return@run "$root/${it.dst}/${this.simpleName}.$HEADER_SUFFIX"
                    }
            } else {
                val namespaceRules = GeneratorConfig.getSortRules().namespace
                namespaceRules.find { this.namespace.startsWith(it.namespace + "::") }
                    ?.let {
                        return@run "$root/${it.dst}/${this.simpleName}.$HEADER_SUFFIX"
                    }
            }
            regexRules.filter { !it.override }.find { this.name.matches(it.regex.toRegex()) }?.let {
                return@run "$root/${it.dst}/${this.simpleName}.$HEADER_SUFFIX"
            }
            notSortedTypes.add(this.name)
            return@run "$root/${name.replace("::", "__")}.$HEADER_SUFFIX"
        }
    }

    abstract fun generateTypeDefine(): String

    open fun generateTypeDeclare(): String {
        error("Type $name can not generate declare, current type: ${this.javaClass.simpleName}")
    }

    open fun initIncludeAndForwardDeclareList() {}

    fun constructInnerTypeList(outerType: BaseType? = null) {
        this.outerType = outerType
        innerTypes.addAll(
            TypeManager.getAllTypes().filter {
                it.name.startsWith(this.name + "::") && !it.name.substring(this.name.length + 2).contains("::")
            }.onEach {
                it.constructInnerTypeList(this)
            }
        )
    }

    fun collectSelfReferencedType() {
        typeData.collectReferencedTypes().forEach { (name, _) ->
            TypeManager.getType(name)?.let(referenceTypes::add)
        }
    }

    enum class TypeKind {
        CLASS, STRUCT, ENUM, UNION, NAMESPACE;

        override fun toString(): String {
            return when (this) {
                CLASS -> "class"
                STRUCT -> "struct"
                ENUM -> "enum"
                UNION -> "union"
                NAMESPACE -> "namespace"
            }
        }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
        val declareMap by lazy {
            runCatching {
                File(GeneratorConfig.declareMapPath).readText().let { Json.decodeFromString<Map<String, String>>(it) }
            }.onFailure {
                logger.error { "Failed to load declare map, types will be sorted all by rules" }
            }.getOrNull() ?: emptyMap()
        }
        val notSortedTypes = hashSetOf<String>()
    }
}
