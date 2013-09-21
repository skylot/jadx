package jadx.core.codegen;

import jadx.core.dex.attributes.AttributeFlag;
import jadx.core.dex.attributes.AttributeType;
import jadx.core.dex.attributes.DeclareVariableAttr;
import jadx.core.dex.attributes.ForceReturnAttr;
import jadx.core.dex.attributes.IAttribute;
import jadx.core.dex.instructions.ArithNode;
import jadx.core.dex.instructions.IfOp;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.SwitchNode;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.IBlock;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.IfCondition;
import jadx.core.dex.regions.IfRegion;
import jadx.core.dex.regions.LoopRegion;
import jadx.core.dex.regions.Region;
import jadx.core.dex.regions.SwitchRegion;
import jadx.core.dex.regions.SynchronizedRegion;
import jadx.core.dex.trycatch.CatchAttr;
import jadx.core.dex.trycatch.ExceptionHandler;
import jadx.core.dex.trycatch.TryCatchBlock;
import jadx.core.utils.ErrorsCounter;
import jadx.core.utils.RegionUtils;
import jadx.core.utils.exceptions.CodegenException;

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

	private void declareVars(CodeWriter code, IContainer cont) {
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
		code.add("if (").add(makeCondition(region.getCondition())).add(") {");
		makeRegionIndent(code, region.getThenRegion());
		code.startLine('}');

		IContainer els = region.getElseRegion();
		if (els != null && RegionUtils.notEmpty(els)) {
			code.add(" else ");

			// connect if-else-if block
			if (els instanceof Region) {
				Region re = (Region) els;
				List<IContainer> subBlocks = re.getSubBlocks();
				if (subBlocks.size() == 1 && subBlocks.get(0) instanceof IfRegion) {
					makeIf((IfRegion) subBlocks.get(0), code);
					return;
				}
			}

			code.add('{');
			makeRegionIndent(code, els);
			code.startLine('}');
		}
	}

	private CodeWriter makeLoop(LoopRegion region, CodeWriter code) throws CodegenException {
		IfCondition condition = region.getCondition();
		if (condition == null) {
			// infinite loop
			code.startLine("while (true) {");
			makeRegionIndent(code, region.getBody());
			code.startLine('}');
			return code;
		}

		if (region.isConditionAtEnd()) {
			code.startLine("do {");
			makeRegionIndent(code, region.getBody());
			code.startLine("} while (").add(makeCondition(condition)).add(");");
		} else {
			code.startLine("while (").add(makeCondition(condition)).add(") {");
			makeRegionIndent(code, region.getBody());
			code.startLine('}');
		}
		return code;
	}

	private void makeSynchronizedRegion(SynchronizedRegion cont, CodeWriter code) throws CodegenException {
		code.startLine("synchronized(").add(arg(cont.getInsn().getArg(0))).add(") {");
		makeRegionIndent(code, cont.getRegion());
		code.startLine('}');
	}

	private String makeCondition(IfCondition condition) throws CodegenException {
		switch (condition.getMode()) {
			case COMPARE:
				return makeCompare(condition.getCompare());
			case NOT:
				return "!" + makeCondition(condition.getArgs().get(0));
			case AND:
			case OR:
				String mode = condition.getMode() == IfCondition.MODE.AND ? " && " : " || ";
				StringBuilder sb = new StringBuilder();
				for (IfCondition arg : condition.getArgs()) {
					if (sb.length() != 0) {
						sb.append(mode);
					}
					sb.append('(').append(makeCondition(arg)).append(')');
				}
				return sb.toString();
			default:
				return "??" + condition.toString();
		}
	}

	private String makeCompare(IfCondition.Compare compare) throws CodegenException {
		IfOp op = compare.getOp();
		InsnArg firstArg = compare.getA();
		InsnArg secondArg = compare.getB();
		if (firstArg.getType().equals(ArgType.BOOLEAN)
				&& secondArg.isLiteral()
				&& secondArg.getType().equals(ArgType.BOOLEAN)) {
			LiteralArg lit = (LiteralArg) secondArg;
			if (lit.getLiteral() == 0) {
				op = op.invert();
			}
			if (op == IfOp.EQ) {
				return arg(firstArg, false); // == true
			} else if (op == IfOp.NE) {
				return "!" + arg(firstArg); // != true
			}
			LOG.warn(ErrorsCounter.formatErrorMsg(mth, "Unsupported boolean condition " + op.getSymbol()));
		}
		return arg(firstArg, isWrapNeeded(firstArg))
				+ " " + op.getSymbol() + " "
				+ arg(secondArg, isWrapNeeded(secondArg));
	}

	private boolean isWrapNeeded(InsnArg arg) {
		if (!arg.isInsnWrap()) {
			return false;
		}
		InsnNode insn = ((InsnWrapArg) arg).getWrapInsn();
		if(insn.getType() == InsnType.ARITH) {
			ArithNode arith = ((ArithNode) insn);
			switch (arith.getOp()) {
				case ADD:
				case SUB:
				case MUL:
				case DIV:
				case REM:
					return false;
			}
		}
		return true;
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
		code.add(" {");
		if (RegionUtils.notEmpty(c)) {
			makeRegionIndent(code, c);
			if (RegionUtils.hasExitEdge(c)) {
				code.startLine(1, "break;");
			}
		} else {
			code.startLine(1, "break;");
		}
		code.startLine('}');
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
			code.add(mgen.assignNamedArg(handler.getArg()));
			code.add(") {");
			makeRegionIndent(code, region);
		}
	}

}
