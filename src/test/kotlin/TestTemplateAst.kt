import com.liteldev.headeroutput.ast.template.Parser
import com.liteldev.headeroutput.ast.template.TypeNode
import kotlin.test.Test

class TestTemplateAst {
    private fun parseType(template: String): TypeNode? {
        val result = Parser(template).parse()
        if (result is TypeNode) {
            return result
        }
        return null
    }

    @Test
    fun testBlank() {
        val test = "  "
        val ast = parseType(test)
        assert(ast == null)
    }

    @Test
    fun testFunction() {
        val test =
            "class std::function<class ITreeCanopyWrapper&(struct FeatureLoading::ConcreteFeatureHolder<class VanillaTreeFeature>*)>"
        val ast = parseType(test)
        println(ast)
    }

    @Test
    fun testLiteral() {
        val test = "class std::a<struct std::c<unsigned __int64 const, class d>, 1, bool, 1.2, false>"
        val ast = parseType(test)
        assert(ast != null)
        println(ast)
    }

    @Test
    fun testSpace() {
        val test = " class ABC::DE啊aF < const class  Test < class Testa> , 11,  2.44 ,struct aaiwja **& >"
        val ast = parseType(test)
        println(ast)
    }

    @Test
    fun testParentFunction() {
        val test =
            "struct std::pair<bool ( CommandRegistry::*)(void *, struct CommandRegistry::ParseToken const &, class CommandOrigin const &, int, class std::basic_string<char, struct std::char_traits<char>, class std::allocator<char>> &, class std::vector<class std::basic_string<char, struct std::char_traits<char>, class std::allocator<char>>, class std::allocator<class std::basic_string<char, struct std::char_traits<char>, class std::allocator<char>>>> &) const, class CommandRegistry::Symbol> const"
        val ast = parseType(test)
        println(ast)
    }
}
