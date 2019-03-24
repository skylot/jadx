package jadx.gui.update;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class VersionComparatorTest {

	@Test
	public void testCompare() {
		checkCompare("", "", 0);
		checkCompare("1", "1", 0);
		checkCompare("1", "2", -1);
		checkCompare("1.1", "1.1", 0);
		checkCompare("0.5", "0.5", 0);
		checkCompare("0.5", "0.5.0", 0);
		checkCompare("0.5", "0.5.00", 0);
		checkCompare("0.5", "0.5.0.0", 0);
		checkCompare("0.5", "0.5.0.1", -1);
		checkCompare("0.5.0", "0.5.0", 0);
		checkCompare("0.5.0", "0.5.1", -1);
		checkCompare("0.5", "0.5.1", -1);
		checkCompare("0.4.8", "0.5", -1);
		checkCompare("0.4.8", "0.5.0", -1);
		checkCompare("0.4.8", "0.6", -1);
	}

	private static void checkCompare(String a, String b, int result) {
		assertThat(VersionComparator.compare(a, b), is(result));
		assertThat(VersionComparator.compare(b, a), is(-result));
	}
}
