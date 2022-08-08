package com.liteldev.headeroutput.entity

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.reflect.KClass

@Suppress("unused")
@Serializable(with = SymbolNodeType.Companion::class)
enum class SymbolNodeType(override val value: Int) : IntEnumBase {
    Normal(0),
    Constructor(1),
    Destructor(2),
    Operator(3),
    StaticVar(4),
    Unknown(5);

    companion object : IntEnumSerializer<SymbolNodeType>(SymbolNodeType::class.simpleName!!, SymbolNodeType::class)
}

@Suppress("unused")
@Serializable(with = AccessType.Companion::class)
enum class AccessType(override val value: Int) : IntEnumBase {
    Public(0),
    Protected(1),
    Private(2),
    None(3);

    companion object : IntEnumSerializer<AccessType>(AccessType::class.simpleName!!, AccessType::class)
}

/** StorageClassType 表示一个Symbol的类属性
 * 以下为其可能的取值
 *
 * static:  静态成员，全局实例，无实例绑定
 *
 * virtual: 虚拟对象，由虚表访问
 *
 * none:    标准成员，全局实例，绑定到实例的访问
 * */
@Suppress("unused")
@Serializable(with = StorageClassType.Companion::class)
enum class StorageClassType(override val value: Int) : IntEnumBase {
    Static(0),
    Virtual(1),
    None(2);

    companion object : IntEnumSerializer<StorageClassType>(StorageClassType::class.simpleName!!, StorageClassType::class)
}

@Suppress("unused")
@Serializable(with = VarSymbolType.Companion::class)
enum class VarSymbolType(override val value: Int) : IntEnumBase {
    Unknown(0),
    Md5Symbol(1),
    PrimitiveType(2),
    FunctionSignature(3),
    Identifier(4),
    NamedIdentifier(5),
    VcallThunkIdentifier(6),
    LocalStaticGuardIdentifier(7),
    IntrinsicFunctionIdentifier(8),
    ConversionOperatorIdentifier(9),
    DynamicStructorIdentifier(10),
    StructorIdentifier(11),
    LiteralOperatorIdentifier(12),
    ThunkSignature(13),
    PointerType(14),
    TagType(15),
    ArrayType(16),
    Custom(17),
    IntrinsicType(18),
    NodeArray(19),
    QualifiedName(20),
    TemplateParameterReference(21),
    EncodedStringLiteral(22),
    IntegerLiteral(23),
    RttiBaseClassDescriptor(24),
    LocalStaticGuardVariable(25),
    FunctionSymbol(26),
    VariableSymbol(27),
    SpecialTableSymbol(28);

    companion object : IntEnumSerializer<VarSymbolType>(VarSymbolType::class.simpleName!!, VarSymbolType::class)
}

interface IntEnumBase {
    val value: Int
}

open class IntEnumSerializer<E>(serialName: String, private val clazz: KClass<E>) :
    KSerializer<E> where E : Enum<E>, E : IntEnumBase {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(serialName, PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: E) {
        encoder.encodeInt(value.value)
    }

    override fun deserialize(decoder: Decoder): E {
        val decodeInt = decoder.decodeInt()
        return clazz.java.enumConstants.firstOrNull { it.value == decodeInt }
            ?: throw IllegalArgumentException("No enum constant found for value $decodeInt")
    }
}