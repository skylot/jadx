package jadx.core.xmlgen.entry;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ValuesParserTest {

	@Test
	void testResMapLoad() {
		Map<Integer, String> androidResMap = ValuesParser.getAndroidResMap();
		assertThat(androidResMap).isNotNull().isNotEmpty();
	}
}
