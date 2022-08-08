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

object TestEnumSerializer : KSerializer<TestEnum> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("TestEnum", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: TestEnum) {
        encoder.encodeInt(value.value)
    }

    override fun deserialize(decoder: Decoder): TestEnum {
        return TestEnum.getByValue(decoder.decodeInt())!!
    }
}

@Serializable(with = TestEnumSerializer::class)
enum class TestEnum(val value: Int) {
    TestEnum1(1), TestEnum2(2);

    companion object {
        fun getByValue(value: Int) = values().firstOrNull { it.value == value }
    }
}

@Serializable
data class SomeData(val TestEnum: TestEnum)

fun main() {/*val someJson = """
        {"TestEnum":2}
    """.trimIndent()
    println(Json.decodeFromString<SomeData>(someJson))
    println(Json.encodeToString(SomeData(TestEnum.TestEnum1)))*/
    val json = Json.encodeToString(Test2Enum.B)
    println(Json.encodeToString(Test2Enum.B))
    println(Json.decodeFromString<Test2Enum>(json))
}