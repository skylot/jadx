package jadx.samples;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestGenerics extends AbstractTest {

	private List<String> test1(Map<String, String> map) {
		List<String> list = new ArrayList<String>();
		String str = map.get("key");
		list.add(str);
		return list;
	}

	public void test2(Map<String, String> map, List<Object> list) {
		String str = map.get("key");
		list.add(str);
	}

	public void test3(List<Object> list, int a, float[] b, String[] c, String[][][] d) {

	}

	@Override
	public boolean testRun() throws Exception {
		assertTrue(test1(new HashMap<String, String>()) != null);
		return true;
	}

	public static void main(String[] args) throws Exception {
		new TestGenerics().testRun();
	}
}
