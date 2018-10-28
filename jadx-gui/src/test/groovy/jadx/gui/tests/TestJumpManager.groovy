package jadx.gui.tests

import jadx.gui.utils.JumpManager
import jadx.gui.utils.JumpPosition
import spock.lang.Specification

class TestJumpManager extends Specification {

    JumpManager jm

    def setup() {
        jm = new JumpManager()
    }

    def "empty history"() {
        expect:
        jm.getPrev() == null
        jm.getNext() == null
    }

    def "empty history 2"() {
        expect:
        jm.getPrev() == null
        jm.getNext() == null
        jm.getPrev() == null
        jm.getNext() == null
        jm.getPrev() == null
    }

    def "1 element"() {
        when:
        jm.addPosition(Mock(JumpPosition))
        then:
        jm.getPrev() == null
        jm.getNext() == null
    }

    def "2 elements"() {
        when:
        def mock1 = Mock(JumpPosition)
        jm.addPosition(mock1)
        def mock2 = Mock(JumpPosition)
        jm.addPosition(mock2)
        // 1 - 2@
        then:
        noExceptionThrown()
        jm.getPrev() == mock1
        jm.getPrev() == null
        jm.getNext() == mock2
        jm.getNext() == null
    }

    def "navigation"() {
        expect:
        def mock1 = Mock(JumpPosition)
        jm.addPosition(mock1)
        // 1@
        def mock2 = Mock(JumpPosition)
        jm.addPosition(mock2)
        // 1 - 2@
        jm.getPrev() == mock1
        // 1@ - 2
        def mock3 = Mock(JumpPosition)
        jm.addPosition(mock3)
        // 1 - 3@
        jm.getNext() == null
        jm.getPrev() == mock1
        // 1@ - 3
        jm.getNext() == mock3
    }

    def "navigation2"() {
        expect:
        def mock1 = Mock(JumpPosition)
        jm.addPosition(mock1)
        // 1@
        def mock2 = Mock(JumpPosition)
        jm.addPosition(mock2)
        // 1 - 2@
        def mock3 = Mock(JumpPosition)
        jm.addPosition(mock3)
        // 1 - 2 - 3@
        def mock4 = Mock(JumpPosition)
        jm.addPosition(mock4)
        // 1 - 2 - 3 - 4@
        jm.getPrev() == mock3
        // 1 - 2 - 3@ - 4
        jm.getPrev() == mock2
        // 1 - 2@ - 3 - 4
        def mock5 = Mock(JumpPosition)
        jm.addPosition(mock5)
        // 1 - 2 - 5@
        jm.getNext() == null
        jm.getNext() == null
        jm.getPrev() == mock2
        // 1 - 2@ - 5
        jm.getPrev() == mock1
        // 1@ - 2 - 5
        jm.getPrev() == null
        jm.getNext() == mock2
        // 1 - 2@ - 5
        jm.getNext() == mock5
        // 1 - 2 - 5@
        jm.getNext() == null
    }

    def "add same element"() {
        when:
        def mock = Mock(JumpPosition)
        jm.addPosition(mock)
        jm.addPosition(mock)
        then:
        noExceptionThrown()
        jm.getPrev() == null
        jm.getNext() == null
    }
}
