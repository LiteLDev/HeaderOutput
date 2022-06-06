package data

import OLD_PATH
import classMap
import substring
import java.io.File

class ClassType(
    name: String, typeData: TypeData,
    var parent: ClassType? = null,
    private val children: MutableMap<String, ClassType> = mutableMapOf(),
) : BaseType(name, typeData) {
    override fun getPath(): String {
        return "./$name.hpp"
        /*if (parent == null) {
            "./$name"
        } else {
            parent!!.getPath() + "/" + name
        }*/
    }

    override fun readOldAddition() {
//        val origin = File(OLD_PATH, "./${name}API.hpp").readText().replace("\r\n", "\n")
//        beforeAddition = origin.substring("#ifdef EXTRA_INCLUDE_PART_${name.uppercase()}\n", "\n#else")
//        afterAddition = origin.substring("#else\n", "\n#endif")
//        return
        val origin = File(OLD_PATH, getPath()).readText().replace("\r\n", "\n")
        beforeAddition = origin.substring(
            "#define BEFORE_EXTRA\n", "\n#undef BEFORE_EXTRA"
        )
        afterAddition = origin.substring(
            "#define AFTER_EXTRA\n", "\n#undef AFTER_EXTRA"
        )
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ClassType

        if (name != other.name) return false
        if (typeData != other.typeData) return false

        return true
    }

    fun constructLinkedClassMap(rootClasses: MutableMap<String, ClassType>) {
        typeData.parentTypes?.also { parentNames ->
            parent = classMap[parentNames[0]]?.also {
                it.children[name] = this
            }
        } ?: run { rootClasses[name] = this }
        parent?.let(includeList::add)
    }

    fun genAntiReconstruction(): String {
        val public = arrayListOf<MemberTypeData>()
        typeData.virtual?.let(public::addAll)
        typeData.publicTypes?.let(public::addAll)
        typeData.protectedTypes?.let(public::addAll)
        typeData.privateTypes?.let(public::addAll)
        public.filter { it.isNew() || (it.isOperator() && it.method == "operator=") }
            .let(public::addAll)
        val genOperator = public.find {
            it.isOperator() && it.params.run {
                size == 1 && this[0] == "class $name const &"
            } && it.returnType == "class $name &"
        } == null
        val genEmptyParamConstructor = public.find { it.method == name && it.params.isEmpty() } == null
        val genMoveConstructor = public.find {
            it.method == name && it.params.run {
                size == 1 && this[0] == "class $name const &"
            }
        } == null
        val sb = StringBuilder()
        if (genOperator || genEmptyParamConstructor || genMoveConstructor) {
            sb.appendLine()
            sb.appendLine("#ifndef DISABLE_CONSTRUCTOR_PREVENTION_${name.uppercase()}")
            sb.appendLine("public:")
            if (genOperator) {
                sb.appendLine("    class $name& operator=(class $name const &) = delete;")
            }
            if (genMoveConstructor) {
                sb.appendLine("    $name(class $name const &) = delete;")
            }
            if (genEmptyParamConstructor) {
                sb.appendLine("    $name() = delete;")
            }
            sb.appendLine("#endif")
        }
        return sb.toString()
    }

    fun genPublic(): String {
        val sb = StringBuilder()
        sb.appendLine("public:")
        var counter = 0
        typeData.virtual?.forEach {
            if ((it.className ?: "").isEmpty() || it.className == name) sb.append("    /*${counter}*/ ")
                .appendLine(it.genFuncString())
            counter++
        }

        sb.appendLine("#ifdef ENABLE_VIRTUAL_FAKESYMBOL_${name.uppercase()}")
        //sb.appendLine("public:")
        typeData.virtualUnordered?.sortedBy { it.method }?.forEach {
            sb.append("    ").appendLine(it.genFuncString(useDlsym = true))
        }
        sb.appendLine("#endif")


        typeData.publicTypes?.sortedBy { it.method }?.forEach {
            sb.append("    ").appendLine(it.genFuncString())
        }
        typeData.publicStaticTypes?.sortedBy { it.method }?.forEach {
            sb.append("    ").appendLine(it.genFuncString())
        }
        return sb.toString()
    }

    fun genProtected(): String {
        val sb = StringBuilder()
        sb.appendLine("protected:")
        typeData.protectedTypes?.sortedBy { it.method }?.forEach {
            sb.append("    ").appendLine(it.genFuncString())
        }
        typeData.protectedStaticTypes?.sortedBy { it.method }?.forEach {
            sb.append("    ").appendLine(it.genFuncString())
        }
        return sb.toString()
    }

    fun genPrivate(): String {
        val sb = StringBuilder()
        sb.appendLine("private:")
        typeData.privateTypes?.sortedBy { it.method }?.forEach {
            sb.append("    ").appendLine(it.genFuncString())
        }
        typeData.privateStaticTypes?.sortedBy { it.method }?.forEach {
            sb.append("    ").appendLine(it.genFuncString())
        }
        return sb.toString()
    }
}
