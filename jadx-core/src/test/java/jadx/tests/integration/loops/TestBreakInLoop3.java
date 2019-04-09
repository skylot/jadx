package jadx.tests.integration.loops;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;
import java.io.File;
import java.io.FileOutputStream;
import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

public class TestBreakInLoop3 extends IntegrationTest {

	public static class TestCls {

		public static void writeMore(String fid) {
			boolean tryMkdir = true;
			File ff = new File(fid);
			while (true) {
				prt("before try");
				try {
					new FileOutputStream(fid).close();
				} catch (Exception ex) {
					if (tryMkdir) {  // On first error, try creating the base dirs.
						tryMkdir = false;
						prt("  then block of if stmt in catch, before 'continue'");
						continue;
					}
					prt("  after if stmt in catch block");
					if (ff.exists()) {
						prt("then more stuff");
					} else {
						prt("else more stuff");
					}
					prt("  end of after if stmt in catch block");
				}
				prt("after catch, before break");
				break;
			} // end of while true, loop to allow retry of fos.write after mkdir
			prt("after while loop");
		} // end of writeMore

		private static void prt(String s) {
			System.out.println(s);
		}
	}

	@Test
	@NotYetImplemented
	public void test43() throws Exception {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("continue;"));
	}
}
