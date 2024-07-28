package com.liteldev.headeroutput.data

import com.liteldev.headeroutput.TypeManager
import com.liteldev.headeroutput.appendSpace
import com.liteldev.headeroutput.config.GeneratorConfig
import com.liteldev.headeroutput.entity.BaseType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Suppress("unused")
@Serializable
data class MemberTypeData(
    @SerialName("storage_class") var storageClass: StorageClassType,
    @SerialName("access_type") val accessType: AccessType, // public
    @SerialName("symbol_type") var symbolType: SymbolNodeType,
    @SerialName("type") val valType: VariableTypeData,
    @SerialName("namespace") val namespace: String, // Mob
    @SerialName("name") var name: String, // hasComponent
    @SerialName("params") val params: List<VariableTypeData> = emptyList(),
    @SerialName("params_name") val paramsName: List<String> = emptyList(),
    @SerialName("flag_bits") var flags: Int, // 1
    @SerialName("rva") val rva: Long, // 13417264
    @SerialName("symbol") var symbol: String, // ?hasComponent@Mob@@UEBA_NAEBVHashedString@@@Z
    @SerialName("fake_symbol") val fakeSymbol: String?, // ?hasComponent@Mob@@UEBA_NAEBVHashedString@@@Z
) {
    val type: BaseType by lazy {
        TypeManager.getType(namespace)!!
    }

    fun genFuncString(
        namespace: Boolean = false,
        useFakeSymbol: Boolean = false,
        vIndex: Int? = null
    ): String {
        fun StringBuilder.appendIndented(str: String) = appendSpace(START_BLANK_SPACE).append(str).append("\n")
        var origin = buildString {
            val isFunctionPtr = valType.name.contains("*)(") && valType.name.endsWith(")")
            val infos = mutableListOf<String>()
            if (vIndex != null) appendIndented("// vIndex: $vIndex")
            appendSpace(START_BLANK_SPACE)
            if (isStaticGlobalVariable()) {
                append("MCAPI ")
                if (namespace) append("extern ") else append("static ")
                if (valType.name.isBlank()) valType.name = "auto"
                else valType.name = valType.name.replaceEnumType()
                append("${valType.name} $name;")
            } else {
                if (isOperator() && (name.startsWith("operator ") || name == "operator ${valType.name}"))
                    valType.name = ""
                else if (valType.name.isBlank() && !isConstructor() && !isDestructor()) valType.name = "auto"
                else valType.name = valType.name.replaceEnumType()

                val paramNameBeginIndex = if (isPtrCall()) 1 else 0
                val paramsString =
                    if (paramsName.isNotEmpty() && params.size + paramNameBeginIndex == paramsName.size) {
                        var i = paramNameBeginIndex
                        params.joinToString(", ") {
                            if (it.name.contains("*)(") && (it.name.endsWith(")") || it.name.endsWith(") const")))
                                it.name.replace("*)(", "*${paramsName[i++]})(")
                            else if (it.name.contains("&)(") && it.name.endsWith(")") || it.name.endsWith(") const"))
                                it.name.replace("&)(", "&${paramsName[i++]})(")
                            else "${it.name} ${paramsName[i++]}"
                        }
                    } else {
                        params.joinToString(", ") { it.name }
                    }.replaceEnumType()

                // Linkage specifiers
                if (isVirtual())
                    if (useFakeSymbol) append("MCVAPI ")
                    else append("virtual ")
                else append("MCAPI ")

                if (!(isPtrCall() || isVirtual() || namespace)) append("static ")
                if ((isConstructor() && !isCopyConstructor() && !isMoveConstructor() && params.size == 1) || isConversionOperator())
                    append("explicit ")
                if (isFunctionPtr) append("auto ")
                else if (valType.name != "") append("${valType.name} ")
                append("$name(${paramsString})")

                if (isConst()) append(" const")
                if (isFunctionPtr) append(" -> ${valType.name}")
                if (isPureCall())
                    append(" = 0")
                else if (isVirtual() && !useFakeSymbol && symbol.startsWith("__gen_"))
                    append(" = default")
                append(";")
            }
        }
        GeneratorConfig.replacementRegex.forEach { origin = origin.replace(it.first, it.second) }
        return origin
    }

    fun isConstructor() = symbolType == SymbolNodeType.Constructor

    fun isDestructor() = symbolType == SymbolNodeType.Destructor

    fun isOperator() = symbolType == SymbolNodeType.Operator

    fun isUnknownFunction() = symbolType == SymbolNodeType.Unknown

    fun isStaticGlobalVariable() = symbolType == SymbolNodeType.StaticVar

    fun isConst() = hasFlag(FLAG_CONST)

    fun isPtrCall() = hasFlag(FLAG_PTR_CALL)

    fun isPureCall() = hasFlag(FLAG_PURE_CALL)

    fun isConversionOperator() = isOperator() && name.startsWith("operator ")

    fun isCopyOperator() = isOperator() && params.run {
        size == 1 && (this[0].name == "${type.type} ${type.name} const &")
    }

    fun isDefaultConstructor() = isConstructor() && params.isEmpty()

    fun isCopyConstructor() = isConstructor() && params.run {
        size == 1 && (this[0].name == "${type.type} ${type.name} const &")
    }

    fun isMoveConstructor() = isConstructor() && params.run {
        size == 1 && (this[0].name == "${type.type} ${type.name} &&")
    }

    fun addFlag(flag: Int) {
        if (flags and flag != flag) flags += flag
    }

    fun removeFlag(flag: Int) {
        if (flags and flag == flag) flags -= flag
    }

    fun hasFlag(flag: Int) = flags and flag == flag

    fun isVirtual() = storageClass == StorageClassType.Virtual

    fun compare(other: MemberTypeData): Int {
        if (this.name != other.name) return this.name.compareTo(other.name)
        if (this.valType.name != other.valType.name) return this.valType.name.compareTo(other.valType.name)
        if (this.params.size != other.params.size) return this.params.size - other.params.size
        this.params.forEachIndexed { index, variableTypeData ->
            if (variableTypeData.name != other.params[index].name) return variableTypeData.name.compareTo(other.params[index].name)
        }
        return 0
    }

    companion object {
        //[0] const
        //[1] __ptr64 spec
        //[2] isPureCall
        const val FLAG_CONST = 1 shl 0
        const val FLAG_PTR_CALL = 1 shl 1
        const val FLAG_PURE_CALL = 1 shl 2

        const val START_BLANK_SPACE = 4

        private val typeMatchRegex = Regex("enum\\s+([a-zA-Z0-9_]+(?:::[a-zA-Z0-9_]+)*)")
        fun String.replaceEnumType(): String {
            var result = this
            typeMatchRegex.findAll(this).forEach { matchResult ->
                val name = matchResult.groupValues[1]
                result = if (TypeManager.hasType(name)) result.replace("enum $name", "::$name")
                else result.replace("enum $name", "enum class $name")
            }
            return result
        }
    }
}
