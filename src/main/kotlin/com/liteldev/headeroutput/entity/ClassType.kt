package com.liteldev.headeroutput.entity

import com.liteldev.headeroutput.HeaderOutput
import com.liteldev.headeroutput.config.MemberTypeData
import com.liteldev.headeroutput.config.TypeData
import com.liteldev.headeroutput.substring
import java.io.File

open class ClassType(
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
        val origin = File(HeaderOutput.OLD_PATH, getPath()).readText().replace("\r\n", "\n")
        beforeExtra = origin.substring(
            "#define BEFORE_EXTRA\n", "\n#undef BEFORE_EXTRA"
        )
        afterExtra = origin.substring(
            "#define AFTER_EXTRA\n", "\n#undef AFTER_EXTRA"
        )
        readComments()
    }

    override fun readComments() {
        readComments("class")
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
            parent = HeaderOutput.classMap[parentNames[0]]?.also {
                it.children[name] = this
            }
        } ?: run { rootClasses[name] = this }
        parent?.let(includeList::add)
    }

    open fun genAntiReconstruction(): String {
        val public = arrayListOf<MemberTypeData>()
        typeData.virtual?.let(public::addAll)
        typeData.publicTypes?.let(public::addAll)
        typeData.protectedTypes?.let(public::addAll)
        typeData.privateTypes?.let(public::addAll)
        public.filter { it.isConstructor() || (it.isOperator() && it.name == "operator=") }
            .let(public::addAll)
        val genOperator = public.find {
            it.isOperator() && it.params?.run {
                size == 1 && this[0].Name == "class $name const &"
            } == true && it.valType.Name == "class $name &"
        } == null
        val genEmptyParamConstructor = public.find { it.name == name && it.params?.isEmpty() ?: true } == null
        val genMoveConstructor = public.find {
            it.name == name && it.params?.run {
                size == 1 && this[0].Name == "class $name const &"
            } == true
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
        sb.appendLine()
        return sb.toString()
    }

    fun genPublic(): String {
        val sb = StringBuilder()
        sb.appendLine("public:")
        var counter = 0
        typeData.virtual?.forEach {
            if (it.namespace.isEmpty() || it.namespace == name)
                sb.appendLine(it.genFuncString(comment = this.getCommentOf(it), vIndex = counter))
            counter++
        }

        if (typeData.virtualUnordered?.isNotEmpty() == true) {
            sb.appendLine("#ifdef ENABLE_VIRTUAL_FAKESYMBOL_${name.uppercase()}")
            typeData.virtualUnordered?.sortedBy { it.name }?.forEach {
                sb.appendLine(
                    it.genFuncString(
                        comment = getCommentOf(it),
                        use_fake_symbol = true
                    )
                )
            }
            sb.appendLine("#endif")
        }

        typeData.publicTypes?.sortedBy { it.name }?.forEach {
            sb.appendLine(it.genFuncString(comment = this.getCommentOf(it)))
        }
        typeData.publicStaticTypes?.sortedBy { it.name }?.forEach {
            sb.appendLine(it.genFuncString(comment = this.getCommentOf(it)))
        }
        if (sb.equals("public:"))
            return ""
        sb.trim()
        sb.appendLine()
        return sb.toString()
    }

    fun genProtected(genFunc: Boolean = true): String {
        if ((typeData.protectedTypes == null || typeData.protectedTypes?.isEmpty() == true)
            && (typeData.protectedStaticTypes == null || typeData.protectedStaticTypes?.isEmpty() == true)
        ) {
            return ""
        }
        val sb = StringBuilder()
        if (genFunc)
            sb.appendLine("//protected:")
        else
            sb.appendLine("protected:")
        typeData.protectedTypes?.sortedBy { it.name }?.forEach {
            if ((genFunc && !it.isStaticGlobalVariable()) || (!genFunc && it.isStaticGlobalVariable()))
                sb.appendLine(it.genFuncString(comment = this.getCommentOf(it)))
        }
        typeData.protectedStaticTypes?.sortedBy { it.name }?.forEach {
            if ((genFunc && !it.isStaticGlobalVariable()) || (!genFunc && it.isStaticGlobalVariable()))
                sb.appendLine(it.genFuncString(comment = this.getCommentOf(it)))
        }
        if (sb.equals("protected:") || sb.equals("//protected:"))
            return ""
        sb.trim()
        sb.appendLine()
        return sb.toString()
    }

    fun genPrivate(genFunc: Boolean = true): String {
        if ((typeData.privateTypes == null || typeData.privateTypes?.isEmpty() == true)
            && (typeData.privateStaticTypes == null || typeData.privateStaticTypes?.isEmpty() == true)
        ) {
            return ""
        }
        val sb = StringBuilder()
        if (genFunc)
            sb.appendLine("//private:")
        else
            sb.appendLine("private:")
        typeData.privateTypes?.sortedBy { it.name }?.forEach {
            if ((genFunc && !it.isStaticGlobalVariable()) || (!genFunc && it.isStaticGlobalVariable()))
                sb.appendLine(it.genFuncString(comment = this.getCommentOf(it)))
        }
        typeData.privateStaticTypes?.sortedBy { it.name }?.forEach {
            if ((genFunc && !it.isStaticGlobalVariable()) || (!genFunc && it.isStaticGlobalVariable()))
                sb.appendLine(it.genFuncString(comment = this.getCommentOf(it)))
        }
        if (sb.equals("private:") || sb.equals("//private:"))
            return ""
        sb.trim()
        sb.appendLine()
        return sb.toString()
    }
}
