package jadx.tests

import jadx.core.utils.StringUtils
import spock.lang.Specification

class TestStringUtils extends Specification {

    def "unescape string"() {
        expect:
        StringUtils.unescapeString(input) == "\"$expected\""

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
        StringUtils.unescapeChar(input as char) == "'$expected'"

        where:
        input | expected
        'a'   | "a"
        '\n'  | "\\n"
        '\''  | "\\\'"
    }
}
