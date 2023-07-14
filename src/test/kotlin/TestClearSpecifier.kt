import com.liteldev.headeroutput.removeTypeSpecifier

fun main() {
    removeTypeSpecifier("const Mojang::Sima && const *").let(::println)
    removeTypeSpecifier("class WeakEntityRef const &").let(::println)
}
