package jadx.tests.integration.invoke;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestCastInOverloadedInvoke2 extends SmaliTest {

	@Test
	public void test() {
		disableCompilation();

		ClassNode cls = getClassNodeFromSmali();
		String code = cls.getCode().toString();

		assertThat(code, containsOne("new Intent().putExtra(\"param\", (Parcelable) null);"));
	}
}
