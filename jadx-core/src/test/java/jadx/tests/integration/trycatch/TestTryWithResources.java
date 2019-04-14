package jadx.tests.integration.trycatch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

public class TestTryWithResources extends SmaliTest {

	public static class TestCls {

		public static void writeFully(File file, byte[] data) throws IOException {
			try (OutputStream out = new FileOutputStream(file)) {
				out.write(data);
			}
		}
	}

	@Test
	@NotYetImplemented
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("try ("));
		assertThat(code, not(containsString("close()")));
	}
}
