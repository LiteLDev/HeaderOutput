import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import data.*
import generate.ClassGenerator
import generate.NamespaceGenerator
import generate.StructGenerator
import java.io.File

const val JSON_PATH = "Z:\\originalData.json"

const val OLD_PATH = "Z:\\MC"
const val GENERATE_PATH = "Z:\\headers"

val RULE: Rule = JSONObject.parseObject(File("rule.json").readText(), Rule::class.java)

val originData: JSONObject = JSONObject.parseObject(File(JSON_PATH).readText())

var funcListOfTypes = (originData["classes"] as JSONObject).filter { (k, _) ->
    RULE.exclusion.generation.regex.find { k.matches(Regex(it)) } == null
}.filterValues { it != null }.mapValues { entry ->
    JSONObject.parseObject(
        JSON.toJSONString(entry.value), TypeData::class.java
    ).also { type ->
        var counter = 0
        type.virtual?.forEach { memberType ->
            run {
                if (memberType.method == "" && !memberType.isUnknownFunction()) memberType.addFlag(MemberTypeData.UNKNOWN_FUNCTION)
                if (memberType.isUnknownFunction()) {
                    memberType.memberType = "virtual"
                    memberType.addFlag(MemberTypeData.PTR_CALL)
                    memberType.method = "void __unk_vfn_${counter}"
                }
                counter++
            }
        }
    }
}

val identifier = originData["identifier"] as JSONObject
val realClassNameList: List<String> = (identifier["class"] as JSONArray).toJavaList(String::class.java)
val realStructNameList: List<String> = (identifier["struct"] as JSONArray).toJavaList(String::class.java)

val classMap = mutableMapOf<String, ClassType>()
val structMap = mutableMapOf<String, StructType>()
val namespaceMap = mutableMapOf<String, NamespaceType>()

val notExistBaseType = mutableSetOf<String>()

fun main() {

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

fun isNameSpace(typeName: String, typeData: TypeData): Boolean {
    if (   typeData.privateTypes != null
        || typeData.privateStaticTypes != null
        || typeData.protectedTypes != null
        || typeData.protectedStaticTypes != null
        || typeData.publicStaticTypes != null
        || typeData.virtual != null
        || typeData.vtblEntry != null) {
        return false
    }
    val containsInIdentifierList = realStructNameList.contains(typeName) || realClassNameList.contains(typeName)
    val isNamespace = typeData.publicTypes?.find { it.isPtrCall() } == null
            && !containsInIdentifierList
    return isNamespace
}