package data

import com.alibaba.fastjson.annotation.JSONField

data class TypeData(
    @field:JSONField(name = "child_types") val childTypes: List<String>?,
    @field:JSONField(name = "parent_types") val parentTypes: List<String>?,
    @field:JSONField(name = "private") val privateTypes: List<MemberTypeData>?,
    @field:JSONField(name = "private.static") val privateStaticTypes: List<MemberTypeData>?,
    @field:JSONField(name = "protected") val protectedTypes: List<MemberTypeData>?,
    @field:JSONField(name = "protected.static") val protectedStaticTypes: List<MemberTypeData>?,
    @field:JSONField(name = "public") val publicTypes: List<MemberTypeData>?,
    @field:JSONField(name = "public.static") val publicStaticTypes: List<MemberTypeData>?,
    val virtual: List<MemberTypeData>?,
    @field:JSONField(name = "virtual.unordered") val virtualUnordered: List<MemberTypeData>?,
    @field:JSONField(name = "vtbl_entry") val vtblEntry: List<String>?
)
