package jadx.gui.tests
import jadx.gui.utils.search.StringRef
import spock.lang.Specification

import static jadx.gui.utils.search.StringRef.fromStr
import static jadx.gui.utils.search.StringRef.subString

class TestStringRef extends Specification {

    def "test substring"() {
        expect:
        s1.toString() == expected
        s1 == fromStr(expected)
        where:
        s1                     | expected
        fromStr("a")           | "a"
        subString("a", 0)      | "a"
        subString("a", 1)      | ""
        subString("a", 0, 0)   | ""
        subString("a", 0, 1)   | "a"
        subString("abc", 1, 2) | "b"
        subString("abc", 2)    | "c"
        subString("abc", 2, 3) | "c"
    }

    def "compare with original substring"() {
        expect:
        s == expected
        where:
        s                   | expected
        "a".substring(0)    | "a"
        "a".substring(1)    | ""
        "a".substring(0, 0) | ""
        "a".substring(0, 1) | "a"
    }

    def "test trim"() {
        expect:
        s.trim().toString() == expected
        where:
        s                         | expected
        fromStr("a")              | "a"
        fromStr(" a ")            | "a"
        fromStr("\ta")            | "a"
        subString("a b c", 1)     | "b c"
        subString("a b\tc", 1, 4) | "b"
        subString("a b\tc", 2, 3) | "b"
    }

    def "test split"() {
        expect:
        StringRef.split(s, d) == (expected as String[]).collect { fromStr(it) }
        if (!Arrays.equals(s.split(d), (expected).toArray(new String[0]))) {
            throw new IllegalArgumentException("Don't match with original split: "
                    + " s='" + s + "' d='" + d
                    + "', expected:" + expected + ", got: " + Arrays.toString(s.split(d)));
        }
        where:
        s       | d      | expected
        "abc"   | "b"    | ["a", "c"]
        "abc"   | "a"    | ["", "bc"]
        "abc"   | "c"    | ["ab"]
        "abc"   | "d"    | ["abc"]
        "abbbc" | "b"    | ["a", "", "", "c"]
        "abbbc" | "bb"   | ["a", "bc"]
        "abbbc" | "bbb"  | ["a", "c"]
        "abbbc" | "bbc"  | ["ab"]
        "abbbc" | "bbbc" | ["a"]
    }
}
