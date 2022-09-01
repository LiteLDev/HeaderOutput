import com.liteldev.headeroutput.HeaderOutput
import org.junit.jupiter.api.Test

object TranscriptionTest {
    @Test
    fun `try transcription`() {
        HeaderOutput.main(
            arrayOf(
                "-o", "Z:/header",
                "-j", "originalData.json",
                "-g", "Z:/new",
            )
        )
    }
}

