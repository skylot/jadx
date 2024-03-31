package jadx.core.xmlgen.entry;

import java.util.Map;

import org.junit.jupiter.api.Test;

import jadx.core.utils.android.AndroidResourcesMap;

import static org.assertj.core.api.Assertions.assertThat;

class ValuesParserTest {

	@Test
	void testResMapLoad() {
		Map<Integer, String> androidResMap = AndroidResourcesMap.getMap();
		assertThat(androidResMap).isNotNull().isNotEmpty();
	}
}
