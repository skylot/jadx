package jadx.tests.integration.generics;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestMissingGenericsTypes2 extends SmaliTest {
	// @formatter:off
	/*
	package generics;

	import java.util.Iterator;

	public class TestMissingGenericsTypes2<T> implements Iterable<T> {

		@Override
		public Iterator<T> iterator() {
			return null;
		}

		public void test(TestMissingGenericsTypes2<String> l) {
			Iterator<String> i = l.iterator(); // <-- This generics type was removed in smali
			while (i.hasNext()) {
				String s = i.next();
				doSomething(s);
			}
		}

		private void doSomething(String s) {
		}
	}
	*/
		// @formatter:on

	@Test
	public void test() {
		assertThat(getClassNodeFromSmali())
				.code()
				.contains("for (String s : l) {")
				.doesNotContain("Iterator i");
	}
}
