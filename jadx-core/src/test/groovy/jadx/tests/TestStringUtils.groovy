package jadx.tests

import jadx.api.JadxArgs
import jadx.core.utils.StringUtils
import spock.lang.Specification

class TestStringUtils extends Specification {

    def "unescape string"() {
        def args = new JadxArgs()
        args.setEscapeUnicode(true)
        def stringUtils = new StringUtils(args)
        expect:
        stringUtils.unescapeString(input) == "\"$expected\""

        where:
        input    | expected
        ""       | ""
        "'"      | "'"
        "a"      | "a"
        "\n"     | "\\n"
        "\t"     | "\\t"
        "\r"     | "\\r"
        "\b"     | "\\b"
        "\f"     | "\\f"
        "\\"     | "\\\\"
        "\""     | "\\\""
        "\u1234" | "\\u1234"
    }

    def "unescape char"() {
        expect:
        new StringUtils(new JadxArgs()).unescapeChar(input as char) == "'$expected'"

        where:
        input | expected
        'a'   | "a"
        ' '   | " "
        '\n'  | "\\n"
        '\''  | "\\\'"
        '\0'  | "\\u0000"
    }
}
