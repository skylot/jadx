package jadx.gui.device.debugger.smali;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static org.assertj.core.api.Assertions.assertThat;

class DbgSmaliTest extends SmaliTest {
	private static final Logger LOG = LoggerFactory.getLogger(DbgSmaliTest.class);

	@BeforeEach
	public void initProject() {
		setCurrentProject("jadx-gui");
	}

	@Test
	void testSwitch() {
		disableCompilation();
		ClassNode cls = getClassNodeFromSmali("switch", "SwitchTest");
		Smali disasm = Smali.disassemble(cls);
		LOG.debug("{}", disasm.getCode());
	}

	@Test
	void testParams() {
		disableCompilation();
		ClassNode cls = getClassNodeFromSmali("params", "ParamsTest");
		Smali disasm = Smali.disassemble(cls);
		String code = disasm.getCode();
		LOG.debug("{}", code);
		assertThat(code)
				.doesNotContain("Failed to write method")
				.doesNotContain(".param p1")
				.contains(".local p1, \"arg0\":Landroid/widget/AdapterView;, \"Landroid/widget/AdapterView<*>;\"");
	}
}
