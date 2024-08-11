package jadx.core.utils.log;

import org.junit.jupiter.api.Test;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

class LogUtilsTest {

	@Test
	void escape() {
		assertThat(LogUtils.escape("Guest'%0AUser:'Admin")).isEqualTo("Guest..0AUser..Admin");
	}
}
