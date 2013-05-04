package jadx.codegen;

import jadx.dex.attributes.AttributeFlag;
import jadx.dex.attributes.AttributeType;
import jadx.dex.attributes.DeclareVariableAttr;
import jadx.dex.attributes.ForceReturnAttr;
import jadx.dex.attributes.IAttribute;
import jadx.dex.instructions.IfNode;
import jadx.dex.instructions.IfOp;
import jadx.dex.instructions.SwitchNode;
import jadx.dex.instructions.args.ArgType;
import jadx.dex.instructions.args.InsnArg;
import jadx.dex.instructions.args.PrimitiveType;
import jadx.dex.instructions.args.RegisterArg;
import jadx.dex.nodes.IBlock;
import jadx.dex.nodes.IContainer;
import jadx.dex.nodes.IRegion;
import jadx.dex.nodes.InsnNode;
import jadx.dex.nodes.MethodNode;
import jadx.dex.regions.IfRegion;
import jadx.dex.regions.LoopRegion;
import jadx.dex.regions.Region;
import jadx.dex.regions.SwitchRegion;
import jadx.dex.regions.SynchronizedRegion;
import jadx.dex.trycatch.CatchAttr;
import jadx.dex.trycatch.ExceptionHandler;
import jadx.dex.trycatch.TryCatchBlock;
import jadx.utils.ErrorsCounter;
import jadx.utils.RegionUtils;
import jadx.utils.exceptions.CodegenException;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegionGen extends InsnGen {
	private static final Logger LOG = LoggerFactory.getLogger(RegionGen.class);

	public RegionGen(MethodGen mgen, MethodNode mth) {
		super(mgen, mth, false);
	}

	public void makeRegion(CodeWriter code, IContainer cont) throws CodegenException {
		assert cont != null;

		if (cont instanceof IBlock) {
			makeSimpleBlock((IBlock) cont, code);
		} else if (cont instanceof IRegion) {
			declareVars(code, cont);
			if (cont instanceof Region) {
				Region r = (Region) cont;
				CatchAttr tc = (CatchAttr) r.getAttributes().get(AttributeType.CATCH_BLOCK);
				if (tc != null) {
					makeTryCatch(cont, tc.getTryBlock(), code);
				} else {
					for (IContainer c : r.getSubBlocks())
						makeRegion(code, c);
				}
			} else if (cont instanceof IfRegion) {
				code.startLine();
				makeIf((IfRegion) cont, code);
			} else if (cont instanceof SwitchRegion) {
				makeSwitch((SwitchRegion) cont, code);
			} else if (cont instanceof LoopRegion) {
				makeLoop((LoopRegion) cont, code);
			} else if (cont instanceof SynchronizedRegion) {
				makeSynchronizedRegion((SynchronizedRegion) cont, code);
			}
		} else {
			throw new CodegenException("Not processed container: " + cont.toString());
		}
	}

	private void declareVars(CodeWriter code, IContainer cont) throws CodegenException {
		DeclareVariableAttr declVars =
				(DeclareVariableAttr) cont.getAttributes().get(AttributeType.DECLARE_VARIABLE);
		if (declVars != null) {
			for (RegisterArg v : declVars.getVars()) {
				code.startLine(declareVar(v)).add(';');
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
		if (block.getAttributes().contains(AttributeFlag.BREAK)) {
			code.startLine("break;");
		} else {
			IAttribute attr;
			if ((attr = block.getAttributes().get(AttributeType.FORCE_RETURN)) != null) {
				ForceReturnAttr retAttr = (ForceReturnAttr) attr;
				makeInsn(retAttr.getReturnInsn(), code);
			}
		}
	}

	private void makeIf(IfRegion region, CodeWriter code) throws CodegenException {
		IfNode insn = region.getIfInsn();
		code.add("if ").add(makeCondition(insn)).add(" {");
		makeRegionIndent(code, region.getThenRegion());
		code.startLine('}');

		IContainer els = region.getElseRegion();
		if (els != null && RegionUtils.notEmpty(els)) {
			code.add(" else ");

			// connect if-else-if block
			if (els instanceof Region) {
				Region re = (Region) els;
				if (re.getSubBlocks().size() == 1
						&& re.getSubBlocks().get(0) instanceof IfRegion) {
					makeIf((IfRegion) re.getSubBlocks().get(0), code);
					return;
				}
			}

			code.add('{');
			makeRegionIndent(code, els);
			code.startLine('}');
		}
	}

	private CodeWriter makeLoop(LoopRegion region, CodeWriter code) throws CodegenException {
		if (region.getConditionBlock() == null) {
			// infinite loop
			code.startLine("while (true) {");
			makeRegionIndent(code, region.getBody());
			code.startLine('}');
			return code;
		}

		IfNode insn = region.getIfInsn();
		if (!region.isConditionAtEnd()) {
			code.startLine("while ").add(makeCondition(insn)).add(" {");
			makeRegionIndent(code, region.getBody());
			code.startLine('}');
		} else {
			code.startLine("do {");
			makeRegionIndent(code, region.getBody());
			code.startLine("} while ").add(makeCondition(insn)).add(';');
		}
		return code;
	}

	private void makeSynchronizedRegion(SynchronizedRegion cont, CodeWriter code) throws CodegenException {
		code.startLine("synchronized(").add(arg(cont.getArg())).add(") {");
		makeRegionIndent(code, cont.getRegion());
		code.startLine('}');
	}

	private String makeCondition(IfNode insn) throws CodegenException {
		String second;
		IfOp op = insn.getOp();
		if (insn.isZeroCmp()) {
			ArgType type = insn.getArg(0).getType();
			if (type.getPrimitiveType() == PrimitiveType.BOOLEAN) {
				if (op == IfOp.EQ) {
					// == false
					return "(!" + arg(insn.getArg(0)) + ")";
				} else if (op == IfOp.NE) {
					// == true
					return "(" + arg(insn.getArg(0)) + ")";
				}
				LOG.warn(ErrorsCounter.formatErrorMsg(mth, "Unsupported boolean condition " + op.getSymbol()));
			}
			second = arg(InsnArg.lit(0, type));
		} else {
			second = arg(insn.getArg(1));
		}
		return "(" + arg(insn.getArg(0)) + " "
				+ op.getSymbol() + " "
				+ second + ")";
	}

	private CodeWriter makeSwitch(SwitchRegion sw, CodeWriter code) throws CodegenException {
		SwitchNode insn = (SwitchNode) sw.getHeader().getInstructions().get(0);
		InsnArg arg = insn.getArg(0);
		code.startLine("switch(").add(arg(arg)).add(") {");
		code.incIndent();

		int size = sw.getKeys().size();
		for (int i = 0; i < size; i++) {
			List<Integer> keys = sw.getKeys().get(i);
			IContainer c = sw.getCases().get(i);
			for (Integer k : keys) {
				code.startLine("case ");
				code.add(TypeGen.literalToString(k, arg.getType()));
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
		if (RegionUtils.notEmpty(c)) {
			code.add(" {");
			makeRegionIndent(code, c);
			if (RegionUtils.hasExitEdge(c)) {
				code.startLine(1, "break;");
			}
			code.startLine('}');
		} else {
			code.startLine(1, "break;");
		}
	}

	private void makeTryCatch(IContainer region, TryCatchBlock tryCatchBlock, CodeWriter code)
			throws CodegenException {
		code.startLine("try {");
		region.getAttributes().remove(AttributeType.CATCH_BLOCK);
		makeRegionIndent(code, region);
		ExceptionHandler allHandler = null;
		for (ExceptionHandler handler : tryCatchBlock.getHandlers()) {
			if (!handler.isCatchAll()) {
				makeCatchBlock(code, handler);
			} else {
				if (allHandler != null)
					LOG.warn("Several 'all' handlers in try/catch block in " + mth);
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
		if (region != null /* && RegionUtils.notEmpty(region) */) {
			code.startLine("} catch (");
			code.add(handler.isCatchAll() ? "Throwable" : useClass(handler.getCatchType()));
			code.add(' ');
			code.add(mgen.assignArg(handler.getArg()));
			code.add(") {");
			makeRegionIndent(code, region);
		}
	}

}
