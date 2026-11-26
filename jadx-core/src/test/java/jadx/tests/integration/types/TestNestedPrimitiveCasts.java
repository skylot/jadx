package jadx.tests.integration.types;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

public class TestNestedPrimitiveCasts extends IntegrationTest {

	public static class TestCls {
		public long roundToFloat(long l) {
			return (long) (float) l;
		}

		public int roundIntToFloat(int i) {
			return (int) (float) i;
		}

		public int truncateToLong(double d) {
			return (int) (long) d;
		}

		public long sum(long l) {
			return (int) l + (long) (float) l;
		}

		public byte toByte(long l) {
			return (byte) l;
		}

		public byte floatToByte(float f) {
			return (byte) f;
		}

		public void check() {
			assertThat(roundToFloat(16777217L)).isEqualTo(16777216L);
			assertThat(roundIntToFloat(16777217)).isEqualTo(16777216);
			assertThat(truncateToLong(1e10)).isEqualTo(1410065408);
			assertThat(sum(16777217L)).isEqualTo(33554433L);
			assertThat(toByte(300L)).isEqualTo((byte) 44);
			assertThat(floatToByte(300.5f)).isEqualTo((byte) 44);
		}
	}

	@TestWithProfiles({ TestProfile.D8_J11, TestProfile.JAVA11 })
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("return (long) ((float) l);")
				.containsOne("return (int) ((float) i);")
				.containsOne("return (int) ((long) d);")
				.containsOne("return (byte) l;")
				.containsOne("return (byte) f;");
	}
}
