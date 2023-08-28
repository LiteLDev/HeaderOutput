package com.liteldev.headeroutput

import com.liteldev.headeroutput.config.GeneratorConfig
import com.liteldev.headeroutput.data.MemberTypeData
import com.liteldev.headeroutput.data.StorageClassType
import com.liteldev.headeroutput.data.SymbolNodeType
import com.liteldev.headeroutput.data.TypeData
import com.liteldev.headeroutput.entity.BaseType
import com.liteldev.headeroutput.entity.ClassType
import com.liteldev.headeroutput.entity.NamespaceType
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import java.io.File

@OptIn(ExperimentalSerializationApi::class)
private val json = Json { explicitNulls = false }

object HeaderOutput {
    private val logger = KotlinLogging.logger { }

    private lateinit var originData: JsonObject
    lateinit var classNameList: MutableSet<String>
    lateinit var structNameList: MutableSet<String>
    lateinit var typeDataMap: MutableMap<String, TypeData>

    @JvmStatic
    fun main(args: Array<String>) {
        if (!readCommandLineArgs(args)) return

        GeneratorConfig.loadConfig()
        loadOriginData()
        loadIdentifiedTypes()
        constructTypes()

        TypeManager.constructNotExistedType()
        TypeManager.initParents()
        TypeManager.initReferences()
        TypeManager.initNestingMap()
        TypeManager.initInclusionList()

        HeaderGenerator.generate()
        val duplicateFiles = HeaderGenerator.pathMap.filterValues { it.size > 1 }
        if (duplicateFiles.isNotEmpty()) {
            logger.warn { "These files are generated more than once:" }
            duplicateFiles.toSortedMap().forEach { (path, types) ->
                logger.warn { "$path: ${types.joinToString { it.name }}" }
            }
        }

        logger.warn { "These types are not sorted by any rules or declare map: ${BaseType.notSortedTypes.sorted()}" }
    }

    private fun readCommandLineArgs(args: Array<String>): Boolean {
        val parser = ArgParser("HeaderOutput")
        val configPath by parser.option(ArgType.String, "config", "c", "The config file path").default("./config.toml")
        val declareMapFile by parser.option(ArgType.String, "declare-map", "d", "The declare map file path")
            .default("./declareMap.json")
        val predefineHeader by parser.option(ArgType.String, "predefine-header", "p", "The predefine header file path")
            .default("./predefine.h")
        val generatePath by parser.option(ArgType.String, "output-dir", "o", "The header output path")
            .default("./header")
        val jsonPath by parser.option(ArgType.String, "input", "i", "The original data json file path")
            .default("./originalData.json")
        parser.parse(args)
        GeneratorConfig.configPath = configPath
        GeneratorConfig.generatePath = generatePath
        GeneratorConfig.jsonPath = jsonPath
        GeneratorConfig.declareMapPath = declareMapFile
        GeneratorConfig.predefineHeaderPath = predefineHeader
        if (!File(GeneratorConfig.configPath).isFile) {
            logger.error { "Invalid config file path" }
            return false
        }
        if (!File(GeneratorConfig.generatePath).isDirectory) {
            if (!File(GeneratorConfig.generatePath).mkdirs()) {
                logger.error { "Fail to create generate header files path" }
                return false
            }
        }
        if (!File(GeneratorConfig.jsonPath).isFile) {
            logger.error { "Invalid original data json file path" }
            return false
        }
        if (!File(GeneratorConfig.declareMapPath).isFile) {
            logger.error { "Invalid declare map file path" }
            return false
        }
        if (!File(GeneratorConfig.predefineHeaderPath).isFile) {
            logger.error { "Invalid predefine header file path" }
            return false
        }
        return true
    }


    private fun loadOriginData() {
        logger.info { "Loading origin data..." }
        val configText = File(GeneratorConfig.jsonPath).readText()
        originData = Json.parseToJsonElement(configText).jsonObject
        typeDataMap = originData["classes"]?.jsonObject?.mapValues { entry ->
            json.decodeFromJsonElement<TypeData>(entry.value)
        }?.toMutableMap() ?: mutableMapOf()
        typeDataMap.values.forEach { type ->
            var counter = 0
            type.virtual?.forEach {
                // 对于没有名字的虚函数，将其标记为未知函数，并且将其名字设置为 __unk_vfn_0, __unk_vfn_1, ...
                if (it.name == "" && !it.isUnknownFunction())
                    it.symbolType = SymbolNodeType.Unknown
                if (it.isUnknownFunction()) {
                    it.storageClass = StorageClassType.Virtual
                    it.addFlag(MemberTypeData.FLAG_PTR_CALL)
                    it.name = "void __unk_vfn_${counter}"
                }
                counter++

            }
        }
    }

    private fun constructTypes() {
        val notIdentifiedTypes = mutableSetOf<String>()
        logger.info { "Loading types..." }
        typeDataMap
            .filterNot { (k, _) -> GeneratorConfig.isExcludedFromGeneration(k) }
            .forEach { (typeName, type) ->
                TypeManager.addType(
                    typeName,
                    when {
                        isStruct(typeName) -> ClassType(typeName, type, isStructType = true)
                        isClass(typeName) -> ClassType(typeName, type)
                        isNameSpace(typeName, type) -> NamespaceType(typeName, type)
                        else -> {
                            notIdentifiedTypes.add(typeName)
                            ClassType(typeName, type)
                        }
                    }
                )
            }
        logger.debug { "Can not determine these types' type. Treat them as class type" }
        logger.debug { notIdentifiedTypes.joinToString() }
    }

    private fun loadIdentifiedTypes() {
        logger.info { "Loading identifier..." }
        val identifier = originData["identifier"]?.jsonObject
        classNameList =
            (identifier?.get("class")?.jsonArray).orEmpty().map { it.jsonPrimitive.content }.toMutableSet()
        structNameList =
            (identifier?.get("struct")?.jsonArray).orEmpty().map { it.jsonPrimitive.content }.toMutableSet()

        // check if any type is not identified but derived from other types
        typeDataMap.values
            .flatMap { it.collectReferencedTypes().toList() }
            .filter { it.first in originData["classes"]?.jsonObject?.keys.orEmpty() }
            .toMutableSet()
            .forEach {
                when (it.second) {
                    BaseType.TypeKind.CLASS -> classNameList.add(it.first)
                    BaseType.TypeKind.STRUCT -> structNameList.add(it.first)
                    else -> {}
                }
            }
    }

    private fun isNameSpace(typeName: String, typeData: TypeData): Boolean {
        if (isStruct(typeName) || isClass(typeName))
            return false
        if (listOf(
                typeData.privateTypes,
                typeData.privateStaticTypes,
                typeData.protectedTypes,
                typeData.protectedStaticTypes,
                typeData.publicStaticTypes,
                typeData.virtual,
                typeData.vtblEntry
            ).any { it != null }
        ) return false
        return typeData.publicTypes?.none { it.isPtrCall() } == true
    }

    private fun isStruct(typeName: String) = structNameList.contains(typeName)


    private fun isClass(typeName: String) = classNameList.contains(typeName)

}
