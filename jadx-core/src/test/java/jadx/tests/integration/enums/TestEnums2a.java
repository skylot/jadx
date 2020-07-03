package jadx.tests.integration.enums;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;
import static jadx.tests.integration.enums.TestEnums2a.TestCls.DoubleOperations.DIVIDE;
import static jadx.tests.integration.enums.TestEnums2a.TestCls.DoubleOperations.TIMES;

public class TestEnums2a extends IntegrationTest {

	public static class TestCls {

		public interface IOps {
			double apply(double x, double y);
		}

		public enum DoubleOperations implements IOps {
			TIMES("*") {
				@Override
				public double apply(double x, double y) {
					return x * y;
				}
			},
			DIVIDE("/") {
				@Override
				public double apply(double x, double y) {
					return x / y;
				}
			};

			private final String op;

			DoubleOperations(String op) {
				this.op = op;
			}

			public String getOp() {
				return op;
			}
		}

		public void check() {
			assertThat(TIMES.getOp()).isEqualTo("*");
			assertThat(DIVIDE.getOp()).isEqualTo("/");

			assertThat(TIMES.apply(2, 3)).isEqualTo(6);
			assertThat(DIVIDE.apply(10, 5)).isEqualTo(2);
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("TIMES(\"*\") {")
				.containsOne("DIVIDE(\"/\")");
	}
}
