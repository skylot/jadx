package jadx.gui.device.debugger.smali;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

class DbgSmaliTest extends SmaliTest {
	private static final Logger LOG = LoggerFactory.getLogger(DbgSmaliTest.class);

	@BeforeEach
	public void initProject() {
		setCurrentProject("jadx-gui");
	}

	@Test
	void test() {
		disableCompilation();
		ClassNode cls = getClassNodeFromSmali("switch", "SwitchTest");
		Smali disasm = Smali.disassemble(cls);
		LOG.debug("{}", disasm.getCode());
	}
}
