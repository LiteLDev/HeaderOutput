import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals

interface IntEnumKind {
    val value: Int
}

open class IntEnumSerializer<E>(serialName: String, private val clazz: KClass<E>) :
    KSerializer<E> where E : Enum<E>, E : IntEnumKind {

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

@Suppress("unused")
@Serializable(with = Test2Enum.Companion::class)
enum class Test2Enum(override val value: Int) : IntEnumKind {
    A(0), B(1), C(2);

    companion object : IntEnumSerializer<Test2Enum>(Test2Enum::class.simpleName!!, Test2Enum::class)
}

object Test1EnumSerializer : KSerializer<Test1Enum> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Test1Enum", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Test1Enum) {
        encoder.encodeInt(value.value)
    }

    override fun deserialize(decoder: Decoder): Test1Enum {
        return Test1Enum.getByValue(decoder.decodeInt())!!
    }
}

@Serializable(with = Test1EnumSerializer::class)
enum class Test1Enum(val value: Int) {
    Test1Enum1(1), Test1Enum2(2);

    companion object {
        fun getByValue(value: Int) = entries.firstOrNull { it.value == value }
    }
}

@Serializable
data class SomeData(val test1Enum: Test1Enum)

class TestEnum {
    @Test
    fun testEnumSerializer() {
        val json = Json.encodeToString(Test2Enum.B)
        assertEquals("1", json)
        assertEquals(Test2Enum.B, Json.decodeFromString<Test2Enum>(json))
    }
}
