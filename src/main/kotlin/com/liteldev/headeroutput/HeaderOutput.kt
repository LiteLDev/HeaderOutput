package com.liteldev.headeroutput

import com.liteldev.headeroutput.config.GeneratorConfig
import com.liteldev.headeroutput.config.origindata.MemberTypeData
import com.liteldev.headeroutput.config.origindata.TypeData
import com.liteldev.headeroutput.entity.*
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import java.io.File

@OptIn(ExperimentalSerializationApi::class)
private val json = Json { explicitNulls = false }

fun main(args: Array<String>) {
    HeaderOutput.run(args)
}

object HeaderOutput {
    private lateinit var originData: JsonObject

    val notExistBaseType = mutableSetOf<String>()

    fun run(args: Array<String>) {
        if (!readCommandLineArgs(args)) return

        GeneratorConfig.loadConfig()
        loadOriginData()
        loadIdentifiedTypes()
        loadTypes()

        TypeManager.initParents()
        TypeManager.initInclusionList()

        println("Warning: these class has no information in originData but used by other classes\n$notExistBaseType")

        File(GeneratorConfig.generatePath).mkdirs()

        TypeManager.generateNestingMap()

        HeaderGenerator.generate()
    }

    private fun readCommandLineArgs(args: Array<String>): Boolean {
        val parser = ArgParser("HeaderOutput")
        val configPath by parser.option(ArgType.String, "config", "c", "The config file path").default("./config.json")
        val generatePath by parser.option(ArgType.String, "output-dir", "o", "The header output path")
            .default("./header")
        val jsonPath by parser.option(ArgType.String, "input", "i", "The original data json file path")
            .default("./header.json")
        parser.parse(args)
        GeneratorConfig.configPath = configPath
        GeneratorConfig.generatePath = generatePath
        GeneratorConfig.jsonPath = jsonPath
        if (!File(GeneratorConfig.configPath).isFile) {
            println("Invalid config file path")
            return false
        }
        if (!File(GeneratorConfig.generatePath).isDirectory) {
            if (!File(GeneratorConfig.generatePath).mkdirs()) {
                println("Fail to create generate header files path")
                return false
            }
        }
        if (!File(GeneratorConfig.jsonPath).isFile) {
            println("Invalid original data json file path")
            return false
        }
        return true
    }


    private fun loadOriginData() {
        println("Loading origin data...")
        val configText = File(GeneratorConfig.jsonPath).readText()
        originData = Json.parseToJsonElement(configText).jsonObject
    }

    private fun loadTypes() {
        val notIdentifiedTypes = mutableSetOf<String>()
        println("Loading types...")
        originData["classes"]?.jsonObject?.filter { (k, _) ->
            GeneratorConfig.generationExcludeRegexList.find { k.matches(Regex(it)) } == null
        }?.mapValues { entry ->
            json.decodeFromJsonElement<TypeData>(entry.value).also { type ->
                var counter = 0
                type.virtual?.forEach { memberType ->
                    // 对于没有名字的虚函数，将其标记为未知函数，并且将其名字设置为 __unk_vfn_0, __unk_vfn_1, ...
                    if (memberType.name == "" && !memberType.isUnknownFunction())
                        memberType.symbolType = SymbolNodeType.Unknown
                    if (memberType.isUnknownFunction()) {
                        memberType.storageClass = StorageClassType.Virtual
                        memberType.addFlag(MemberTypeData.FLAG_PTR_CALL)
                        memberType.name = "void __unk_vfn_${counter}"
                    }
                    counter++
                }
            }
        }?.forEach { (typeName, type) ->
            TypeManager.addType(
                typeName,
                when {
                    isNameSpace(typeName, type) ->
                        NamespaceType(typeName, type)

                    isStruct(typeName) ->
                        StructType(typeName, type)

                    isClass(typeName) ->
                        ClassType(typeName, type)

                    else -> {
                        notIdentifiedTypes.add(typeName)
                        ClassType(typeName, type)
                    }
                }
            )
        }
        println(
            "Warning: can not determine these types' type. Treat them as class type\n$notIdentifiedTypes"
        )
    }

    private fun loadIdentifiedTypes() {
        println("Loading identifier...")
        val identifier = originData["identifier"]?.jsonObject
        TypeManager.classNameList =
            (identifier?.get("class")?.jsonArray).orEmpty().map { it.jsonPrimitive.content }
        TypeManager.structNameList =
            (identifier?.get("struct")?.jsonArray).orEmpty().map { it.jsonPrimitive.content }
    }
}
