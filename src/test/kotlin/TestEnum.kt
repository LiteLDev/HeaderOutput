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
    TestEnum1(1),
    TestEnum2(2);

    companion object {
        fun getByValue(value: Int) = values().firstOrNull { it.value == value }
    }
}

@Serializable
data class SomeData(val TestEnum: TestEnum)

fun main() {
    val someJson = """
        {"TestEnum":2}
    """.trimIndent()
    println(Json.decodeFromString<SomeData>(someJson))
    println(Json.encodeToString(SomeData(TestEnum.TestEnum1)))
}