package jadx.tests.integration.debuginfo;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestLineNumbers3 extends IntegrationTest {

	public static class TestCls extends Exception {

		public TestCls(final Object message) {
			super((message == null) ? "" : message.toString());
			/*
			 * comment to increase line number in return instruction
			 * -
			 * -
			 * -
			 * -
			 * -
			 * -
			 * -
			 * -
			 * -
			 * -
			 */
		}
	}

	@TestWithProfiles({ TestProfile.DX_J8, TestProfile.JAVA8 })
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		assertThat(cls).code().containsOne("super(message == null ? \"\" : message.toString());");
		String linesMapStr = cls.getCode().getCodeMetadata().getLineMapping().toString();
		assertThat(linesMapStr).isEqualTo("{4=13, 5=14, 6=15}");
	}
}
