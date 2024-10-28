package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

// Source: https://github.com/skylot/jadx/issues/1620
public class TestPrimitiveCasts2 extends IntegrationTest {

	@SuppressWarnings("DataFlowIssue")
	public static class TestCls {
		long instanceCount;

		{
			float f = 50.231F;
			instanceCount &= (long) f;
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code();
	}
}
