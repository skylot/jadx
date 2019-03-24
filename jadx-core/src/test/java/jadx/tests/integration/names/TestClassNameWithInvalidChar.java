package jadx.tests.integration.names;

import org.junit.jupiter.api.Test;

import jadx.api.JadxDecompiler;
import jadx.api.JadxInternalAccess;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.RootNode;
import jadx.tests.api.SmaliTest;

public class TestClassNameWithInvalidChar extends SmaliTest {
	/*
		public class do- {}
		public class i-f {}
	*/

	@Test
	public void test() {
		JadxDecompiler d = loadSmaliFiles("names", "TestClassNameWithInvalidChar");
		RootNode root = JadxInternalAccess.getRoot(d);
		for (ClassNode cls : root.getClasses(false)) {
			decompileAndCheckCls(d, cls);
		}
	}

	@Test
	public void testWithDeobfuscation() {
		enableDeobfuscation();

		JadxDecompiler d = loadSmaliFiles("names", "TestClassNameWithInvalidChar");
		RootNode root = JadxInternalAccess.getRoot(d);
		for (ClassNode cls : root.getClasses(false)) {
			decompileAndCheckCls(d, cls);
		}
	}
}
