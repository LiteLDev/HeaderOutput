package data

import com.alibaba.fastjson.annotation.JSONField


data class MemberTypeData(
    @field:JSONField(name = "access_type") val accessType: String?, // public
    @field:JSONField(name = "class") val className: String?, // Mob
    @field:JSONField(name = "flag_bits") var flags: Int, // 1
    @field:JSONField(name = "member_type") var memberType: String?, // virtual
    var method: String, // hasComponent
    val params: List<String>,
    @field:JSONField(name = "return") var returnType: String?, // bool
    val rva: Long, // 13417264
    val symbol: String, // ?hasComponent@Mob@@UEBA_NAEBVHashedString@@@Z
) {


    fun genFuncString(namespace: Boolean = false, useDlsym: Boolean = false): String {
        val ret: String
        if (!useDlsym) {
            if (isStaticGlobalVariable()) {
                ret = "MCAPI ${if (!namespace) "static " else "extern "}$returnType $method;"
            } else {
                if (isOperator() && (method.startsWith("operator ") || method == "operator $returnType"))
                    returnType = ""
                var paramsString = ""
                params.forEach { paramsString = "$paramsString$it, " }
                if (paramsString != "") paramsString = paramsString.substring(0, paramsString.length - 2)
                ret =
                    "${run { if (isVirtual()) "virtual " else "MCAPI " }}${run { if (isPtrCall() || isVirtual() || namespace) "" else "static " }}${
                        run {
                            if (returnType != "") "$returnType "
                            else ""
                        }
                    }${method}($paramsString)${
                        run {
                            if (isConst()) " const" else ""
                        }
                    }${
                        run {
                            if (isPureCall()) " = 0" else ""
                        }
                    };"
            }
        } else {
            var inParams = ""
            var forwardParams = ""
            var defParams = ""
            var counter = 0
            params.forEach {
                Regex("(.*)\\(\\*\\)(.*)").find(it)
                    ?.run { inParams = "$inParams${groupValues[1]}(*a${counter})${groupValues[2]}, " }
                    ?: run { inParams = "$inParams$it a${counter}, " }
                forwardParams = "${forwardParams}std::forward<$it>(a$counter), "
                defParams = "${defParams}$it, "
                counter++
            }
            inParams = inParams.removeSuffix(", ")
            forwardParams = forwardParams.removeSuffix(", ")
            defParams = defParams.removeSuffix(", ")

            ret = """inline $returnType $method($inParams)${run { if (isConst()) " const" else "" }}{
        $returnType ($className::*rv)($defParams)${run { if (isConst()) " const" else "" }};
        *((void**)&rv) = dlsym("$symbol");
        return (this->*rv)($forwardParams);
    }"""
        }
        // if (isVirtual()) ret = ret.replace(Regex("(enum ([a-zA-Z_:][a-zA-Z:_0-9]*))"), "int /*enum \$1*/")
        return ret.replace(
            Regex(
                "class std::basic_string<char, ?struct std::char_traits<char>, ?class std::allocator<char ?> ?>"
            ), "std::string"
        ).replace(
            Regex("class std::(\\w*)<(.*),\\s*(?:class|struct)\\s*std::allocator<\\s*\\2\\s*>\\s*>"),
            "std::\$1<\$2>"
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
        if(flags and flag != flag)
            flags += flag
    }

    fun removeFlag(flag: Int) {
        if(flags and flag == flag)
            flags -= flag
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