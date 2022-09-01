package com.liteldev.headeroutput

import com.liteldev.headeroutput.config.GeneratorConfig
import com.liteldev.headeroutput.config.MemberTypeData
import com.liteldev.headeroutput.config.TypeData
import com.liteldev.headeroutput.entity.*
import com.liteldev.headeroutput.generate.ClassGenerator
import com.liteldev.headeroutput.generate.NamespaceGenerator
import com.liteldev.headeroutput.generate.StructGenerator
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import java.io.File


@OptIn(ExperimentalSerializationApi::class)
private val json = Json { explicitNulls = false }

object HeaderOutput {

    private lateinit var JSON_PATH: String
    lateinit var OLD_PATH: String
    lateinit var GENERATE_PATH: String
    private lateinit var CONFIG_PATH: String

    private lateinit var originData: JsonObject
    private lateinit var funcListOfTypes: Map<String, TypeData>
    lateinit var generatorConfig: GeneratorConfig
    lateinit var realClassNameList: List<String>
    lateinit var realStructNameList: List<String>

    val classMap = mutableMapOf<String, ClassType>()
    val structMap = mutableMapOf<String, StructType>()
    val namespaceMap = mutableMapOf<String, NamespaceType>()
    val notExistBaseType = mutableSetOf<String>()

    @JvmStatic
    fun main(args: Array<String>) {
        if (!readCommandLineArgs(args)) return

        loadConfig()
        loadOriginData()
        loadIdentifier()
        loadFuncListOfTypes()

        funcListOfTypes.forEach { (typeName, type) ->
            when {
                isNameSpace(typeName, type) -> {
                    namespaceMap[typeName] = NamespaceType(typeName, type).also {
                        runCatching {
                            it.readOldAddition()
                        }.onFailure {
                            println("Warning: $typeName not found in old")
                        }
                    }
                }

                realStructNameList.contains(typeName) -> {
                    structMap[typeName] = StructType(typeName, type).also {
                        runCatching {
                            it.readOldAddition()
                        }.onFailure {
                            println("Warning: $typeName not found in old")
                        }
                    }
                }

                else/*realClassNameList.contains(typeName)*/ -> {
                    classMap[typeName] = ClassType(typeName, type).also {
                        runCatching {
                            it.readOldAddition()
                        }.onFailure {
                            println("Warning: $typeName not found in old")
                        }
                    }
                }
            }
        }

        //link every class
        val rootClasses = mutableMapOf<String, ClassType>()
        classMap.values.forEach { classType ->
            classType.initIncludeList()
            classType.constructLinkedClassMap(rootClasses)
        }

        structMap.values.forEach { structType ->
            structType.initIncludeList()
        }

        namespaceMap.values.forEach { namespaceType ->
            namespaceType.initIncludeList()
        }

        println("Warning: these class has no information in originData but used by other classes\n$notExistBaseType")
        //println(namespaceMap.keys)

        File(GENERATE_PATH).mkdirs()

        ClassGenerator.generate()
        StructGenerator.generate()
        NamespaceGenerator.generate()

        File(OLD_PATH).listFiles()?.forEach {
            val origin = it.readText()
            if (!origin.contains("#define AUTO_GENERATED")) {
                val dest = File(GENERATE_PATH, it.name)
                if (dest.isFile)
                    println("Warning: ${dest.name} is already exist")
                it.copyTo(dest, true)
            }
        }

        val oldFileNames = (File(OLD_PATH).listFiles()?.map { it.name } ?: arrayListOf()).toSet()
        val newFileNames = (File(GENERATE_PATH).listFiles()?.map { it.name } ?: arrayListOf()).toSet()

        println("Deleted:\t" + oldFileNames.subtract(newFileNames))
        println("Modified:\t" + oldFileNames.intersect(newFileNames))
        println("Addition:\t" + newFileNames.subtract(oldFileNames))
    }

    private fun readCommandLineArgs(args: Array<String>): Boolean {
        val parser = ArgParser("HeaderOutput")
        val configPath by parser.option(ArgType.String, "config", "c", "The config file path").default("./config.json")
        val oldPath by parser.option(ArgType.String, "old", "o", "The old header path").default("./old")
        val generatePath by parser.option(ArgType.String, "generate", "g", "The generate header files path")
            .default("./header")
        val jsonPath by parser.option(ArgType.String, "json", "j", "The original data json file path")
            .default("./header.json")
        parser.parse(args)
        CONFIG_PATH = configPath
        OLD_PATH = oldPath
        GENERATE_PATH = generatePath
        JSON_PATH = jsonPath
        if (!File(CONFIG_PATH).isFile) {
            println("Invalid config file path")
            return false
        }
        if (!File(OLD_PATH).isDirectory) {
            println("Invalid old header files path")
            return false
        }
        if (!File(GENERATE_PATH).isDirectory) {
            try {
                File(GENERATE_PATH).mkdirs()
            } catch (e: Exception) {
                println("Fail to create generate header files path")
                return false
            }
        }
        if (!File(JSON_PATH).isFile) {
            println("Invalid original data json file path")
            return false
        }
        return true
    }

    private fun loadConfig() {
        println("Loading config...")
        val configText = File(this.CONFIG_PATH).readText()
        generatorConfig = json.decodeFromString(configText)
    }

    private fun loadOriginData() {
        println("Loading origin data...")
        val configText = File(JSON_PATH).readText()
        originData = Json.parseToJsonElement(configText).jsonObject
    }

    private fun loadFuncListOfTypes() {
        println("Loading func list of types...")
        funcListOfTypes = originData["classes"]?.jsonObject?.filter { (k, _) ->
            generatorConfig.exclusion.generation.regex.find { k.matches(Regex(it)) } == null
        }?.mapValues { entry ->
            json.decodeFromJsonElement<TypeData>(entry.value).also { type ->
                var counter = 0
                type.virtual?.forEach { memberType ->
                    run {
                        if (memberType.name == "" && !memberType.isUnknownFunction())
                            memberType.symbolType = SymbolNodeType.Unknown
                        if (memberType.isUnknownFunction()) {
                            memberType.storageClass = StorageClassType.Virtual
                            memberType.addFlag(MemberTypeData.PTR_CALL)
                            memberType.name = "void __unk_vfn_${counter}"
                        }
                        counter++
                    }
                }
            }
        } ?: mapOf()
    }

    private fun loadIdentifier() {
        println("Loading identifier...")
        val identifier = originData["identifier"]?.jsonObject
        realClassNameList =
            (identifier?.get("class")?.jsonArray).orEmpty().map { it.jsonPrimitive.content }
        realStructNameList =
            (identifier?.get("struct")?.jsonArray).orEmpty().map { it.jsonPrimitive.content }
    }
}