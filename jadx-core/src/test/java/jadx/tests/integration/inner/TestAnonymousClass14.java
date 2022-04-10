package jadx.tests.integration.inner;

import org.junit.jupiter.api.Test;

import jadx.api.CommentsLevel;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.ListUtils;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestAnonymousClass14 extends SmaliTest {
	// @formatter:off
	/*
		public class OuterCls implements Runnable {

			class TestCls {
				private TestCls() {
					new ArrayList();
				}
			}

			public void makeAnonymousCls() {
				use(new Thread(this) {
				/ * class inner.OuterCls.AnonymousClass1 * /

					public void someMethod() {
					}
				});
			}

			public void makeTestCls() {
				new TestCls();
			}

			public void run() {
			}

			public void use(Thread thread) {
			}
		}
	 */
	// @formatter:on

	@Test
	public void test() {
		getArgs().setCommentsLevel(CommentsLevel.WARN);
		ClassNode outerCls = getClassNodeFromSmaliFiles("OuterCls");
		assertThat(outerCls).code()
				.doesNotContain("synthetic", "AnonymousClass1")
				.describedAs("only one constructor").containsOne("private TestCls(")
				.describedAs("constructor without args").containsOne("private TestCls() {");

		MethodNode makeTestClsMth = outerCls.searchMethodByShortName("makeTestCls");
		assertThat(makeTestClsMth).isNotNull();

		ClassNode testCls = searchCls(outerCls.getInnerClasses(), "TestCls");
		MethodNode ctrMth = ListUtils.filterOnlyOne(testCls.getMethods(),
				m -> m.isConstructor() && !m.getAccessFlags().isSynthetic());
		assertThat(ctrMth).isNotNull();
		assertThat(ctrMth.getUseIn()).hasSize(1);
		assertThat(ctrMth.getUseIn().get(0)).isEqualTo(makeTestClsMth);

		assertThat(outerCls).checkCodeAnnotationFor("new TestCls();", 4, ctrMth);
	}
}
