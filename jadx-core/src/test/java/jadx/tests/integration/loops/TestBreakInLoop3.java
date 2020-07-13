package jadx.tests.integration.loops;

import java.io.File;
import java.io.FileOutputStream;

import jadx.NotYetImplemented;
import jadx.tests.api.IntegrationTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestBreakInLoop3 extends IntegrationTest {

	public static class TestCls {

		private StringBuilder builder = new StringBuilder();

		public void writeMore(String fid) {
			boolean tryMkdir = true;
			File ff = new File(fid);
			while (true) {
				prt("1");
				try {
					new FileOutputStream(fid).close();
				} catch (Exception ex) {
					if (tryMkdir) { // On first error, try creating the base dirs.
						tryMkdir = false;
						prt("2");
						continue;
					}
					prt("3");
					if (ff.exists()) {
						prt("4");
					} else {
						prt("5");
					}
					prt("6");
				}
				prt("7");
				break;
			} // end of while true, loop to allow retry of fos.write after mkdir
			prt("8");
		} // end of writeMore

		private void prt(String s) {
			builder.append(s);
		}

		public void check() {
			writeMore("");
			assertEquals("12135678", builder.toString());
		}
	}

	// @Test
	@NotYetImplemented
	public void test43() throws Exception {
		getClassNode(TestCls.class);
	}
}
