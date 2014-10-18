package jadx.tests

import jadx.api.IJadxArgs
import jadx.api.JadxDecompiler
import jadx.core.utils.exceptions.JadxException
import jadx.core.utils.exceptions.JadxRuntimeException
import spock.lang.Specification

class TestAPI extends Specification {

    def "no loaded files"() {
        setup:
        def d = new JadxDecompiler()
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
        new JadxDecompiler().save()
        then:
        def e = thrown(JadxRuntimeException)
        e.message == "No loaded files"
    }

    def "load empty files list"() {
        when:
        new JadxDecompiler().loadFiles(Collections.emptyList())
        then:
        def e = thrown(JadxException)
        e.message == "Empty file list"
    }

    def "load null"() {
        when:
        new JadxDecompiler().loadFile(null)
        then:
        thrown(NullPointerException)
    }

    def "load missing file"() {
        when:
        new JadxDecompiler().loadFile(new File("_.dex"))
        then:
        def e = thrown(JadxException)
        e.message == "Error load file: _.dex"
        e.cause.class == IOException
    }

    def "pass decompiler args"() {
        setup:
        def args = Mock(IJadxArgs)
        when:
        new JadxDecompiler(args)
        then:
        noExceptionThrown()
    }

    def "get errors count for new decompiler"() {
        expect:
        new JadxDecompiler().getErrorsCount() == 0
    }
}
