package jadx.tests

import jadx.api.Decompiler
import jadx.api.IJadxArgs
import jadx.core.dex.nodes.MethodNode
import jadx.core.utils.ErrorsCounter
import jadx.core.utils.exceptions.JadxException
import jadx.core.utils.exceptions.JadxRuntimeException
import spock.lang.Specification

class TestAPI extends Specification {

    def "no loaded files"() {
        setup:
        def d = new Decompiler()
        when:
        def classes = d.getClasses()
        def packages = d.getPackages()
        then:
        notThrown(NullPointerException)
        classes?.isEmpty()
        packages?.isEmpty()
    }

    def "save with no loaded files"() {
        when:
        new Decompiler().save()
        then:
        def e = thrown(JadxRuntimeException)
        e.message == "No loaded files"
    }

    def "load empty files list"() {
        when:
        new Decompiler().loadFiles(Collections.emptyList())
        then:
        def e = thrown(JadxException)
        e.message == "Empty file list"
    }

    def "load null"() {
        when:
        new Decompiler().loadFile(null)
        then:
        thrown(NullPointerException)
    }

    def "load missing file"() {
        when:
        new Decompiler().loadFile(new File("_.dex"))
        then:
        def e = thrown(JadxException)
        e.message == "Error load file: _.dex"
        e.cause.class == IOException
    }

    def "pass decompiler args"() {
        setup:
        def args = Mock(IJadxArgs)
        when:
        new Decompiler(args)
        then:
        noExceptionThrown()
    }

    def "get errors count for new decompiler"() {
        expect:
        new Decompiler().getErrorsCount() == 0
    }

    def "get errors count after one more init"() {
        setup:
        new Decompiler()
        def mth = Mock(MethodNode)
        when:
        ErrorsCounter.methodError(mth, "")
        def d = new Decompiler()
        then:
        d.getErrorsCount() == 0
    }

    def "decompiler toString()"() {
        expect:
        new Decompiler().toString() == "jadx decompiler"
    }
}
