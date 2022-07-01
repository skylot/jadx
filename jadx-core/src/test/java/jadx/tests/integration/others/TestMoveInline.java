package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

@SuppressWarnings("CommentedOutCode")
public class TestMoveInline extends SmaliTest {
	// @formatter:off
	/*
		public final void Y(int i) throws k {
			int i2 = 0;
			while ((i & (-128)) != 0) {
				this.h[i2] = (byte) ((i & 127) | 128);
				i >>>= 7;
				i2++;
			}
			byte[] bArr = this.h;
			bArr[i2] = (byte) i;
			this.a.k(bArr, 0, i2 + 1);
		}
	*/
	// @formatter:on

	@Test
	public void test() {
		assertThat(getClassNodeFromSmali())
				.code()
				// check operations order
				.containsLines(3,
						"i >>>= 7;",
						"i2++;");
	}
}
