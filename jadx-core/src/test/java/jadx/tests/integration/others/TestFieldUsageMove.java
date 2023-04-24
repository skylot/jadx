package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestFieldUsageMove extends SmaliTest {

	public static class TestCls {
		public static void test(Object obj) {
			if (obj instanceof Boolean) {
				System.out.println("Boolean: " + obj);
			}
			if (obj instanceof Float) {
				System.out.println("Float: " + obj);
			}
		}
	}

	@TestWithProfiles(TestProfile.D8_J11)
	public void test() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("System.out.println(\"Boolean: \" +")
				.containsOne("System.out.println(\"Float: \" +");
	}

	@Test
	public void testSmali() {
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("System.out.println(\"Boolean: \" +")
				.containsOne("System.out.println(\"Float: \" +");
	}
}
