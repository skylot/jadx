package jadx.tests.integration.deobf;

import java.util.List;
import java.util.Queue;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestFieldFromInnerClass extends IntegrationTest {

	public static class TestCls<T> {
		TestCls<T>.I f;

		public class I {
			Queue<T> a;

			Queue<TestCls<T>.I> b;

			public class X {
				List<TestCls<T>.I.X> c;
			}
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		enableDeobfuscation();

		ClassNode cls = getClassNode(TestCls.class);
		assertThat(cls)
				.code()
				.doesNotContain("class I {")
				.doesNotContain(".I ")
				.doesNotContain(".I.X>");
	}
}
