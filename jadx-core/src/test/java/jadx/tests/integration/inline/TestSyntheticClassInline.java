package jadx.tests.integration.inline;

import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestSyntheticClassInline extends SmaliTest {

	@Test
	public void test() {
		List<ClassNode> classes = loadFromSmaliFiles();
		assertThat(searchCls(classes, "inline.A"))
				.code()
				.containsOne("static Supplier<Long> test(final long x1, final long x2) {")
				.containsOne("return new Supplier() {")
				.containsOne("return A.lambda$test$0(x1, x2);");
	}
}
