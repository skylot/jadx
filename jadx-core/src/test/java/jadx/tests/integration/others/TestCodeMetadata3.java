package jadx.tests.integration.others;

import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.api.ICodeInfo;
import jadx.api.JavaClass;
import jadx.api.JavaVariable;
import jadx.api.metadata.annotations.VarNode;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

/**
 * Test variables refs in code metadata
 */
public class TestCodeMetadata3 extends IntegrationTest {

	public static class TestCls {
		public String test(String str) {
			int k = str.length();
			k++;
			return str + ':' + k;
		}
	}

	@Test
	public void test() {
		disableCompilation();
		ClassNode cls = getClassNode(TestCls.class);
		ICodeInfo codeInfo = cls.getCode();
		System.out.println(codeInfo.getCodeMetadata());

		MethodNode testMth = getMethod(cls, "test");
		JavaClass javaClass = toJavaClass(cls);
		List<VarNode> varNodes = testMth.collectArgNodes();
		assertThat(varNodes).hasSize(1);
		VarNode strVar = varNodes.get(0);
		JavaVariable strJavaVar = toJavaVariable(strVar);
		assertThat(strJavaVar.getName()).isEqualTo("str");

		List<Integer> strUsePlaces = javaClass.getUsePlacesFor(codeInfo, strJavaVar);
		assertThat(strUsePlaces).hasSize(2);
		assertThat(codeInfo).code().countString(3, "str");
	}
}
