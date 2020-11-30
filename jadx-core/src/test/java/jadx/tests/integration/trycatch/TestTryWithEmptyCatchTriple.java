package jadx.tests.integration.trycatch;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestTryWithEmptyCatchTriple extends SmaliTest {
	@Test
	public void test() {
		ClassNode cls = getClassNodeFromSmaliWithPkg("trycatch", "TestTryWithEmptyCatchTriple");
		String code = cls.getCode().toString();

		assertThat(code, containsOne("} catch (Error unused) {"));
		assertThat(code, containsOne("} catch (Error unused2) {"));
		assertThat(code, containsOne("} catch (Error unused3) {"));
	}
}
