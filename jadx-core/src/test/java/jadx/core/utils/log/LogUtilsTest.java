package jadx.core.utils.log;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogUtilsTest {

	@Test
	void escape() {
		assertThat(LogUtils.escape("Guest'%0AUser:'Admin")).isEqualTo("Guest..0AUser..Admin");
	}
}
