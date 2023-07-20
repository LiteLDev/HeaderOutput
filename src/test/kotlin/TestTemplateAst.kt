import com.liteldev.headeroutput.ast.template.parseType

fun main() {
    val type =
        "class std::a<struct std::c<unsigned __int64 const, class d>, 1, bool, 1.2, false>"
    val ast = parseType(type)
    println(ast)
    println(ast?.genTemplateDeclares())
}
