package jadx.tests.internal.enums;

import jadx.api.InternalJadxTest;
import jadx.core.dex.nodes.ClassNode;

import org.junit.Test;

import static jadx.tests.utils.JadxMatchers.countString;
import static org.junit.Assert.assertThat;

public class TestSwitchOverEnum extends InternalJadxTest {

	public enum Count {
		ONE, TWO, THREE
	}

	public int testEnum(Count c) {
		switch (c) {
			case ONE:
				return 1;
			case TWO:
				return 2;
			case THREE:
				return 3;
		}
		return 0;
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestSwitchOverEnum.class);
		String code = cls.getCode().toString();
		System.out.println(code);

		assertThat(code, countString(1, "synthetic"));
		assertThat(code, countString(2, "switch (c) {"));
		assertThat(code, countString(2, "case ONE:"));
	}
}
