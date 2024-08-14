package jadx.core.xmlgen.entry;

import java.util.Map;

import org.junit.jupiter.api.Test;

import jadx.core.utils.android.AndroidResourcesMap;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

class ValuesParserTest {

	@Test
	void testResMapLoad() {
		Map<Integer, String> androidResMap = AndroidResourcesMap.getMap();
		assertThat(androidResMap).isNotNull().isNotEmpty();
	}
}
