package com.liteldev.headeroutput.entity

import com.liteldev.headeroutput.HeaderGenerator.HEADER_SUFFIX
import com.liteldev.headeroutput.TypeManager
import com.liteldev.headeroutput.config.GeneratorConfig
import com.liteldev.headeroutput.data.TypeData
import com.liteldev.headeroutput.getTopLevelFileType
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

    val simpleName = name.substringAfterLast("::")
    val fullEscapeName = name.replace("::", "_")
    val fullUpperEscapeName = fullEscapeName.uppercase(Locale.getDefault())

    // should be initialized after nested types are constructed and dummy types are created
    val path by lazy {
        getTopLevelFileType().run {
            val regexRules = GeneratorConfig.getSortRules().regex
            regexRules.filter { it.override }.find { this.name.matches(it.regex.toRegex()) }?.let {
                return@run "./${it.dst}/${this.name}.$HEADER_SUFFIX"
            }
            if (declareMap.containsKey(this.name)) {
                return@run "./${declareMap[this.name]}/${this.name}.$HEADER_SUFFIX"
            }
            regexRules.filter { !it.override }.find { this.name.matches(it.regex.toRegex()) }?.let {
                return@run "./${it.dst}/${this.name}.$HEADER_SUFFIX"
            }
            if (this is ClassType) {
                val parentRules = GeneratorConfig.getSortRules().parent
                parentRules.find { this.typeData.parentTypes?.contains(it.src) == true || this.name == it.src }
                    ?.let {
                        return@run "./${it.dst}/${this.name}.$HEADER_SUFFIX"
                    }
            }
            if (this is EnumType) {
                return@run "./enums/${this.name.replace("::", "/")}.$HEADER_SUFFIX"
            }

            notSortedTypes.add(this.name)
            return@run "./${name.replace("::", "__")}.$HEADER_SUFFIX"

//            if (this.name.contains("::")) {
//                return@run "./${
//                    this.name.replace("::", "/").substringBeforeLast("/", "").toSnakeCase()
//                }/$simpleName.$HEADER_SUFFIX"
//            }
//            return@run "./$name.$HEADER_SUFFIX"
        }
    }

    abstract fun generateTypeDefine(): String

    open fun initIncludeList() {}

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

    protected fun collectAllReferencedType(): Set<BaseType> =
        referenceTypes + innerTypes.flatMap { it.collectAllReferencedType() }

    fun generateInnerTypeDefine(): String {
        val generatedTypes = innerTypes.joinToString(separator = "\n") { it.generateTypeDefine() }
        return if (generatedTypes.isNotBlank()) "\n$generatedTypes" else ""
    }

    fun collectSelfReferencedType() {
        typeData.collectReferencedTypes().forEach { (name, kind) ->
            if (!TypeManager.hasType(name)) {
                TypeManager.createDummyClass(name, kind)
            }
            TypeManager.getType(name)?.let(referenceTypes::add)
        }
    }

    enum class TypeKind {
        CLASS, STRUCT, ENUM, UNION, NAMESPACE
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
