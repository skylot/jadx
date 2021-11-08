package jadx.tests.integration.generics;

import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestSyntheticOverride extends SmaliTest {
	// @formatter:off
	/*
		final class TestSyntheticOverride extends Lambda implements Function1<String, Unit> {

			// fixing method types to match interface (i.e Unit invoke(String str))
			// make duplicate methods signatures
			public bridge synthetic Object invoke(Object str) {
				invoke(str);
				return Unit.INSTANCE;
			}

			public final void invoke(String str) {
				...
			}
		}
	*/
	// @formatter:on

	@Test
	public void test() {
		allowWarnInCode();
		disableCompilation();
		List<ClassNode> classNodes = loadFromSmaliFiles();
		assertThat(searchCls(classNodes, "TestSyntheticOverride"))
				.code()
				.containsOne("invoke(String str)")
				.containsOne("invoke2(String str)");
	}
}
