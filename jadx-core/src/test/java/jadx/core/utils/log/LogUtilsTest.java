package jadx.core.utils.log;

import org.junit.jupiter.api.Test;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

class LogUtilsTest {

	@Test
	void escape() {
		String src = "a.b,c:d;e disallowed\"a'b#c*d\te\rf\ng";
		String out = "a.b,c:d;e disallowed.a.b.c.d.e.f.g";
		assertThat(LogUtils.escape(src)).isEqualTo(out);
	}
}
