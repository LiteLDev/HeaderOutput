package com.liteldev.headeroutput

import com.liteldev.headeroutput.config.GeneratorConfig
import com.liteldev.headeroutput.config.MemberTypeData
import com.liteldev.headeroutput.config.TypeData
import com.liteldev.headeroutput.entity.*
import com.liteldev.headeroutput.generate.ClassGenerator
import com.liteldev.headeroutput.generate.NamespaceGenerator
import com.liteldev.headeroutput.generate.StructGenerator
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Options
import org.apache.commons.cli.ParseException
import java.io.File


@OptIn(ExperimentalSerializationApi::class)
private val json = Json { explicitNulls = false }

object HeaderOutput {

    private lateinit var JSON_PATH: String
    lateinit var OLD_PATH: String
    lateinit var GENERATE_PATH: String
    lateinit var CONFIG_PATH: String

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
        try {
            val options = Options()
            options.addOption("c", "config", true, "The config file path(default ./config.json)")
            options.addOption("o", "old", true, "The old header files path(default ./old)")
            options.addOption("g", "generate", true, "The generate header files path(default ./header)")
            options.addOption("j", "json", true, "The original data json file path(default ./header.json)")
            val cmd = DefaultParser().parse(options, args)
            CONFIG_PATH = if (cmd.hasOption("c")) cmd.getOptionValue("c") else "./config.json"
            OLD_PATH = if (cmd.hasOption("o")) cmd.getOptionValue("o") else "./old"
            GENERATE_PATH = if (cmd.hasOption("g")) cmd.getOptionValue("g") else "./header"
            JSON_PATH = if (cmd.hasOption("j")) cmd.getOptionValue("j") else "./originalData.json"
            if (!File(CONFIG_PATH).isFile) {
                println("Invalid config file path")
                return
            }
            if (!File(OLD_PATH).isDirectory) {
                println("Invalid old header files path")
                return
            }
            if (!File(GENERATE_PATH).isDirectory) {
                println("Invalid header generate path")
                return
            }
            if (!File(JSON_PATH).isFile) {
                println("Invalid original data json file path")
                return
            }
        } catch (e: ParseException) {
            println(e.localizedMessage)
        }
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