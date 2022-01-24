package jadx.tests.integration.variables;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import jadx.api.data.annotations.VarDeclareRef;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestVariablesDeclAnnotation extends IntegrationTest {

	public abstract static class TestCls {
		public int test(String str, int i) {
			return i;
		}

		public abstract int test2(String str);
	}

	@Test
	public void test() {
		noDebugInfo();
		ClassNode cls = getClassNode(TestCls.class);
		assertThat(cls).code()
				.containsOne("public int test(String str, int i) {")
				.containsOne("public abstract int test2(String str);");

		checkArgNamesInMethod(cls, "test", "[str, i]");
		checkArgNamesInMethod(cls, "test2", "[str]");
	}

	private static void checkArgNamesInMethod(ClassNode cls, String mthName, String expectedVars) {
		MethodNode testMth = cls.searchMethodByShortName(mthName);
		assertThat(testMth).isNotNull();

		int mthLine = testMth.getDecompiledLine();
		List<String> argNames = cls.getCode().getAnnotations().entrySet().stream()
				.filter(e -> e.getKey().getLine() == mthLine && e.getValue() instanceof VarDeclareRef)
				.sorted(Comparator.comparingInt(e -> e.getKey().getPos()))
				.map(e -> ((VarDeclareRef) e.getValue()).getName())
				.collect(Collectors.toList());

		assertThat(argNames).doesNotContainNull();
		assertThat(argNames.toString()).isEqualTo(expectedVars);
	}
}
