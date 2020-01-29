package jadx.tests.integration.trycatch;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.junit.jupiter.api.Test;

import jadx.core.clsp.ClspClass;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestTryCatchFinally2 extends IntegrationTest {

	public static class TestCls {
		private ClspClass[] classes;

		public void test(OutputStream output) throws IOException {
			DataOutputStream out = new DataOutputStream(output);
			try {
				out.writeByte(1);
				out.writeInt(classes.length);
				for (ClspClass cls : classes) {
					writeString(out, cls.getName());
				}
				for (ClspClass cls : classes) {
					ArgType[] parents = cls.getParents();
					out.writeByte(parents.length);
					for (ArgType parent : parents) {
						out.writeInt(parent.getObject().hashCode());
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

		assertThat(code, containsOne("for (ArgType parent : parents) {"));

		assertThat(code, containsOne("for (ClspClass cls : this.classes) {"));
		assertThat(code, containsOne("for (ClspClass cls2 : this.classes) {"));
	}
}
