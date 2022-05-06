package jadx.tests.integration.variables;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.api.ICodeInfo;
import jadx.api.metadata.ICodeNodeRef;
import jadx.api.metadata.annotations.NodeDeclareRef;
import jadx.api.metadata.annotations.VarNode;
import jadx.api.utils.CodeUtils;
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

		ICodeInfo codeInfo = cls.getCode();
		int mthDefPos = testMth.getDefPosition();
		int lineEndPos = CodeUtils.getLineEndForPos(codeInfo.getCodeStr(), mthDefPos);
		List<String> argNames2 = new ArrayList<>();
		codeInfo.getCodeMetadata().searchDown(mthDefPos, (pos, ann) -> {
			if (pos > lineEndPos) {
				return Boolean.TRUE; // stop at line end
			}
			if (ann instanceof NodeDeclareRef) {
				ICodeNodeRef declRef = ((NodeDeclareRef) ann).getNode();
				if (declRef instanceof VarNode) {
					VarNode varNode = (VarNode) declRef;
					if (varNode.getMth().equals(testMth)) {
						argNames2.add(varNode.getName());
					}
				}
			}
			return null;
		});

		assertThat(argNames2).doesNotContainNull();
		assertThat(argNames2.toString()).isEqualTo(expectedVars);
	}
}
