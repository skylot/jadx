package jadx.tests.integration.enums;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestEnums9 extends IntegrationTest {

	public static class TestCls {
		public enum Types {
			INT,
			FLOAT,
			LONG,
			DOUBLE,
			OBJECT,
			ARRAY;

			private static Set<Types> primitives = EnumSet.of(INT, FLOAT, LONG, DOUBLE);
			public static List<Types> references = new ArrayList<>();

			static {
				references.add(OBJECT);
				references.add(ARRAY);
			}

			public static Set<Types> getPrimitives() {
				return primitives;
			}
		}

		public void check() {
			assertThat(Types.getPrimitives()).contains(Types.INT);
			assertThat(Types.references).hasSize(2);
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("EnumSet.of((Enum) INT,");
	}
}
