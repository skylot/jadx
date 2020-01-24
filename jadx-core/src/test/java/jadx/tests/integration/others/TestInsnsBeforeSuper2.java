package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestInsnsBeforeSuper2 extends SmaliTest {
	// @formatter:off
	/*
		public class TestInsnsBeforeSuper2 extends java.lang.Exception {
			private int mErrorType;

			public TestInsnsBeforeSuper2(java.lang.String r9, int r10) {
				r8 = this;
				r0 = r8
				r1 = r9
				r2 = r10
				r3 = r0
				r4 = r1
				r5 = r2
				r6 = r1
				r0.<init>(r6)
				r7 = 0
				r0.mErrorType = r7
				r0.mErrorType = r2
				return
			}
		}
	*/
	// @formatter:on

	@Test
	public void test() {
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("super(message);")
				.containsOne("this.mErrorType = errorType;");
	}
}
