package jadx.tests.integration.others;

import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

@SuppressWarnings("CommentedOutCode")
public class TestOverrideWithSameName extends SmaliTest {

	//@formatter:off
	/*
		interface A {
			B a();
			C a();
		}

		abstract class B implements A {
			@Override
			public C a() {
				return null;
			}
		}

		public class C extends B {
			@Override
			public B a() {
				return null;
			}
		}
	*/
	//@formatter:on

	@Test
	public void test() {
		List<ClassNode> clsNodes = loadFromSmaliFiles();
		assertThat(searchCls(clsNodes, "test.A"))
				.code()
				.containsOne("C mo0a();") // assume second method was renamed
				.doesNotContain("@Override");

		ClassNode bCls = searchCls(clsNodes, "test.B");
		assertThat(bCls)
				.code()
				.containsOne("C mo0a() {")
				.containsOne("@Override");

		assertThat(getMethod(bCls, "a").get(AType.METHOD_OVERRIDE).getOverrideList())
				.singleElement()
				.satisfies(mth -> assertThat(mth.getMethodInfo().getDeclClass().getShortName()).isEqualTo("A"));

		ClassNode cCls = searchCls(clsNodes, "test.C");
		assertThat(cCls)
				.code()
				.containsOne("B a() {")
				.containsOne("@Override");

		assertThat(getMethod(cCls, "a").get(AType.METHOD_OVERRIDE).getOverrideList())
				.singleElement()
				.satisfies(mth -> assertThat(mth.getMethodInfo().getDeclClass().getShortName()).isEqualTo("A"));
	}
}
