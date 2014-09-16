package jadx.gui.tests

import jadx.gui.update.VersionComparator
import spock.lang.Specification

class TestVersionsComparator extends Specification {

    def "test"() {
        expect:
        VersionComparator.compare(s1, s2) == expected
        VersionComparator.compare(s2, s1) == -expected

        where:
        s1      | s2        | expected
        ""      | ""        | 0
        "1"     | "1"       | 0
        "1"     | "2"       | -1
        "1.1"   | "1.1"     | 0
        "0.5"   | "0.5"     | 0
        "0.5"   | "0.5.0"   | 0
        "0.5"   | "0.5.00"  | 0
        "0.5"   | "0.5.0.0" | 0
        "0.5"   | "0.5.0.1" | -1
        "0.5.0" | "0.5.0"   | 0
        "0.5.0" | "0.5.1"   | -1
        "0.5"   | "0.5.1"   | -1
        "0.4.8" | "0.5"     | -1
        "0.4.8" | "0.5.0"   | -1
        "0.4.8" | "0.6"     | -1
    }
}
