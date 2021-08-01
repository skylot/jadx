package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestJavaDupInsn extends IntegrationTest {

	public static class TestCls {
		private MethodNode mth;
		private BlockNode block;
		private SSAVar[] vars;
		private int[] versions;

		public SSAVar test(RegisterArg regArg) {
			int regNum = regArg.getRegNum();
			int version = versions[regNum]++;
			SSAVar ssaVar = mth.makeNewSVar(regNum, version, regArg);
			vars[regNum] = ssaVar;
			return ssaVar;
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code();
	}
}
