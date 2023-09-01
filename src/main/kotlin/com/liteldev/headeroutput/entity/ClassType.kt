package com.liteldev.headeroutput.entity

import com.liteldev.headeroutput.*
import com.liteldev.headeroutput.ast.template.NodeType
import com.liteldev.headeroutput.data.MemberTypeData
import com.liteldev.headeroutput.data.TypeData

open class ClassType(
    name: String, typeData: TypeData, val isTemplateClass: Boolean = false, isStructType: Boolean = false,
) : BaseType(name, TypeKind.CLASS, typeData) {

    private val publicFunctions = arrayListOf<MemberTypeData>()
    private val publicVariables = arrayListOf<MemberTypeData>()
    private val protectedFunctions = arrayListOf<MemberTypeData>()
    private val protectedVariables = arrayListOf<MemberTypeData>()
    private val privateFunctions = arrayListOf<MemberTypeData>()
    private val privateVariables = arrayListOf<MemberTypeData>()

    val parents = arrayListOf<BaseType>()

    // fixme: Fix in header generator
    init {
        if (isStructType) {
            this.type = TypeKind.STRUCT
        }
        val ordered = typeData.virtual.map { it.symbol }.toMutableList()
        typeData.virtualUnordered.removeIf { unordered ->
            unordered.symbol in ordered
        }
        fun determineType(
            members: List<MemberTypeData>,
            funList: MutableList<MemberTypeData>,
            varList: MutableList<MemberTypeData>
        ) {
            members.forEach { member ->
                if (member.isStaticGlobalVariable()) {
                    varList.add(member)
                } else {
                    funList.add(member)
                }
            }
        }
        determineType(typeData.publicTypes, publicFunctions, publicVariables)
        determineType(typeData.publicStaticTypes, publicFunctions, publicVariables)
        determineType(typeData.protectedTypes, protectedFunctions, protectedVariables)
        determineType(typeData.protectedStaticTypes, protectedFunctions, protectedVariables)
        determineType(typeData.privateTypes, privateFunctions, privateVariables)
        determineType(typeData.privateStaticTypes, privateFunctions, privateVariables)
    }

    private fun generateInnerTypeDefine(): String {
        if (innerTypes.isEmpty()) return ""
        val generateOrder = innerTypes.toMutableList()
        generateOrder.sortWith(Comparator { o1, o2 ->
            if (o1 is EnumType && o2 is EnumType) {
                return@Comparator o1.name.compareTo(o2.name)
            }
            if (o1 is EnumType || o2 is EnumType) {
                return@Comparator if (o1 is EnumType) -1 else 1
            }
            if (o1 in o2.allReferences || o1.allInnerTypes.any { it in o2.allReferences }) {
                return@Comparator -1
            }
            if (o2 in o1.allReferences || o2.allInnerTypes.any { it in o1.allReferences }) {
                return@Comparator 1
            }

            return@Comparator o1.name.compareTo(o2.name)
        })
        return buildString {
            val inners = generateOrder.filterIsInstance<ClassType>()
            if (inners.isNotEmpty()) {
                appendLine("// $simpleName inner types declare")
                appendLine("// clang-format off")
                appendLine(inners.joinToString(separator = "\n") { it.generateTypeDeclare() })
                appendLine("// clang-format on\n")
            }
            appendLine("// $simpleName inner types define")
            append(generateOrder.joinToString(separator = "\n") { it.generateTypeDefine() })
        }
    }

    override fun generateTypeDefine(): String = buildString {
        val classType = if (type == TypeKind.STRUCT) "struct" else "class"
        getTemplateDefine()?.let(this::appendLine)
        appendLine("$classType $simpleName ${genParents()}{")
        if (innerTypes.isNotEmpty()) {
            appendLine("public:")
            val def = generateInnerTypeDefine()
            if (def.isNotBlank()) {
                append("    ")
                append(def.replace("\n", "\n    "))
            }
            appendLine()
        }
        append(generateAntiReconstruction())
        append(generatePublic())
        append(generateProtected())
        append(generatePrivate())
        append(generateProtected(genFunc = false))
        append(generatePrivate(genFunc = false))
        append(generateMemberAccessor())
        appendLine("};")
    }

    /**
     * generate type declare
     * when type's outer type is null or class, just return the type's declaration because nested type declare forward is not allowed in c++
     * when type's outer type is namespace, return namespace declaration + type's declaration
     */
    override fun generateTypeDeclare(): String {
        var baseDeclaration = when (type) {
            TypeKind.CLASS -> "class $simpleName;"
            TypeKind.STRUCT -> "struct $simpleName;"
            else -> error("not support type $type")
        }
        getTemplateDefine()?.let { baseDeclaration = "$it $baseDeclaration" }
        if (outerType == null || outerType is ClassType) {
            return baseDeclaration
        }
        if (outerType?.type == TypeKind.NAMESPACE) {
            return "namespace ${outerType?.name} { $baseDeclaration }"
        }
        error("unreachable")
    }

    override fun initIncludeAndForwardDeclareList() {
        // not include self, inner type, and types can forward declare
        val declareRequiredTypes = allReferences.filter {
            val notInSameFile = it.getTopLevelFileType() != this.getTopLevelFileType()
            val canNotForwardDeclareSimply = outerType?.isNamespace() == true
                    || it.name.contains("::")
                    || ((it as? ClassType)?.isTemplateClass == true)
                    || it.isEnum()
            notInSameFile && canNotForwardDeclareSimply
        }
        declareRequiredTypes.forEach {
            if (it.isEnum() || it.outerType is ClassType || (it as? ClassType)?.isTemplateClass == true) {
                this.path.relativePathTo(it.path).let(includeList::add)
            } else {
                it.generateTypeDeclare().let(forwardDeclareList::add)
            }
        }
        val parents = collectParents()
        if (parents.isNotEmpty()) {
            includeList.addAll(parents.map { this.path.relativePathTo(it.path) })
        }
        includeList.remove(this.path.relativePathTo(this.path))
        includeList.remove("")
    }

    private fun genParents(): String {
        if (parents.isEmpty()) {
            return ""
        }
        val sb = StringBuilder(": ")
        parents.joinToString { "public ::${it.name}" }.let(sb::append)
        sb.append(" ")
        return sb.toString()
    }

    private fun generateAntiReconstruction(): String {
        val classType = if (this.type == TypeKind.STRUCT) "struct" else "class"
        val public = typeData.collectInstanceFunction()
            .filter { it.isConstructor() || (it.isOperator() && it.name == "operator=") }
        val genOperator = public.none {
            it.isOperator() && it.params.let { params ->
                params.size == 1 && params[0].name == "$classType $name const &"
            } // && it.valType.Name == "$classType $name &" Removed because of overload
        }
        val genEmptyParamConstructor = public.none { it.name == simpleName && it.params.isEmpty() }
        val genCopyConstructor = public.none {
            it.name == simpleName && it.params.let { params ->
                params.size == 1 && params[0].name == "$classType $name const &"
            }
        }
        return if (!genOperator && !genEmptyParamConstructor && !genCopyConstructor) {
            ""
        } else StringBuilder(
            "public:\n    // prevent constructor by default\n"
        ).apply {
            if (genOperator) {
                appendLine("    $simpleName& operator=($simpleName const &) = delete;")
            }
            if (genCopyConstructor) {
                appendLine("    $simpleName($simpleName const &) = delete;")
            }
            if (genEmptyParamConstructor) {
                appendLine("    $simpleName() = delete;")
            }
            appendLine()
        }.toString()
    }

    private fun generatePublic() = buildString {
        if (typeData.publicTypes.isEmpty() && typeData.publicStaticTypes.isEmpty() && typeData.virtual.isEmpty() && typeData.virtualUnordered.isEmpty()
        ) {
            return ""
        }
        appendLine("public:")
        appendLine("    // NOLINTBEGIN")
        var counter = 0
        val virtual = typeData.virtual.map {
            counter++ to it
        }.filter { (_, it) -> it.namespace.isEmpty() || it.namespace == name }
        if (virtual.isNotEmpty())
            virtual.joinToString("\n\n", postfix = "\n\n") { (i, it) ->
                it.genFuncString(vIndex = i)
            }.let(::append)
        if (typeData.virtualUnordered.isNotEmpty()) {
            typeData.virtualUnordered.sortedBy { it.name }.joinToString("\n\n", postfix = "\n\n") {
                it.genFuncString(useFakeSymbol = true)
            }.let(::append)
        }
        publicFunctions.let(::generateMembers).let(::append)
        publicVariables.let(::generateMembers).let(::append)
        appendLine("    // NOLINTEND")
        trim()
        appendLine()
    }

    /**
     * @param genFunc if true, generate function, otherwise generate static global variable
     */
    private fun generateProtected(genFunc: Boolean = true) = buildString {
        if (genFunc) {
            if (protectedFunctions.isEmpty()) {
                return ""
            }
            appendLine("    // protected:")
            appendLine("    // NOLINTBEGIN")
            protectedFunctions.let(::generateMembers).let(::append)
            appendLine("    // NOLINTEND")
        } else {
            if (protectedVariables.isEmpty()) {
                return ""
            }
            appendLine("protected:")
            appendLine("    // NOLINTBEGIN")
            protectedVariables.let(::generateMembers).let(::append)
            appendLine("    // NOLINTEND")
        }
        trim()
        appendLine()
    }

    /**
     * @param genFunc if true, generate function, otherwise generate static global variable
     */
    private fun generatePrivate(genFunc: Boolean = true) = buildString {
        if (genFunc) {
            if (privateFunctions.isEmpty()) {
                return ""
            }
            appendLine("    // private:")
            appendLine("    // NOLINTBEGIN")
            privateFunctions.let(::generateMembers).let(::append)
            appendLine("    // NOLINTEND")
        } else {
            if (privateVariables.isEmpty()) {
                return ""
            }
            appendLine("private:")
            appendLine("    // NOLINTBEGIN")
            privateVariables.let(::generateMembers).let(::append)
            appendLine("    // NOLINTEND")
        }
        trim()
        appendLine()
    }

    private fun generateMembers(members: List<MemberTypeData>) = buildString {
        if (members.isEmpty()) return ""
        fun isStatic(member: MemberTypeData) = !(member.isPtrCall() || member.isVirtual())
        members.sortedWith { o1, o2 ->
            if (isStatic(o1) == isStatic(o2)) {
                o1.name.compareTo(o2.name)
            } else {
                if (isStatic(o1)) 1 else -1
            }
        }.joinToString("\n\n", postfix = "\n\n") {
            it.genFuncString()
        }.let(::append)
    }

    private fun generateMemberAccessor() = buildString {
        val members = privateVariables + protectedVariables
        if (members.isEmpty()) return ""
        appendLine("// member accessor")
        appendLine("public:")
        appendLine("    // NOLINTBEGIN")
        privateVariables.forEach {
            val pureName = it.name.removeSuffix("[]")
            appendLine("    inline auto& $$pureName() { return $pureName; }\n")
        }
        appendLine("    // NOLINTEND")
        trim()
        appendLine()
    }

    private fun getTemplateDefine(): String? {
        val params = TypeManager.template[name] ?: return null
        check(params.isNotEmpty()) { "$name: template must have at least one parameter." }
        return buildString {
            append("template<")
            if (params[0] == NodeType.VARIABLE) {
                append("typename... T0>")
                return@buildString
            }
            check(params.last() != NodeType.VARIABLE || params.size > 1) {
                "$name: variable template must be the last template parameter, but not the only one."
            }
            val isVariable = params.last() == NodeType.VARIABLE
            params.forEachIndexed { index, nodeType ->
                when (nodeType) {
                    NodeType.TYPE -> append("typename")
                    NodeType.INTEGER -> append("int")
                    NodeType.FLOAT -> append("float")
                    NodeType.BOOLEAN -> append("bool")
                    NodeType.VARIABLE -> return@forEachIndexed
                }
                if (isVariable && index == params.size - 2) {
                    append("...")
                }
                append(" T$index")
                if (index != params.size - 1 && params.getOrNull(index + 1) != NodeType.VARIABLE) {
                    append(", ")
                }
            }
            append(">")
        }
    }

    fun collectParents(): List<BaseType> {
        val parents = arrayListOf<BaseType>()
        parents.addAll(this.parents)
        innerTypes.forEach { innerType ->
            if (innerType is ClassType) parents.addAll(innerType.collectParents())
        }
        return parents
    }
}
