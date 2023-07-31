package com.liteldev.headeroutput.data

import com.liteldev.headeroutput.appendSpace
import com.liteldev.headeroutput.config.GeneratorConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MemberTypeData(
    @SerialName("storage_class") var storageClass: StorageClassType,
    @SerialName("access_type") val accessType: AccessType, // public
    @SerialName("symbol_type") var symbolType: SymbolNodeType,
    @SerialName("type") val valType: VariableTypeData,
    @SerialName("namespace") val namespace: String, // Mob
    @SerialName("name") var name: String, // hasComponent
    @SerialName("params") val params: List<VariableTypeData>?,
    @SerialName("flag_bits") var flags: Int, // 1

    @SerialName("rva") val rva: Long, // 13417264
    @SerialName("symbol") val symbol: String, // ?hasComponent@Mob@@UEBA_NAEBVHashedString@@@Z
    @SerialName("fake_symbol") val fakeSymbol: String?, // ?hasComponent@Mob@@UEBA_NAEBVHashedString@@@Z
) {

    fun genFuncString(
        namespace: Boolean = false,
        useFakeSymbol: Boolean = false,
        vIndex: Int = -1
    ): String {
        val symbol =
            if (this.isUnknownFunction())
                "__unk_vfn_${vIndex}"
            else if (this.isVirtual() && this.isDestructor())
                "__unk_destructor_${vIndex}"
            else this.symbol

        val sb = StringBuilder()
        sb.appendSpace(START_BLANK_SPACE).append("/**\n")
        if (isVirtual() && !useFakeSymbol) sb.appendSpace(START_BLANK_SPACE + 1).append("* @vftbl $vIndex\n")
        if (symbol.isNotEmpty()) {
            sb.appendSpace(START_BLANK_SPACE + 1).append("* @symbol ${symbol.replace("@", "\\@")}\n")
        }
        sb.appendSpace(START_BLANK_SPACE + 1).append("*/\n")

        sb.appendSpace(START_BLANK_SPACE)
        if (isStaticGlobalVariable()) {
            sb.append(
                "MCAPI ${if (!namespace) "static " else "extern "}${
                    valType.Name?.replace(
                        "enum ",
                        "enum class "
                    )
                } $name;"
            )
        } else {
            if (isOperator() && (name.startsWith("operator ") || name == "operator ${valType.Name}"))
                valType.Name = ""
            var paramsString = ""
            params?.forEach { paramsString = "$paramsString${it.Name}, " }
            if (paramsString != "") paramsString = paramsString.substring(0, paramsString.length - 2)

            sb.append(run {
                if (isVirtual())
                    if (useFakeSymbol) "MCVAPI "
                    else "virtual "
                else
                    "MCAPI "
            })
            if (!(isPtrCall() || isVirtual() || namespace)) sb.append("static ")
            if (valType.Name != "") sb.append("${valType.Name?.replace("enum ", "enum class ")} ")
            sb.append("$name(${paramsString.replace("enum ", "enum class ")})")
            if (isConst()) sb.append(" const")
            if (isPureCall()) sb.append(" = 0")
            sb.append(";")

        }
        var sbString = sb.toString()
        GeneratorConfig.replacementRegex.forEach { sbString = sbString.replace(it.first, it.second) }
        return sbString
    }

    fun isConstructor() = symbolType == SymbolNodeType.Constructor

    fun isDestructor() = symbolType == SymbolNodeType.Destructor

    fun isOperator() = symbolType == SymbolNodeType.Operator

    fun isUnknownFunction() = symbolType == SymbolNodeType.Unknown

    fun isStaticGlobalVariable() = symbolType == SymbolNodeType.StaticVar

    fun isConst() = hasFlag(FLAG_CONST)
    fun isPtrCall() = hasFlag(FLAG_PTR_CALL)

    fun isPureCall() = hasFlag(FLAG_PURE_CALL)

    fun addFlag(flag: Int) {
        if (flags and flag != flag) flags += flag
    }

    fun removeFlag(flag: Int) {
        if (flags and flag == flag) flags -= flag
    }

    fun hasFlag(flag: Int) = flags and flag == flag

    fun isVirtual() = storageClass == StorageClassType.Virtual

    companion object {
        //[0] const
        //[1] __ptr64 spec
        //[2] isPureCall
        const val FLAG_CONST = 1 shl 0
        const val FLAG_PTR_CALL = 1 shl 1
        const val FLAG_PURE_CALL = 1 shl 2

        const val START_BLANK_SPACE = 4
    }
}
