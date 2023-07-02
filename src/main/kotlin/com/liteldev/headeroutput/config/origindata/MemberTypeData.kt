package com.liteldev.headeroutput.config.origindata

import com.liteldev.headeroutput.appendSpace
import com.liteldev.headeroutput.entity.AccessType
import com.liteldev.headeroutput.entity.StorageClassType
import com.liteldev.headeroutput.entity.SymbolNodeType
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
        comment: String = "",
        vIndex: Int = -1
    ): String {
        val symbol =
            if (this.isUnknownFunction())
                "__unk_vfn_${vIndex}"
            else if (this.isVirtual() && this.isDestructor())
                "__unk_destructor_${vIndex}"
            else this.symbol

        val ret = StringBuilder()
        ret.appendSpace(START_BLANK_SPACE).append("/**\n")
        if (isVirtual() && !useFakeSymbol) ret.appendSpace(START_BLANK_SPACE + 1).append("* @vftbl $vIndex\n")
        if (symbol.isNotEmpty()) {
            ret.appendSpace(START_BLANK_SPACE + 1).append("* @symbol ${symbol.replace("@", "\\@")}\n")
        }
        if (comment.isNotEmpty()) ret.appendSpace(START_BLANK_SPACE + 1).append("*\n").append(comment)
        ret.appendSpace(START_BLANK_SPACE + 1).append("*/\n")

        ret.appendSpace(START_BLANK_SPACE)
        if (isStaticGlobalVariable()) {
            ret.append(
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

            ret.append(run {
                if (isVirtual())
                    if (useFakeSymbol) "MCVAPI "
                    else "virtual "
                else
                    "MCAPI "
            })
            if (!(isPtrCall() || isVirtual() || namespace)) ret.append("static ")
            if (valType.Name != "") ret.append("${valType.Name?.replace("enum ", "enum class ")} ")
            ret.append("$name(${paramsString.replace("enum ", "enum class ")})")
            if (isConst()) ret.append(" const")
            if (isPureCall()) ret.append(" = 0")
            ret.append(";")

        }
        // if (isVirtual()) ret = ret.replace(Regex("(enum ([a-zA-Z_:][a-zA-Z:_0-9]*))"), "int /*enum \$1*/")
        return ret.replace(
            Regex(
                "class std::basic_string<char, ?struct std::char_traits<char>, ?class std::allocator<char ?> ?>"
            ), "std::string"
        ).replace(
            Regex("class std::(\\w*)<(.*),\\s*(?:class|struct)\\s*std::allocator<\\s*\\2\\s*>\\s*>"), "std::\$1<\$2>"
        ).replace(
            Regex("class std::(\\w*)<(.*),\\s*(?:class|struct)\\s*std::default_delete<\\s*\\2\\s*>\\s*>"),
            "std::\$1<\$2>"
        )
    }

    fun isConst() = flags and CONST == CONST

    fun isConstructor() = symbolType == SymbolNodeType.Constructor

    fun isDestructor() = symbolType == SymbolNodeType.Destructor

    fun isOperator() = symbolType == SymbolNodeType.Operator

    fun isUnknownFunction() = symbolType == SymbolNodeType.Unknown

    fun isStaticGlobalVariable() = symbolType == SymbolNodeType.StaticVar

    fun isPtrCall() = flags and PTR_CALL == PTR_CALL

    fun isPureCall() = flags and PURE_CALL == PURE_CALL

    fun addFlag(flag: Int) {
        if (flags and flag != flag) flags += flag
    }

    fun removeFlag(flag: Int) {
        if (flags and flag == flag) flags -= flag
    }

    fun isVirtual() = storageClass == StorageClassType.Virtual

    companion object {
        //[0] const
        //[1] __ptr64 spec
        //[2] isPureCall
        const val CONST = 1 shl 0
        const val PTR_CALL = 1 shl 1
        const val PURE_CALL = 1 shl 2

        const val START_BLANK_SPACE = 4
    }


}