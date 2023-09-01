import com.liteldev.headeroutput.HeaderOutput

fun main() {
    HeaderOutput.main(arrayOf(
        "-c", "assets/config.toml",
        "-d", "assets/declareMap.json",
        "-p", "assets/predefine.h",
        "-y"
    ))
}
