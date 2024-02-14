package jadx.tests.integration.enums;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestSwitchOverEnum extends SmaliTest {

	public enum Count {
		ONE, TWO, THREE
	}

	public int testEnum(Count c) {
		switch (c) {
			case ONE:
				return 1;
			case TWO:
				return 2;
		}
		return 0;
	}

	public void check() {
		assertThat(testEnum(Count.ONE)).isEqualTo(1);
		assertThat(testEnum(Count.TWO)).isEqualTo(2);
		assertThat(testEnum(Count.THREE)).isEqualTo(0);
	}

	@Test
	public void test() {
		// remapping array placed in top class, place test also in top class
		assertThat(getClassNode(TestSwitchOverEnum.class))
				.code()
				.countString(1, "synthetic")
				.countString(2, "switch (c) {")
				.countString(3, "case ONE:");
	}

	/**
	 * Java 21 compiler can omit a remapping array and use switch over ordinal directly
	 */
	@Test
	public void testSmaliDirect() {
		assertThat(getClassNodeFromSmaliFiles())
				.code()
				.containsOne("switch (v) {")
				.containsOne("case ONE:");
	}
}
