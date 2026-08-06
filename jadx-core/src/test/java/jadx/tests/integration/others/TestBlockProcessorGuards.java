package jadx.tests.integration.others;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.blocks.BlockProcessor;
import jadx.tests.api.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

public class TestBlockProcessorGuards extends IntegrationTest {

	public static class TestCls {
		public int test() {
			return 1;
		}
	}

	@Test
	public void testCheckForUnreachableBlocksRemovesOrphanBlock() throws Exception {
		disableCompilation();

		ClassNode cls = getClassNode(TestCls.class);
		MethodNode mth = getMethod(cls, "test");
		List<BlockNode> blocks = new ArrayList<>(mth.getBasicBlocks());
		BlockNode orphanBlock = new BlockNode(1000, blocks.size(), 0x100);
		blocks.add(orphanBlock);
		mth.setBasicBlocks(blocks);

		Method check = BlockProcessor.class.getDeclaredMethod("checkForUnreachableBlocks", MethodNode.class);
		check.setAccessible(true);
		check.invoke(null, mth);

		assertThat(mth.getBasicBlocks()).doesNotContain(orphanBlock);
		assertThat(orphanBlock.getPredecessors()).isEmpty();
		assertThat(orphanBlock.getSuccessors()).isEmpty();
	}
}
