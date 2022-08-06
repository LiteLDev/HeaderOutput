package com.liteldev.headeroutput

import com.liteldev.headeroutput.config.GeneratorConfig
import com.liteldev.headeroutput.config.MemberTypeData
import com.liteldev.headeroutput.config.TypeData
import com.liteldev.headeroutput.entity.ClassType
import com.liteldev.headeroutput.entity.NamespaceType
import com.liteldev.headeroutput.entity.StructType
import com.liteldev.headeroutput.generate.ClassGenerator
import com.liteldev.headeroutput.generate.NamespaceGenerator
import com.liteldev.headeroutput.generate.StructGenerator
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import java.io.File

object HeaderOutput {

    private const val JSON_PATH = "Z:\\originalData.json"
    const val OLD_PATH = "Z:\\MC"
    const val GENERATE_PATH = "Z:\\headers"

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
                it.copyTo(File(GENERATE_PATH, it.name), true)
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
        val configText = File("config.json").readText()
        generatorConfig = Json.decodeFromString(configText)
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
            Json.decodeFromJsonElement<TypeData>(entry.value).also { type ->
                var counter = 0
                type.virtual?.forEach { memberType ->
                    run {
                        if (memberType.method == "" && !memberType.isUnknownFunction()) memberType.addFlag(
                            MemberTypeData.UNKNOWN_FUNCTION
                        )
                        if (memberType.isUnknownFunction()) {
                            memberType.memberType = "virtual"
                            memberType.addFlag(MemberTypeData.PTR_CALL)
                            memberType.method = "void __unk_vfn_${counter}"
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