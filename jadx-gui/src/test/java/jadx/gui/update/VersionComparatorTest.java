package jadx.gui.update;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class VersionComparatorTest {

	/*
	*	SWE 261P Project:
	*	Part 1 Functional Testing and Partitioning
	* 	1. Test normal inputs
	* 	(1) whether input start with “v”:
	*		We split it into 4 types, like “*, *”, “v*, *”, “*, v*”, “v*, v*”.
	*	(2) whether numbers start with zero:
	*		We split it into 4 types, like “*, *”, “0*, *”, “*, 0*”, “0*, 0*”
	*	(3) whether input has multiple layers:
	*		We split it into 4 types, like “1, 1”, “1, mutil”, “mutil, 1”, “mutil, mutil”
	*	(4) different value of two input:
	*		We split it into 4 types, like “a<b”, “a>b”, “a==b”
	*
	*	Pairwise 2-way test suite
	* 	starts with v	zero+numbers	1, mutil layers		a<b, a>b,a==b	Example
	*	*, *			*, *			1, 1				a<b				1,2
	*	v*, *			*, *			1, mutil			a>b				v2.0,1.2
	*	*, v*			*, *			mutil, 1			a==b			2.0,v2
	*	v*, v*			*, *			mutil, mutil		a==b			v2.1,v2.1
	*	*, *			0*, *			mutil, mutil		a==b			02.1,2.1.0
	*	v*, *			0*, *			mutil, 1			a<b				v02.1,3
	*	*, v*			0*, *			1, mutil			a>b				03,v2.1
	*	v*, v*			0*, *			1, 1				a>b				v03,v2
	*	*, *			*, 0*			mutil, 1			a>b				2.3,02
	*	v*, *			*, 0*			mutil, mutil		a==b			v2.2,02.2
	*	*, v*			*, 0*			1, 1				a<b				2,v03
	*	v*, v*			*, 0*			1, mutil			a==b			v1,v01
	*	*, *			0*, 0*			1, mutil			a==b			02,02.0
	*	v*, *			0*, 0*			1, 1				a==b			v02,02
	*	*, v*			0*, 0*			mutil, mutil		a>b				3.03,v2.3
	*	v*, v*			0*, 0*			mutil, 1			a<b				v02.3,v03
	* */

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
