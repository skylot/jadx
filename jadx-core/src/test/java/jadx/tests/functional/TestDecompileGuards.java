package jadx.tests.functional;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.visitors.ModVisitor;
import jadx.core.dex.visitors.PrepareForCodeGen;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.Pair;
import jadx.core.utils.exceptions.JadxRuntimeException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestDecompileGuards {

	@Test
	public void testSearchTopSplitterForHandlerMissing() {
		BlockNode handler = new BlockNode(1, 1, 0);

		assertThat(BlockUtils.searchTopSplitterForHandler(handler)).isNull();
		assertThatThrownBy(() -> BlockUtils.getTopSplitterForHandler(handler))
				.isInstanceOf(JadxRuntimeException.class)
				.hasMessageContaining("Can't find top splitter block");
	}

	@Test
	public void testSearchTopSplitterForHandlerFound() {
		BlockNode splitter = new BlockNode(1, 1, 0);
		splitter.add(AFlag.EXC_TOP_SPLITTER);

		BlockNode handler = new BlockNode(2, 2, 0);
		handler.getPredecessors().add(splitter);

		assertThat(BlockUtils.searchTopSplitterForHandler(handler)).isSameAs(splitter);
		assertThat(BlockUtils.getTopSplitterForHandler(handler)).isSameAs(splitter);
	}

	@Test
	public void testPrepareForCodeGenRemovesMoveWithoutResult() throws Exception {
		BlockNode block = new BlockNode(1, 1, 0);
		InsnNode move = new InsnNode(InsnType.MOVE, 1);
		move.addArg(LiteralArg.make(0, ArgType.INT));
		block.getInstructions().add(move);

		Method removeInstructions = PrepareForCodeGen.class.getDeclaredMethod("removeInstructions", BlockNode.class);
		removeInstructions.setAccessible(true);
		removeInstructions.invoke(null, block);

		assertThat(block.getInstructions()).isEmpty();
	}

	@Test
	public void testPairAllowsNullValues() {
		Pair<BlockNode> pair = new Pair<>(new BlockNode(1, 1, 0), null);
		Pair<BlockNode> samePair = new Pair<>(pair.getFirst(), null);
		Pair<BlockNode> otherPair = new Pair<>(null, pair.getFirst());

		assertThat(pair).isEqualTo(samePair);
		assertThat(pair.hashCode()).isEqualTo(samePair.hashCode());
		assertThat(pair).isNotEqualTo(otherPair);
	}

	@Test
	public void testCheckCastResultTypeRejectsImmutableConflict() throws Exception {
		Method check = ModVisitor.class.getDeclaredMethod("canUpdateCastResultType", RegisterArg.class, ArgType.class);
		check.setAccessible(true);

		RegisterArg result = InsnArg.reg(0, ArgType.OBJECT);
		result.add(AFlag.IMMUTABLE_TYPE);
		new SSAVar(0, 0, result);

		assertThat(check.invoke(null, result, ArgType.OBJECT)).isEqualTo(true);
		assertThat(check.invoke(null, result, ArgType.STRING)).isEqualTo(false);
		assertThat(check.invoke(null, InsnArg.reg(1, ArgType.OBJECT), ArgType.OBJECT)).isEqualTo(false);
	}
}
