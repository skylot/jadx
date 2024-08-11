package jadx.tests.integration.inner;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestAnonymousClass5 extends IntegrationTest {

	public static class TestCls {
		private final Map<String, TestCls> map = new HashMap<>();
		private int a;

		public Iterable<TestCls> test(String name) {
			final TestCls cls = map.get(name);
			if (cls == null) {
				return null;
			}
			final int listSize = cls.size();
			final Iterator<TestCls> iterator = new Iterator<TestCls>() {
				int counter = 0;

				@Override
				public TestCls next() {
					cls.a++;
					counter++;
					return cls;
				}

				@Override
				public boolean hasNext() {
					return counter < listSize;
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
			return new Iterable<TestCls>() {
				@Override
				public Iterator<TestCls> iterator() {
					return iterator;
				}
			};
		}

		private int size() {
			return 7;
		}

		public void check() {
			TestCls v = new TestCls();
			v.a = 3;
			map.put("a", v);
			Iterable<TestCls> it = test("a");
			TestCls next = it.iterator().next();
			assertThat(next).isSameAs(v);
			assertThat(next.a).isEqualTo(4);
		}
	}

	@Test
	@NotYetImplemented
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("map.get(name);")
				.doesNotContain("access$008")
				.doesNotContain("synthetic");
	}
}
