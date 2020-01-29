package jadx.tests.integration.types;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

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
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("public static class ObjIList implements IList<Object> {"));

		assertThat(code, containsOne("Inner<Object> inner = new Inner<>();"));
		assertThat(code, containsOne("inner.overload((IList<? super Object>) new ObjIList());"));
	}

	@NotYetImplemented
	@Test
	public void testOmitCast() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("inner.overload(new ObjIList());"));
	}
}
