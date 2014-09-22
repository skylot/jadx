package jadx.tests.integration.enums;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import java.lang.reflect.Method;

import org.junit.Test;

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

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestSwitchOverEnum.class);
		String code = cls.getCode().toString();
		System.out.println(code);

		assertThat(code, countString(1, "synthetic"));
		assertThat(code, countString(2, "switch (c) {"));
		assertThat(code, countString(2, "case ONE:"));

		Method mth = getReflectMethod("testEnum", Count.class);
		assertEquals(1, invoke(mth, Count.ONE));
		assertEquals(2, invoke(mth, Count.TWO));
		assertEquals(0, invoke(mth, Count.THREE));
	}
}
