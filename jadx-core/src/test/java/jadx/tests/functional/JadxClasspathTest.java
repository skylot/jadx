package jadx.tests.functional;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jadx.api.JadxArgs;
import jadx.core.clsp.ClspGraph;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.RootNode;

import static jadx.core.dex.instructions.args.ArgType.STRING;
import static jadx.core.dex.instructions.args.ArgType.object;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JadxClasspathTest {

	private static final String JAVA_LANG_EXCEPTION = "java.lang.Exception";
	private static final String JAVA_LANG_THROWABLE = "java.lang.Throwable";

	private RootNode root;
	private ClspGraph clsp;

	@BeforeEach
	public void initClsp() {
		this.root = new RootNode(new JadxArgs());
		this.root.loadClasses(Collections.emptyList());
		this.root.initClassPath();
		this.clsp = root.getClsp();
	}

	@Test
	public void test() {
		ArgType objExc = object(JAVA_LANG_EXCEPTION);
		ArgType objThr = object(JAVA_LANG_THROWABLE);

		assertTrue(clsp.isImplements(JAVA_LANG_EXCEPTION, JAVA_LANG_THROWABLE));
		assertFalse(clsp.isImplements(JAVA_LANG_THROWABLE, JAVA_LANG_EXCEPTION));

		assertFalse(ArgType.isCastNeeded(root, objExc, objThr));
		assertTrue(ArgType.isCastNeeded(root, objThr, objExc));

		assertTrue(ArgType.isCastNeeded(root, ArgType.OBJECT, STRING));
	}
}
