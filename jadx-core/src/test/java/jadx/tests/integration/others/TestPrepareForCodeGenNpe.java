package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.CodeVar;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.PrepareForCodeGen;
import jadx.core.utils.InsnRemover;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestPrepareForCodeGenNpe extends SmaliTest {

	@Test
	public void testMoveWithNullResult() throws Exception {
		ClassNode cls = getClassNodeFromSmali();
		MethodNode mth = cls.searchMethodByShortName("test");
		assertThat(mth).isNotNull();

		BlockNode block = mth.getBasicBlocks().get(0);
		InsnNode moveInsn = new InsnNode(InsnType.MOVE, 1);
		moveInsn.addArg(InsnArg.reg(1, ArgType.INT));
		// result is null
		block.getInstructions().add(moveInsn);

		new PrepareForCodeGen().visit(mth);
		assertThat(block.getInstructions()).contains(moveInsn);
	}

	@Test
	public void testMoveWithNullSSAVar() throws Exception {
		ClassNode cls = getClassNodeFromSmali();
		MethodNode mth = cls.searchMethodByShortName("test");
		assertThat(mth).isNotNull();

		BlockNode block = mth.getBasicBlocks().get(0);
		InsnNode moveInsn = new InsnNode(InsnType.MOVE, 1);
		moveInsn.addArg(InsnArg.reg(1, ArgType.INT));
		moveInsn.setResult(InsnArg.reg(1, ArgType.INT)); // result has null sVar
		block.getInstructions().add(moveInsn);

		new PrepareForCodeGen().visit(mth);
		assertThat(block.getInstructions()).contains(moveInsn);
	}

	@Test
	public void testMoveWithUnboundResult() throws Exception {
		ClassNode cls = getClassNodeFromSmali();
		MethodNode mth = cls.searchMethodByShortName("test");
		assertThat(mth).isNotNull();

		BlockNode block = mth.getBasicBlocks().get(0);
		InsnNode moveInsn = new InsnNode(InsnType.MOVE, 1);
		RegisterArg res = InsnArg.reg(1, ArgType.INT);
		mth.makeNewSVar(res);
		moveInsn.addArg(InsnArg.reg(1, ArgType.INT));
		moveInsn.setResult(res);
		InsnRemover.unbindResult(mth, moveInsn); // sets result to null and cleans SSA
		block.getInstructions().add(moveInsn);

		new PrepareForCodeGen().visit(mth);
		assertThat(block.getInstructions()).contains(moveInsn);
	}

	@Test
	public void testRedundantMoveRemoved() throws Exception {
		ClassNode cls = getClassNodeFromSmali();
		MethodNode mth = cls.searchMethodByShortName("test");
		assertThat(mth).isNotNull();

		BlockNode block = mth.getBasicBlocks().get(0);
		InsnNode moveInsn = new InsnNode(InsnType.MOVE, 1);
		RegisterArg arg = InsnArg.reg(1, ArgType.INT);
		RegisterArg res = InsnArg.reg(1, ArgType.INT);
		CodeVar codeVar = new CodeVar();
		codeVar.setName("a");
		SSAVar resSVar = mth.makeNewSVar(res);
		SSAVar argSVar = mth.makeNewSVar(arg);
		resSVar.setCodeVar(codeVar);
		argSVar.setCodeVar(codeVar);
		moveInsn.addArg(arg);
		moveInsn.setResult(res);
		block.getInstructions().add(moveInsn);

		new PrepareForCodeGen().visit(mth);
		assertThat(block.getInstructions()).doesNotContain(moveInsn);
	}
}
