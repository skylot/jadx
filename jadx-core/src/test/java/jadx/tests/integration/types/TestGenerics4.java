package jadx.tests.integration.types;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestGenerics4 extends IntegrationTest {

	public static class TestCls {

		public static class Inner<T> {
			public void overload(IList<? super T> list) {
			}

			public void overload(T t) {
			}
		}

		public interface IList<T> {
			void list(T t);
		}

		public static class ObjIList implements IList<Object> {
			@Override
			public void list(Object o) {
			}
		}

		public Inner<Object> test() {
			Inner<Object> inner = new Inner<>();
			inner.overload(new ObjIList());
			return inner;
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("public static class ObjIList implements IList<Object> {")
				.containsOne("Inner<Object> inner = new Inner<>();")
				.containsOne("inner.overload((IList<? super Object>) new ObjIList());");
	}

	@NotYetImplemented
	@Test
	public void testOmitCast() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("inner.overload(new ObjIList());");
	}
}
