package com.liteldev.headeroutput.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MemberTypeData(
    @SerialName("access_type") val accessType: String?, // public
    @SerialName("class") val className: String?, // Mob
    @SerialName("flag_bits") var flags: Int, // 1
    @SerialName("member_type") var memberType: String?, // virtual
    @SerialName("method") var method: String, // hasComponent
    @SerialName("params") val params: List<String>,
    @SerialName("return") var returnType: String?, // bool
    @SerialName("rva") val rva: Long, // 13417264
    @SerialName("symbol") val symbol: String, // ?hasComponent@Mob@@UEBA_NAEBVHashedString@@@Z
    @SerialName("fake_symbol") val fakeSymbol: String?, // ?hasComponent@Mob@@UEBA_NAEBVHashedString@@@Z
) {


    fun genFuncString(namespace: Boolean = false, useDlsym: Boolean = false): String {
        var ret = StringBuilder()
        if (isStaticGlobalVariable()) {
            ret = StringBuilder("MCAPI ${if (!namespace) "static " else "extern "}$returnType $method;")
        } else {
            if (isOperator() && (method.startsWith("operator ") || method == "operator $returnType"))
                returnType = ""
            var paramsString = ""
            params.forEach { paramsString = "$paramsString$it, " }
            if (paramsString != "") paramsString = paramsString.substring(0, paramsString.length - 2)
            if (useDlsym) {
                ret.append(run { if (isVirtual()) "MCVAPI " else "MCAPI " })
                if (!(isPtrCall() || isVirtual() || namespace)) ret.append("static ")
                if (returnType != "") ret.append("$returnType ")
                ret.append("${method}($paramsString)")
                if (isConst()) ret.append(" const")
            } else {
                ret.append(run { if (isVirtual()) "virtual " else "MCAPI " })
                if (!(isPtrCall() || isVirtual() || namespace)) ret.append("static ")
                if (returnType != "") ret.append("$returnType ")
                ret.append("$method($paramsString)")
                if (isConst()) ret.append(" const")
                if (isPureCall()) ret.append(" = 0")
                ret.append(";")
            }
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

    fun isNone() = flags and NONE == NONE

    fun isConst() = flags and CONST == CONST

    fun isNew() = flags and NEW == NEW

    fun isDelete() = flags and DELETE == DELETE

    fun isOperator() = flags and OPERATOR == OPERATOR

    fun isUnknownFunction() = flags and UNKNOWN_FUNCTION == UNKNOWN_FUNCTION

    fun isStaticGlobalVariable() = flags and STATIC_GLOBAL_VARIABLE == STATIC_GLOBAL_VARIABLE

    fun isPtrCall() = flags and PTR_CALL == PTR_CALL

    fun isPureCall() = flags and PURE_CALL == PURE_CALL

    fun addFlag(flag: Int) {
        if (flags and flag != flag) flags += flag
    }

    fun removeFlag(flag: Int) {
        if (flags and flag == flag) flags -= flag
    }

    fun isVirtual() = memberType == "virtual"

    companion object {

        //Flag
        //[0] none
        //[1] const
        //[2] new
        //[3] delete(v)
        //[4] operate;
        //[5] unknown func
        //[6] static global var
        //[7] HasPtr Call
        //[8] isPureCall

        const val NONE = 1 shl 0
        const val CONST = 1 shl 1
        const val NEW = 1 shl 2
        const val DELETE = 1 shl 3
        const val OPERATOR = 1 shl 4
        const val UNKNOWN_FUNCTION = 1 shl 5
        const val STATIC_GLOBAL_VARIABLE = 1 shl 6
        const val PTR_CALL = 1 shl 7
        const val PURE_CALL = 1 shl 8
    }
}