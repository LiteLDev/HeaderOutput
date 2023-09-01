package com.liteldev.headeroutput.data

import com.liteldev.headeroutput.ast.template.Flattenable
import com.liteldev.headeroutput.ast.template.Parser
import com.liteldev.headeroutput.entity.BaseType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class TypeData(
    @SerialName("child_types") val childTypes: List<String> = emptyList(),
    @SerialName("parent_types") val parentTypes: List<String> = emptyList(),
    @SerialName("private") val privateTypes: List<MemberTypeData> = emptyList(),
    @SerialName("private.static") val privateStaticTypes: List<MemberTypeData> = emptyList(),
    @SerialName("protected") val protectedTypes: List<MemberTypeData> = emptyList(),
    @SerialName("protected.static") val protectedStaticTypes: List<MemberTypeData> = emptyList(),
    @SerialName("public") val publicTypes: List<MemberTypeData> = emptyList(),
    @SerialName("public.static") val publicStaticTypes: List<MemberTypeData> = emptyList(),
    val virtual: List<MemberTypeData> = emptyList(),
    @SerialName("virtual.unordered") val virtualUnordered: MutableList<MemberTypeData> = mutableListOf(),
    @SerialName("vtbl_entry") val virtualTableEntry: List<String> = emptyList()
) {

    private fun collectAllFunction() = listOf(
        privateTypes,
        privateStaticTypes,
        protectedTypes,
        protectedStaticTypes,
        publicTypes,
        publicStaticTypes,
        virtual,
        virtualUnordered,
    ).flatten()

    fun collectInstanceFunction() = listOfNotNull(
        privateTypes,
        protectedTypes,
        publicTypes,
        virtual,
        virtualUnordered,
    ).flatten()

    private fun matchTypes(name: String) = typeMatchRegex.findAll(name)
        .map { it.groupValues[2] to BaseType.TypeKind.valueOf(it.groupValues[1].uppercase(Locale.getDefault())) }
        .onEach { (typeName, _) ->
            if (name.contains("$typeName<")) {
                runCatching { (Parser(name).parse() as Flattenable?)?.flatten() }
            }
        } // detects template class


    fun collectReferencedTypes(): Map<String, BaseType.TypeKind> {
        return collectAllFunction().flatMap { memberType ->
            memberType.params.map { it.name } + listOfNotNull(memberType.valType.name)
        }.flatMap(::matchTypes).toMap()
    }

    companion object {
        val typeMatchRegex = Regex("(struct|class|enum)\\s+([a-zA-Z0-9_]+(?:::[a-zA-Z0-9_]+)*)")

        fun empty(): TypeData {
            return TypeData()
        }
    }
}
