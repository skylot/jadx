package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestComplexIf extends SmaliTest {

	// @formatter:off
	/*
		public final class TestComplexIf {
			private String a;
			private int b;
			private float c;

			public final boolean test() {
				if (this.a.equals("GT-P6200") || this.a.equals("GT-P6210") || ... ) {
					return true;
				}
				if (this.a.equals("SM-T810") || this.a.equals("SM-T813") || ...) {
					return false;
				}
				return this.c > 160.0f ? true : this.c <= 0.0f && ((this.b & 15) == 4 ? 1 : null) != null;
			}
		}
	 */
	// @formatter:on

	@Test
	public void test() {
		assertThat(getClassNodeFromSmaliWithPkg("conditions", "TestComplexIf"))
				.code()
				.containsOne("if (this.a.equals(\"GT-P6200\") || this.a.equals(\"GT-P6210\") || this.a.equals(\"A100\") "
						+ "|| this.a.equals(\"A101\") || this.a.equals(\"LIFETAB_S786X\") || this.a.equals(\"VS890 4G\")) {");
	}
}
