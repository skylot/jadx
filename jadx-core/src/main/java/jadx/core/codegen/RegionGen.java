package jadx.core.codegen;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.DeclareVariablesAttr;
import jadx.core.dex.attributes.nodes.ForceReturnAttr;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.SwitchNode;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.IBlock;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.regions.IfCondition;
import jadx.core.dex.regions.IfRegion;
import jadx.core.dex.regions.LoopRegion;
import jadx.core.dex.regions.Region;
import jadx.core.dex.regions.SwitchRegion;
import jadx.core.dex.regions.SynchronizedRegion;
import jadx.core.dex.trycatch.CatchAttr;
import jadx.core.dex.trycatch.ExceptionHandler;
import jadx.core.dex.trycatch.TryCatchBlock;
import jadx.core.utils.RegionUtils;
import jadx.core.utils.exceptions.CodegenException;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegionGen extends InsnGen {
	private static final Logger LOG = LoggerFactory.getLogger(RegionGen.class);

	public RegionGen(MethodGen mgen) {
		super(mgen, false);
	}

	public void makeRegion(CodeWriter code, IContainer cont) throws CodegenException {
		if (cont instanceof IBlock) {
			makeSimpleBlock((IBlock) cont, code);
		} else if (cont instanceof IRegion) {
			if (cont instanceof Region) {
				makeSimpleRegion(code, (Region) cont);
			} else {
				declareVars(code, cont);
				if (cont instanceof IfRegion) {
					makeIf((IfRegion) cont, code, true);
				} else if (cont instanceof SwitchRegion) {
					makeSwitch((SwitchRegion) cont, code);
				} else if (cont instanceof LoopRegion) {
					makeLoop((LoopRegion) cont, code);
				} else if (cont instanceof SynchronizedRegion) {
					makeSynchronizedRegion((SynchronizedRegion) cont, code);
				}
			}
		} else {
			throw new CodegenException("Not processed container: " + cont);
		}
	}

	private void declareVars(CodeWriter code, IContainer cont) {
		DeclareVariablesAttr declVars = cont.get(AType.DECLARE_VARIABLES);
		if (declVars != null) {
			for (RegisterArg v : declVars.getVars()) {
				code.startLine();
				declareVar(code, v);
				code.add(';');
			}
		}
	}

	private void makeSimpleRegion(CodeWriter code, Region region) throws CodegenException {
		CatchAttr tc = region.get(AType.CATCH_BLOCK);
		if (tc != null) {
			makeTryCatch(region, tc.getTryBlock(), code);
		} else {
			declareVars(code, region);
			for (IContainer c : region.getSubBlocks()) {
				makeRegion(code, c);
			}
		}
	}

	public void makeRegionIndent(CodeWriter code, IContainer region) throws CodegenException {
		code.incIndent();
		makeRegion(code, region);
		code.decIndent();
	}

	private void makeSimpleBlock(IBlock block, CodeWriter code) throws CodegenException {
		for (InsnNode insn : block.getInstructions()) {
			makeInsn(insn, code);
		}
		ForceReturnAttr retAttr = block.get(AType.FORCE_RETURN);
		if (retAttr != null) {
			makeInsn(retAttr.getReturnInsn(), code);
		}
	}

	private void makeIf(IfRegion region, CodeWriter code, boolean newLine) throws CodegenException {
		if (region.getTernRegion() != null) {
			makeSimpleBlock(region.getTernRegion().getBlock(), code);
			return;
		}
		if (newLine) {
			code.startLine();
		}
		code.add("if (");
		new ConditionGen(this).add(code, region.getCondition());
		code.add(") {");
		makeRegionIndent(code, region.getThenRegion());
		code.startLine('}');

		IContainer els = region.getElseRegion();
		if (els != null && RegionUtils.notEmpty(els)) {
			code.add(" else ");
			if (connectElseIf(code, els)) {
				return;
			}
			code.add('{');
			makeRegionIndent(code, els);
			code.startLine('}');
		}
	}

	/**
	 * Connect if-else-if block
	 */
	private boolean connectElseIf(CodeWriter code, IContainer els) throws CodegenException {
		if (!els.contains(AFlag.ELSE_IF_CHAIN)) {
			return false;
		}
		if (!(els instanceof Region)) {
			return false;
		}
		List<IContainer> subBlocks = ((Region) els).getSubBlocks();
		if (subBlocks.size() == 1
				&& subBlocks.get(0) instanceof IfRegion) {
			makeIf((IfRegion) subBlocks.get(0), code, false);
			return true;
		}
		return false;
	}

	private CodeWriter makeLoop(LoopRegion region, CodeWriter code) throws CodegenException {
		BlockNode header = region.getHeader();
		if (header != null) {
			List<InsnNode> headerInsns = header.getInstructions();
			if (headerInsns.size() > 1) {
				// write not inlined instructions from header
				mth.add(AFlag.INCONSISTENT_CODE);
				int last = headerInsns.size() - 1;
				for (int i = 0; i < last; i++) {
					InsnNode insn = headerInsns.get(i);
					makeInsn(insn, code);
				}
			}
		}

		IfCondition condition = region.getCondition();
		if (condition == null) {
			// infinite loop
			code.startLine("while (true) {");
			makeRegionIndent(code, region.getBody());
			code.startLine('}');
			return code;
		}

		ConditionGen conditionGen = new ConditionGen(this);
		if (region.isConditionAtEnd()) {
			code.startLine("do {");
			makeRegionIndent(code, region.getBody());
			code.startLine("} while (");
			conditionGen.add(code, condition);
			code.add(");");
		} else {
			code.startLine("while (");
			conditionGen.add(code, condition);
			code.add(") {");
			makeRegionIndent(code, region.getBody());
			code.startLine('}');
		}
		return code;
	}

	private void makeSynchronizedRegion(SynchronizedRegion cont, CodeWriter code) throws CodegenException {
		code.startLine("synchronized (");
		addArg(code, cont.getEnterInsn().getArg(0));
		code.add(") {");
		makeRegionIndent(code, cont.getRegion());
		code.startLine('}');
	}

	private CodeWriter makeSwitch(SwitchRegion sw, CodeWriter code) throws CodegenException {
		SwitchNode insn = (SwitchNode) sw.getHeader().getInstructions().get(0);
		InsnArg arg = insn.getArg(0);
		code.startLine("switch (");
		addArg(code, arg);
		code.add(") {");
		code.incIndent();

		int size = sw.getKeys().size();
		for (int i = 0; i < size; i++) {
			List<Object> keys = sw.getKeys().get(i);
			IContainer c = sw.getCases().get(i);
			for (Object k : keys) {
				code.startLine("case ");
				if (k instanceof IndexInsnNode) {
					code.add(staticField((FieldInfo) ((IndexInsnNode) k).getIndex()));
				} else {
					code.add(TypeGen.literalToString((Integer) k, arg.getType()));
				}
				code.add(':');
			}
			makeCaseBlock(c, code);
		}
		if (sw.getDefaultCase() != null) {
			code.startLine("default:");
			makeCaseBlock(sw.getDefaultCase(), code);
		}
		code.decIndent();
		code.startLine('}');
		return code;
	}

	private void makeCaseBlock(IContainer c, CodeWriter code) throws CodegenException {
		boolean addBreak = true;
		if (RegionUtils.notEmpty(c)) {
			makeRegionIndent(code, c);
			if (!RegionUtils.hasExitEdge(c)) {
				addBreak = false;
			}
		}
		if (addBreak) {
			code.startLine().addIndent().add("break;");
		}
	}

	private void makeTryCatch(IContainer region, TryCatchBlock tryCatchBlock, CodeWriter code)
			throws CodegenException {
		code.startLine("try {");
		region.remove(AType.CATCH_BLOCK);
		makeRegionIndent(code, region);
		ExceptionHandler allHandler = null;
		for (ExceptionHandler handler : tryCatchBlock.getHandlers()) {
			if (!handler.isCatchAll()) {
				makeCatchBlock(code, handler);
			} else {
				if (allHandler != null) {
					LOG.warn("Several 'all' handlers in try/catch block in " + mth);
				}
				allHandler = handler;
			}
		}
		if (allHandler != null) {
			makeCatchBlock(code, allHandler);
		}
		if (tryCatchBlock.getFinalBlock() != null) {
			code.startLine("} finally {");
			makeRegionIndent(code, tryCatchBlock.getFinalBlock());
		}
		code.startLine('}');
	}

	private void makeCatchBlock(CodeWriter code, ExceptionHandler handler)
			throws CodegenException {
		IContainer region = handler.getHandlerRegion();
		if (region != null) {
			code.startLine("} catch (");
			code.add(handler.isCatchAll() ? "Throwable" : useClass(handler.getCatchType()));
			code.add(' ');
			code.add(mgen.assignNamedArg(handler.getArg()));
			code.add(") {");
			makeRegionIndent(code, region);
		}
	}
}
