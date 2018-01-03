package jadx.tests.functional;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import jadx.core.clsp.ClspGraph;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.DexNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.exceptions.DecodeException;

import static jadx.core.dex.instructions.args.ArgType.STRING;
import static jadx.core.dex.instructions.args.ArgType.object;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JadxClasspathTest {

	private static final String JAVA_LANG_EXCEPTION = "java.lang.Exception";
	private static final String JAVA_LANG_THROWABLE = "java.lang.Throwable";

	private DexNode dex;
	private ClspGraph clsp;

	@Before
	public void initClsp() throws IOException, DecodeException {
		clsp = new ClspGraph();
		clsp.load();
		dex = mock(DexNode.class);
		RootNode rootNode = mock(RootNode.class);
		when(rootNode.getClsp()).thenReturn(clsp);
		when(dex.root()).thenReturn(rootNode);
	}

	@Test
	public void test() {
		ArgType objExc = object(JAVA_LANG_EXCEPTION);
		ArgType objThr = object(JAVA_LANG_THROWABLE);

		assertTrue(clsp.isImplements(JAVA_LANG_EXCEPTION, JAVA_LANG_THROWABLE));
		assertFalse(clsp.isImplements(JAVA_LANG_THROWABLE, JAVA_LANG_EXCEPTION));

		assertFalse(ArgType.isCastNeeded(dex, objExc, objThr));
		assertTrue(ArgType.isCastNeeded(dex, objThr, objExc));

		assertTrue(ArgType.isCastNeeded(dex, ArgType.OBJECT, STRING));
	}
}
