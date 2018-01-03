package jadx.tests.integration.trycatch;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.junit.Test;

import jadx.core.clsp.NClass;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.junit.Assert.assertThat;

public class TestTryCatchFinally2 extends IntegrationTest {

	public static class TestCls {
		private NClass[] classes;

		public void test(OutputStream output) throws IOException {
			DataOutputStream out = new DataOutputStream(output);
			try {
				out.writeByte(1);
				out.writeInt(classes.length);
				for (NClass cls : classes) {
					writeString(out, cls.getName());
				}
				for (NClass cls : classes) {
					NClass[] parents = cls.getParents();
					out.writeByte(parents.length);
					for (NClass parent : parents) {
						out.writeInt(parent.getId());
					}
				}
			} finally {
				out.close();
			}
		}

		private void writeString(DataOutputStream out, String name) {
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("} finally {"));
		assertThat(code, containsOne("out.close();"));

		assertThat(code, containsOne("for (NClass parent : parents) {"));

		// TODO
//		assertThat(code, countString(2, "for (NClass cls : classes) {"));
		assertThat(code, containsOne("for (NClass cls : this.classes) {"));
		assertThat(code, containsOne("for (NClass cls2 : this.classes) {"));
	}
}
