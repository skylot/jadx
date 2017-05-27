package jadx.tests.integration.enums;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.countString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

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
		assertEquals(21, testEnum(Count.ONE, Animal.DOG));
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestSwitchOverEnum2.class);
		String code = cls.getCode().toString();

		assertThat(code, countString(1, "synthetic"));
		assertThat(code, countString(2, "switch (c) {"));
		assertThat(code, countString(2, "case ONE:"));
		assertThat(code, countString(2, "case DOG:"));
	}
}
