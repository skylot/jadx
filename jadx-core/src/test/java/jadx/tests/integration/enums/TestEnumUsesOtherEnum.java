package jadx.tests.integration.enums;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestEnumUsesOtherEnum extends SmaliTest {

	public static class TestCls {

		public enum VType {
			INT(1),
			OTHER_INT(INT);

			private final int type;

			VType(int type) {
				this.type = type;
			}

			VType(VType refType) {
				this(refType.type);
			}
		}
	}

	@TestWithProfiles(TestProfile.D8_J11)
	public void test() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("OTHER_INT(INT);")
				.doesNotContain("\n        \n"); // no indentation for empty string
	}

	@Test
	public void testSmali() {
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("public enum TestEnumUsesOtherEnum {")
				.doesNotContain("static {");
	}
}
