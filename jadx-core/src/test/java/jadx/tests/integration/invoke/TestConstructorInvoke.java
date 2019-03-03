package jadx.tests.integration.invoke;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestConstructorInvoke extends IntegrationTest {

	void test(String root, String name) {
		ViewHolder holder = new ViewHolder(root, name);
	}

	private final class ViewHolder {
		private ViewHolder(String root, String name) {
		}
	}

	@Test
	@NotYetImplemented("Variable lost name from debug info")
	public void test() {
		ClassNode cls = getClassNode(TestConstructorInvoke.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne(indent() + "ViewHolder holder = new ViewHolder(root, name);"));
	}

	// Remove after fix above @NYI
	@Test
	public void test2() {
		ClassNode cls = getClassNode(TestConstructorInvoke.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne(indent() + "ViewHolder viewHolder = new ViewHolder(root, name);"));
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		ClassNode cls = getClassNode(TestConstructorInvoke.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne(indent() + "ViewHolder viewHolder = new ViewHolder("));
	}
}
