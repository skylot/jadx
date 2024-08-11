package jadx.tests.integration.invoke;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestCastInOverloadedInvoke2 extends SmaliTest {

	@Test
	public void test() {
		disableCompilation();
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("new Intent().putExtra(\"param\", (Parcelable) null);");
	}
}
