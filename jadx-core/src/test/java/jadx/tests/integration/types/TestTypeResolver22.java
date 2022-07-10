package jadx.tests.integration.types;

import java.io.IOException;
import java.io.InputStream;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestTypeResolver22 extends IntegrationTest {

	public static class TestCls {
		public void test(InputStream input, long count) throws IOException {
			long pos = input.skip(count);
			while (pos < count) {
				pos += input.skip(count - pos);
			}
		}
	}

	@TestWithProfiles({ TestProfile.JAVA8, TestProfile.DX_J8 })
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("long pos = ");
	}
}
