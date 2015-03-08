package jadx.tests

import spock.lang.Specification

import static jadx.core.deobf.NameMapper.isValidFullIdentifier

class TestNameMapper extends Specification {

    def "test is Valid Full Identifier"() {
        expect:
        isValidFullIdentifier(valid)
        where:
        valid << [
                'C',
                'Cc',
                'b.C',
                'b.Cc',
                'aAa.b.Cc',
                'a.b.Cc',
                'a.b.C_c',
                'a.b.C$c',
                'a.b.C9'
        ]
    }

    def "test is not Valid Full Identifier"() {
        expect:
        !isValidFullIdentifier(invalid)
        where:
        invalid << [
                '',
                '5',
                '7A',
                '.C',
                'b.9C',
                'b..C',
        ]
    }
}
