package jadx.tests.integration.types;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestPrimitiveConversion2 extends SmaliTest {

	@Test
	public void test() {
		disableCompilation();
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("boolean z2 = !convertedPrice2.code.equals(itemCurrency.code);")
				.doesNotContain("z2 == 0")
				.doesNotContain("z2 | 2")
				.containsOne("(z2 ? 1 : 0) | 2")
				.containsOne("if (z2 && formatCurrency != null) {")
				.containsOne("i = 1;");
	}
}
