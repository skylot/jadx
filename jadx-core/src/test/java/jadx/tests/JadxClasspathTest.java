package jadx.tests;

import jadx.core.clsp.ClspGraph;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.utils.exceptions.DecodeException;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import static jadx.core.dex.instructions.args.ArgType.object;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JadxClasspathTest {

	private static final String JAVA_LANG_EXCEPTION = "java.lang.Exception";
	private static final String JAVA_LANG_THROWABLE = "java.lang.Throwable";

	ClspGraph clsp;

	@Before
	public void initClsp() throws IOException, DecodeException {
		clsp = new ClspGraph();
		clsp.load();
		ArgType.setClsp(clsp);
	}

	@Test
	public void test() {
		ArgType objExc = object(JAVA_LANG_EXCEPTION);
		ArgType objThr = object(JAVA_LANG_THROWABLE);

		assertTrue(clsp.isImplements(JAVA_LANG_EXCEPTION, JAVA_LANG_THROWABLE));
		assertFalse(clsp.isImplements(JAVA_LANG_THROWABLE, JAVA_LANG_EXCEPTION));

		assertFalse(ArgType.isCastNeeded(objExc, objThr));
		assertTrue(ArgType.isCastNeeded(objThr, objExc));
	}
}
