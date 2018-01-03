package jadx.tests.integration.names;

import java.util.BitSet;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.ssa.LiveVarAnalysis;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.junit.Assert.assertThat;

public class TestNameAssign2 extends IntegrationTest {

	public static class TestCls {

		private static void test(MethodNode mth, int regNum, LiveVarAnalysis la) {
			List<BlockNode> blocks = mth.getBasicBlocks();
			int blocksCount = blocks.size();
			BitSet hasPhi = new BitSet(blocksCount);
			BitSet processed = new BitSet(blocksCount);
			Deque<BlockNode> workList = new LinkedList<>();

			BitSet assignBlocks = la.getAssignBlocks(regNum);
			for (int id = assignBlocks.nextSetBit(0); id >= 0; id = assignBlocks.nextSetBit(id + 1)) {
				processed.set(id);
				workList.add(blocks.get(id));
			}
			while (!workList.isEmpty()) {
				BlockNode block = workList.pop();
				BitSet domFrontier = block.getDomFrontier();
				for (int id = domFrontier.nextSetBit(0); id >= 0; id = domFrontier.nextSetBit(id + 1)) {
					if (!hasPhi.get(id) && la.isLive(id, regNum)) {
						BlockNode df = blocks.get(id);
						addPhi(df, regNum);
						hasPhi.set(id);
						if (!processed.get(id)) {
							processed.set(id);
							workList.add(df);
						}
					}
				}
			}
		}

		private static void addPhi(BlockNode df, int regNum) {
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		// TODO:
		assertThat(code, containsOne("int id;"));
	}
}
