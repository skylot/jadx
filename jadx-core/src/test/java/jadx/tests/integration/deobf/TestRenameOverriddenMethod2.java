package jadx.tests.integration.deobf;

import org.junit.jupiter.api.Test;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestRenameOverriddenMethod2 extends IntegrationTest {

	public static class TestCls {

		public interface I {
			int call();
		}

		public static class A implements I {
			@Override
			public int call() {
				return 1;
			}
		}

		public static class B implements I {
			@Override
			public int call() {
				return 2;
			}
		}
	}

	@Test
	public void test() {
		enableDeobfuscation();
		args.setDeobfuscationMinLength(100); // rename everything

		ClassNode cls = getClassNode(TestCls.class);
		assertThat(cls)
				.code()
				.countString(2, "@Override")
				.countString(3, "int mo0call()");

		assertThat(searchCls(cls.getInnerClasses(), "I")).isNotNull()
				.extracting(c -> c.searchMethodByShortName("call")).isNotNull()
				.extracting(m -> m.get(AType.METHOD_OVERRIDE)).isNotNull()
				.satisfies(ovrdAttr -> {
					assertThat(ovrdAttr.getRelatedMthNodes()).hasSize(3);
					assertThat(ovrdAttr.getOverrideList()).isEmpty();
				});
	}
}
