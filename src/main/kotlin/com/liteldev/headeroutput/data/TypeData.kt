package com.liteldev.headeroutput.data

import com.liteldev.headeroutput.ast.template.Flattenable
import com.liteldev.headeroutput.ast.template.Parser
import com.liteldev.headeroutput.entity.BaseType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class TypeData(
    @SerialName("child_types") val childTypes: List<String>?,
    @SerialName("parent_types") val parentTypes: List<String>?,
    @SerialName("private") val privateTypes: List<MemberTypeData>?,
    @SerialName("private.static") val privateStaticTypes: List<MemberTypeData>?,
    @SerialName("protected") val protectedTypes: List<MemberTypeData>?,
    @SerialName("protected.static") val protectedStaticTypes: List<MemberTypeData>?,
    @SerialName("public") val publicTypes: List<MemberTypeData>?,
    @SerialName("public.static") val publicStaticTypes: List<MemberTypeData>?,
    val virtual: List<MemberTypeData>?,
    @SerialName("virtual.unordered") val virtualUnordered: MutableList<MemberTypeData>?,
    @SerialName("vtbl_entry") val vtblEntry: List<String>?
) {

    fun collectAllFunction() = listOfNotNull(
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
            (memberType.params?.mapNotNull { it.Name } ?: emptyList()) + listOfNotNull(memberType.valType.Name)
        }.flatMap(::matchTypes).toMap()
    }

    companion object {
        val typeMatchRegex = Regex("(struct|class|enum)\\s+([a-zA-Z0-9_]+(?:::[a-zA-Z0-9_]+)*)")

        fun empty(): TypeData {
            return TypeData(null, null, null, null, null, null, null, null, null, null, null)
        }
    }
}
