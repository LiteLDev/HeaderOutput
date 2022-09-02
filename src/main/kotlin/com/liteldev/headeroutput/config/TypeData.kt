package com.liteldev.headeroutput.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
)
