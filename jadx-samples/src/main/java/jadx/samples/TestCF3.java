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

	private int exc() throws Exception {
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
					if (s1.length() == 5) {
						l2.add(s1);
					} else {
						l1.remove(s2);
					}
				}
			}
		}
		if (l2.size() > 0) {
			l1.clear();
		}
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

	private int testLoops(int[] a, int b) {
		int i = 0;
		while (i < a.length && i < b) {
			a[i]++;
			i++;
		}
		while (i < a.length) {
			a[i]--;
			i++;
		}
		int sum = 0;
		for (int e : a) {
			sum += e;
		}
		return sum;
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

	public String testReturnInLoop(List<String> list) {
		Iterator<String> it = list.iterator();
		while (it.hasNext()) {
			String ver = it.next();
			if (ver != null) {
				return ver;
			}
		}
		return "error";
	}

	public String testReturnInLoop2(List<String> list) {
		try {
			Iterator<String> it = list.iterator();
			while (it.hasNext()) {
				String ver = it.next();
				exc();
				if (ver != null) {
					return ver;
				}
			}
		} catch (Exception e) {
			setEnabled(false);
		}
		return "error";
	}

	public int testComplexIfInLoop(boolean a) {
		int i = 0;
		while (a && i < 10) {
			i++;
		}
		return i;
	}

	public int testComplexIfInLoop2(int k) {
		int i = k;
		while (i > 5 && i < 10) {
			i++;
		}
		return i;
	}

	public int testComplexIfInLoop3(int k) {
		int i = k;
		while (i > 5 && i < k * 3) {
			if (k == 8) {
				i++;
			} else {
				break;
			}
		}
		return i;
	}

	private void f() {
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			// ignore
		}
	}

	public long testInline() {
		long l = System.nanoTime();
		f();
		return System.nanoTime() - l;
	}

	private int f2 = 1;

	public void func() {
		this.f2++;
	}

	public boolean testInline2() {
		int a = this.f2;
		func();
		return a != this.f2;
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
				new ArrayList<>(Arrays.asList("a1", "a2")),
				new ArrayList<>(Arrays.asList("a1", "b2"))));

		List<String> list1 = Arrays.asList(null, "a", "b");
		assertEquals(testReturnInLoop(list1), "a");
		assertEquals(testReturnInLoop2(list1), "a");

		// TODO this line required to omit generic information because it create List<Object>
//		List<String> list2 = Arrays.asList(null, null, null);
//		assertEquals(testReturnInLoop(list2), "error");
//		assertEquals(testReturnInLoop2(list2), "error");

		// assertTrue(testLabeledBreakContinue());

		assertEquals(testComplexIfInLoop(false), 0);
		assertEquals(testComplexIfInLoop(true), 10);

		assertEquals(testComplexIfInLoop2(2), 2);
		assertEquals(testComplexIfInLoop2(6), 10);

		assertEquals(testComplexIfInLoop3(2), 2);
		assertEquals(testComplexIfInLoop3(6), 6);
		assertEquals(testComplexIfInLoop3(8), 24);

		assertEquals(testLoops(new int[]{1, 2, 3, 4, 5, 6}, 2), 19);

		assertTrue(testInline() > 20);
		assertTrue(testInline2());
		return true;
	}

	public static void main(String[] args) throws Exception {
		new TestCF3().testRun();
	}
}
