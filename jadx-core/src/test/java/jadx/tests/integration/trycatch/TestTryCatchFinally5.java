package jadx.tests.integration.trycatch;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestTryCatchFinally5 extends IntegrationTest {

	public static class TestCls {
		public <E> List<E> test(A a, B<E> b) {
			C c = p(a);
			if (c == null) {
				return null;
			}
			D d = b.f(c);
			try {
				if (!d.first()) {
					return null;
				}
				List<E> list = new ArrayList<>();
				do {
					list.add(b.load(d));
				} while (d.toNext());
				return list;
			} finally {
				d.close();
			}
		}

		private C p(A a) {
			return (C) a;
		}

		private interface A {
		}

		private interface B<T> {
			D f(C c);

			T load(D d);
		}

		private interface C {
		}

		private interface D {
			boolean first();

			boolean toNext();

			void close();
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("} finally {"));
	}

	@Test
	@NotYetImplemented
	public void test2() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("d.close();"));
	}
}
