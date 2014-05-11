package jadx.tests

import jadx.core.dex.attributes.AType
import jadx.core.dex.attributes.AttributeStorage
import jadx.core.dex.attributes.IAttribute
import spock.lang.Specification

import static jadx.core.dex.attributes.AFlag.SYNTHETIC

class TestAttributeStorage extends Specification {

    AttributeStorage storage

    def setup() {
        storage = new AttributeStorage()
    }

    def "add flag"() {
        when:
        storage.add(SYNTHETIC)
        then:
        storage.contains(SYNTHETIC)
    }

    def "remove flag"() {
        setup:
        storage.add(SYNTHETIC)
        when:
        storage.remove(SYNTHETIC)
        then:
        !storage.contains(SYNTHETIC)
    }

    def TEST = new AType<TestAttr>()
    class TestAttr implements IAttribute {
        AType<TestAttr> getType() { TEST }
    }

    def "add attribute"() {
        setup:
        def attr = new TestAttr()
        when:
        storage.add(attr)
        then:
        storage.contains(TEST)
        storage.get(TEST) == attr
    }

    def "remove attribute"() {
        setup:
        def attr = new TestAttr()
        storage.add(attr)
        when:
        storage.remove(attr)
        then:
        !storage.contains(TEST)
        storage.get(TEST) == null
    }

    def "remove attribute other"() {
        setup:
        def attr = new TestAttr()
        storage.add(attr)
        when:
        storage.remove(new TestAttr())
        then:
        storage.contains(TEST)
        storage.get(TEST) == attr
    }

    def "clear"() {
        setup:
        storage.add(SYNTHETIC)
        storage.add(new TestAttr())
        when:
        storage.clear()
        then:
        !storage.contains(SYNTHETIC)
        !storage.contains(TEST)
    }

}
