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
				.doesNotContain("Iterator i")
				.containsOne("for (String s : l) {");
	}

	@Test
	public void testTypes() {
		// prevent loop from converting to 'for-each' to keep iterator variable type in code
		getArgs().getDisabledPasses().add("LoopRegionVisitor");
		assertThat(getClassNodeFromSmali())
				.code()
				.doesNotContain("Iterator i")
				.containsOne("Iterator<String> it = "); // variable name reject along with type
	}
}
