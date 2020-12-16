package jadx.tests.integration.enums;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestEnumWithConstInlining extends SmaliTest {
	enum TestCls {
		E0,
		E1,
		E2,
		E3,
		E4,
		E5,
		E6,
		E7,
		E8,
		E9,
		E10,
		E11,
		E12,
		E13,
		E14,
		E15,
		E16,
		E17,
		E18,
		E19,
		E20,
		E21,
		E22,
		E23,
		E24,
		E25,
		E26,
		E27,
		E28,
		E29,
		E30,
		E31,
		E32,
		E33,
		E34,
		E35,
		E36,
		E37,
		E38,
		E39,
		E40,
		E41,
		E42,
		E43,
		E44,
		E45,
		E46,
		E47,
		E48,
		E49,
		E50,
		E51,
		E52,
		E53,
		E54,
		E55,
		E56,
		E57,
		E58,
		E59,
		E60,
		E61,
		E62,
		E63,
		E64,
		E65,
		E66,
		E67,
		E68,
		E69,
		E70,
		E71,
		E72,
		E73,
		E74,
		E75,
		E76,
		E77,
		E78,
		E79,
		E80,
		E81,
		E82,
		E83,
		E84,
		E85,
		E86,
		E87,
		E88,
		E89,
		E90,
		E91,
		E92,
		E93,
		E94,
		E95,
		E96,
		E97,
		E98,
		E99,
		E100;

		/**
		 * Match the length of the $VALUES array.
		 */
		public static final int CONST = 101;
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("E42,"));
	}
}
