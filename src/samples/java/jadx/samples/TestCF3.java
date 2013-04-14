package jadx.samples;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class TestCF3 extends AbstractTest {

	public String f = "str//ing";
	private boolean enabled;

	private void setEnabled(boolean b) {
		this.enabled = b;
	}

	private int next() {
		return 1;
	}

	public void testSwitchInLoop() throws Exception {
		while (true) {
			int n = next();
			switch (n) {
				case 0:
					setEnabled(false);
					break;

				case 1:
					setEnabled(true);
					return;
			}
		}
	}

	private void testIfInLoop() {
		int j = 0;
		for (int i = 0; i < f.length(); i++) {
			char ch = f.charAt(i);
			if (ch == '/') {
				j++;
				if (j == 2) {
					setEnabled(true);
					return;
				}
			}
		}
		setEnabled(false);
	}

	public boolean testNestedLoops(List<String> l1, List<String> l2) {
		Iterator<String> it1 = l1.iterator();
		while (it1.hasNext()) {
			String s1 = it1.next();
			Iterator<String> it2 = l2.iterator();
			while (it2.hasNext()) {
				String s2 = it2.next();
				if (s1.equals(s2)) {
					if (s1.length() == 5)
						l2.add(s1);
					else
						l1.remove(s2);
				}
			}
		}
		if (l2.size() > 0)
			l1.clear();
		return l1.size() == 0;
	}

	public boolean testNestedLoops2(List<String> list) {
		int i = 0;
		int j = 0;
		while (i < list.size()) {
			String s = list.get(i);
			while (j < s.length()) {
				j++;
			}
			i++;
		}
		return j > 10;
	}

	public static boolean testLabeledBreakContinue() {
		String searchMe = "Look for a substring in me";
		String substring = "sub";
		boolean foundIt = false;

		// int max = searchMe.length() - substring.length();
		// test: for (int i = 0; i <= max; i++) {
		// int n = substring.length();
		// int j = i;
		// int k = 0;
		// while (n-- != 0) {
		// if (searchMe.charAt(j++) != substring.charAt(k++)) {
		// continue test;
		// }
		// }
		// foundIt = true;
		// break test;
		// }
		// System.out.println(foundIt ? "Found it" : "Didn't find it");
		return foundIt;
	}

	@Override
	public boolean testRun() throws Exception {
		setEnabled(false);
		testSwitchInLoop();
		assertTrue(enabled);

		setEnabled(false);
		testIfInLoop();
		assertTrue(enabled);

		assertTrue(testNestedLoops(
				new ArrayList<String>(Arrays.asList("a1", "a2")),
				new ArrayList<String>(Arrays.asList("a1", "b2"))));

		// assertTrue(testLabeledBreakContinue());
		return true;
	}

	public static void main(String[] args) throws Exception {
		new TestCF3().testRun();
	}
}
