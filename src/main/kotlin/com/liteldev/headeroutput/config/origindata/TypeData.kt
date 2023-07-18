package com.liteldev.headeroutput.config.origindata

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

    fun collectReferencedTypes(): Map<String, BaseType.TypeKind> {
        val typeRegex = Regex("(struct|class|enum)\\s+([a-zA-Z0-9_]+(?:::[a-zA-Z0-9_]+)*)")
        return collectAllFunction().flatMap { memberType ->
            (memberType.params?.mapNotNull { it.Name } ?: emptyList()) + listOfNotNull(memberType.valType.Name)
        }.mapNotNull { name ->
            typeRegex.find(name)
                ?.let {
                    it.groupValues[2] to BaseType.TypeKind.valueOf(it.groupValues[1].uppercase(Locale.getDefault()))
                }
        }.toMap()
    }

    companion object {
        fun empty(): TypeData {
            return TypeData(null, null, null, null, null, null, null, null, null, null, null)
        }
    }
}
