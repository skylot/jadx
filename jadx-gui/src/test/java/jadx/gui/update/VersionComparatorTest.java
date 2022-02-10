package jadx.gui.update;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class VersionComparatorTest {



	//if project tips not support ParameterizedTest,
	// need to refresh or reload project by Gradle tool
	@ParameterizedTest
	@CsvSource({
			"1,2, -1"
			,"v2.0,1.2, 1"
			,"2.0,v2, 0"
			,"v2.1,v2.1, 0"
			,"02.1,2.1.0, 0"
			,"v02.1,3, -1"
			,"03,v2.1, 1"
			,"v03,v2, 1"
			,"2.3,02, 1"
			,"v2.2,02.2, 0"
			,"2,v03, -1"
			,"v1,v01, 0"
			,"02,02.0, 0"
			,"v02,02, 0"
			,"3.03,v2.3, 1"
			,"v02.3,v03, -1"
	})
	public void testCheckAndCompareNormalInput(String a, String b, int expected){
		assertThat(VersionComparator.checkAndCompare(a, b), is(expected));
	}

	/*
	 *	SWE 261P Project:
	 *	Part 1 Functional Testing and Partitioning
	 * 	2. Test improper inputs:
	 * (1) negative: -2, -3
	 * (2) letter: a, A
	 * (3) whether more than Integer.MAX_VALUE: > 2147483647
	 * (4) illegal char: %, #
	 * */
	@ParameterizedTest
	@CsvSource({
			"-3, 2, -2"
			,"-3, -2, -2"
			,"2, -3, -2"

			,"a, b, -2"
			,"3, a, -2"
			,"a, 2, -2"
			,"A, B, -2"
			,"3, A, -2"
			,"A, 3, -2"
			,"C, b, -2"
			,"a, B, -2"

			,"2147483647, 3, 1"
			,"3, 2147483647, -1"
			,"2147483648, 3, -2"
			,"3, 2147483648, -2"
			,"2147483648, 2147483649, -2"

			,"%, *, -2"
			,"%, #, -2"
			,"*, %, -2"
	})
	public void testCheckAndCompareImproperInput(String a, String b, int expected){
		assertThat(VersionComparator.checkAndCompare(a, b), is(expected));
	}
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
