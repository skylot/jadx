package jadx.tests.integration.enums;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestEnumsWithCustomInit extends IntegrationTest {

	public enum TestCls {
		ONE("I"),
		TWO("II"),
		THREE("III");

		public static final Map<String, TestCls> MAP = new HashMap<>();

		static {
			for (TestCls value : values()) {
				MAP.put(value.toString(), value);
			}
		}

		private final String str;

		TestCls(String str) {
			this.str = str;
		}

		public String toString() {
			return str;
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("ONE(\"I\"),")
				.doesNotContain("new TestEnumsWithCustomInit$TestCls(");
	}
}
