package jadx.tests

import jadx.core.dex.instructions.args.ArgType
import jadx.core.dex.nodes.parser.SignatureParser
import spock.lang.Specification

import static jadx.core.dex.instructions.args.ArgType.*

class TestSignatureParser extends Specification {
    def "simple types"() {
        expect:
        new SignatureParser(str).consumeType() == result

        where:
        str                   | result
        ""                    | null
        "I"                   | INT
        "[I"                  | array(INT)
        "Ljava/lang/Object;"  | OBJECT
        "[Ljava/lang/Object;" | array(OBJECT)
        "[[I"                 | array(array(INT))
    }

    def "generics"() {
        expect:
        new SignatureParser(str).consumeType() == result

        where:
        str                     | result
        "TD;"                   | genericType("D")
        "La<TV;Lb;>;"           | generic("La;", genericType("V"), object("b"))
        "La<Lb<Lc;>;>;"         | generic("La;", generic("Lb;", object("Lc;")))
        "La/b/C<Ld/E<Lf/G;>;>;" | generic("La/b/C;", generic("Ld/E;", object("Lf/G;")))
        "La<TD;>.c;"            | genericInner(generic("La;", genericType("D")), "c", null)
        "La<TD;>.c/d;"          | genericInner(generic("La;", genericType("D")), "c.d", null)
        "La<Lb;>.c<TV;>;"       | genericInner(generic("La;", object("Lb;")), "c", genericType("V"))
    }

    def "inner generic"() {
        expect:
        new SignatureParser(str).consumeType().getObject() == result

        where:
        str                                           | result
        "La<TV;>.LinkedHashIterator<Lb\$c<Ls;TV;>;>;" | "a\$LinkedHashIterator"
    }

    def "wildcards"() {
        expect:
        new SignatureParser("La<$s>;").consumeType() == generic("La;", r as ArgType[])

        where:
        s       | r
        "*"     | wildcard()
        "+Lb;"  | wildcard(object("b"), 1)
        "-Lb;"  | wildcard(object("b"), -1)
        "+TV;"  | wildcard(genericType("V"), 1)
        "-TV;"  | wildcard(genericType("V"), -1)

        "**"    | [wildcard(), wildcard()]
        "*Lb;"  | [wildcard(), object("b")]
        "*TV;"  | [wildcard(), genericType("V")]
        "TV;*"  | [genericType("V"), wildcard()]
        "Lb;*"  | [object("b"), wildcard()]

        "***"   | [wildcard(), wildcard(), wildcard()]
        "*Lb;*" | [wildcard(), object("b"), wildcard()]
    }

    def "generic map"() {
        expect:
        new SignatureParser(str).consumeGenericMap() == result.collectEntries { [genericType(it.key), it.value] }

        where:
        str                                                  | result
        ""                                                   | [:]
        "<T:Ljava/lang/Object;>"                             | ["T": []]
        "<K:Ljava/lang/Object;LongType:Ljava/lang/Object;>"  | ["K": [], "LongType": []]
        "<ResultT:Ljava/lang/Exception;:Ljava/lang/Object;>" | ["ResultT": [object("java.lang.Exception")]]
    }

    def "method args"() {
        when:
        def argTypes = new SignatureParser("(Ljava/util/List<*>;)V").consumeMethodArgs()
        then:
        argTypes.size() == 1
        argTypes.get(0) == generic("Ljava/util/List;", wildcard())
    }

    def "method args 2"() {
        when:
        def argTypes = new SignatureParser("(La/b/C<TT;>.d/E;)V").consumeMethodArgs()
        then:
        argTypes.size() == 1
        def argType = argTypes.get(0)
        argType.getObject().indexOf('/') == -1
        argTypes.get(0) == genericInner(generic("La/b/C;", genericType("T")), "d.E", null)
    }

    def "generic map: bad signature"() {
        when:
        def map = new SignatureParser("<A:Ljava/lang/Object;B").consumeGenericMap()
        then:
        notThrown(NullPointerException)
        map.isEmpty()
    }
}
