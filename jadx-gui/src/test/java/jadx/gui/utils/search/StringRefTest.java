package jadx.gui.utils.search;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//import com.android.tools.r8.internal.S;
import org.junit.jupiter.api.Test;

import static jadx.gui.utils.search.StringRef.fromStr;
import static jadx.gui.utils.search.StringRef.subString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class StringRefTest {

	@Test
	public void testConvert() {
		assertThat(fromStr("a").toString(), is("a"));
	}

	@Test
	public void testSubstring() {
		checkStr(subString("a", 0), "a");
		checkStr(subString("a", 1), "");
		checkStr(subString("a", 0, 0), "");
		checkStr(subString("a", 0, 1), "a");
		checkStr(subString("abc", 1, 2), "b");
		checkStr(subString("abc", 2), "c");
		checkStr(subString("abc", 2, 3), "c");
	}

	public static void checkStr(StringRef ref, String str) {
		assertThat(ref.toString(), is(str));
		assertThat(ref, is(fromStr(str)));
	}

	@Test
	public void testTrim() {
		checkTrim(fromStr("a"), "a");
		checkTrim(fromStr(" a "), "a");
		checkTrim(fromStr("\ta"), "a");
		checkTrim(subString("a b c", 1), "b c");
		checkTrim(subString("a b\tc", 1, 4), "b");
		checkTrim(subString("a b\tc", 2, 3), "b");
	}

	private static void checkTrim(StringRef ref, String result) {
		assertThat(ref.trim().toString(), is(result));
	}

	@Test
	public void testSplit() {
		checkSplit("abc", "b", "a", "c");
		checkSplit("abc", "a", "", "bc");
		checkSplit("abc", "c", "ab");
		checkSplit("abc", "d", "abc");
		checkSplit("abbbc", "b", "a", "", "", "c");
		checkSplit("abbbc", "bb", "a", "bc");
		checkSplit("abbbc", "bbb", "a", "c");
		checkSplit("abbbc", "bbc", "ab");
		checkSplit("abbbc", "bbbc", "a");
	}

	private static void checkSplit(String str, String splitBy, String... result) {
		List<StringRef> expectedStringRegList = Stream.of(result).map(StringRef::fromStr).collect(Collectors.toList());
		assertThat(StringRef.split(str, splitBy), is(expectedStringRegList));

		// compare with original split
		assertThat(str.split(splitBy), is(result));
	}

	@Test
	public void testIndexOf() {
		//test empty str, insentitive, and 3 kinds of from point
		checkIndexOf(fromStr("aBc"),"",-1,false,-1);
		checkIndexOf(fromStr(""),"",-1,false,-1);
		checkIndexOf(fromStr("Ab"),"",0,false,-1);
		checkIndexOf(fromStr(""),"",0,false,0);
		checkIndexOf(fromStr("aBc"),"",3,false,3);

		// sensitive
		checkIndexOf(fromStr("Abc"),"",-1,true,-1);
		checkIndexOf(fromStr(""),"",-1,true,-1);
		checkIndexOf(fromStr("Ab"),"",0,true,-1);
		checkIndexOf(fromStr(""),"",0,true,0);
		checkIndexOf(fromStr("aBc"),"",3,true,3);

		//test no empty str, insentitive, and 3 kinds of from point, find or not find
		checkIndexOf(fromStr("aBc"),"A",-1,true,0);
		checkIndexOf(fromStr("abC"),"d",-1,true,-1);
		checkIndexOf(fromStr(""),"a",-1,true,-1);
		checkIndexOf(fromStr("abC"),"Abc",0,true,0);
		checkIndexOf(fromStr("abc"),"A",1,true,-1); //not find
		checkIndexOf(fromStr("aBc"),"A",4,true,-1);

		//no empty str, sensitive, 3 kinds of value, find or not find
		checkIndexOf(fromStr("Abc"),"A",-1,false,0);
		checkIndexOf(fromStr("abc"),"A",-1,false,-1);
		checkIndexOf(fromStr(""),"a",-1,false,-1);
		checkIndexOf(fromStr("abc"),"bc",1,false,1);
		checkIndexOf(fromStr("abc"),"a",1,false,-1); //not find
		checkIndexOf(fromStr("abc"),"Bc",1,false,-1);
		checkIndexOf(fromStr("abc"),"a",4,false,-1);
	}

	private static void checkIndexOf(StringRef ref, String str,int from, boolean caseInsensitive, int pos){
		assertThat(ref.indexOf(str,from,caseInsensitive),is(pos));
	}

	@Test
	public void checkLength(){
		StringRef stringRef = StringRef.fromStr("");
		assertThat(stringRef.length(),is(0));
		stringRef = StringRef.fromStr("asa");
		assertThat(stringRef.length(),is(3));
	}

	@Test
	public void checkCharAt(){
		StringRef stringRef = StringRef.subString("abc",1);
		assertThat(stringRef.charAt(1),is('c'));
	}

	//this is an error I think
	@Test
	public void checkSubSequence(){
		StringRef stringRef = StringRef.subString("abcde",1,3);
//		System.out.println(stringRef.subSequence(0,2));
		CharSequence a = StringRef.fromStr("ab");
		assertThat(stringRef.subSequence(0,2),is(a));
	}

	@Test
	public void checkIndexOf(){
		StringRef stringRef = StringRef.fromStr("asA");
		assertThat(stringRef.indexOf("A",true),is(0));
		assertThat(stringRef.indexOf("A"),is(2));
		assertThat(stringRef.indexOf("a",1),is(-1));
	}

	@Test
	public void checkStartsWith(){
		StringRef stringRef = StringRef.fromStr("asA");
		assertThat(stringRef.startsWith("abcd"),is(false));
		assertThat(stringRef.startsWith("as"),is(true));
		assertThat(stringRef.startsWith("ad"),is(false));
	}

	@Test
	public void checkEmptySplit(){
		assertThat(StringRef.split("",""),is(Collections.emptyList()));
	}
	
}
