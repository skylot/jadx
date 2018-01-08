package jadx.tests.integration.enums;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.countString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class TestSwitchOverEnum extends IntegrationTest {

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
		assertEquals(1, testEnum(Count.ONE));
		assertEquals(2, testEnum(Count.TWO));
		assertEquals(0, testEnum(Count.THREE));
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestSwitchOverEnum.class);
		String code = cls.getCode().toString();

		assertThat(code, countString(1, "synthetic"));
		assertThat(code, countString(2, "switch (c) {"));
		assertThat(code, countString(2, "case ONE:"));
	}
}
