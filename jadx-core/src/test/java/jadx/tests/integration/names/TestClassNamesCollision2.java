package jadx.tests.integration.names;

import org.junit.jupiter.api.Test;

import jadx.api.CommentsLevel;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestClassNamesCollision2 extends IntegrationTest {

	@SuppressWarnings("rawtypes")
	public static class TestCls {
		static class List {
			public static List getList() {
				return null;
			}
		}

		protected List list = List.getList();

		protected void clearList(java.util.List l) {
			l.clear();
		}
	}

	@Test
	public void test() {
		getArgs().setCommentsLevel(CommentsLevel.WARN);
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("static class List {")
				.containsOne("protected void clearList(java.util.List l) {");
	}
}
