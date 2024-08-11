package jadx.tests.integration.enums;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

public class TestSwitchOverEnum2 extends IntegrationTest {

	public enum Count {
		ONE, TWO, THREE
	}

	public enum Animal {
		CAT, DOG
	}

	public int testEnum(Count c, Animal a) {
		int result = 0;
		switch (c) {
			case ONE:
				result = 1;
				break;
			case TWO:
				result = 2;
				break;
		}
		switch (a) {
			case CAT:
				result += 10;
				break;
			case DOG:
				result += 20;
				break;
		}
		return result;
	}

	public void check() {
		assertThat(testEnum(Count.ONE, Animal.DOG)).isEqualTo(21);
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestSwitchOverEnum2.class))
				.code()
				.countString(1, "synthetic")
				.countString(2, "switch (c) {")
				.countString(2, "case ONE:")
				.countString(2, "case DOG:");
	}
}
