package jadx.core.codegen;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.DeclareVariablesAttr;
import jadx.core.dex.attributes.nodes.ForceReturnAttr;
import jadx.core.dex.attributes.nodes.LoopLabelAttr;
import jadx.core.dex.instructions.SwitchNode;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.NamedArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.IBlock;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.parser.FieldInitAttr;
import jadx.core.dex.regions.Region;
import jadx.core.dex.regions.SwitchRegion;
import jadx.core.dex.regions.SynchronizedRegion;
import jadx.core.dex.regions.TryCatchRegion;
import jadx.core.dex.regions.conditions.IfCondition;
import jadx.core.dex.regions.conditions.IfRegion;
import jadx.core.dex.regions.loops.ForEachLoop;
import jadx.core.dex.regions.loops.ForLoop;
import jadx.core.dex.regions.loops.LoopRegion;
import jadx.core.dex.regions.loops.LoopType;
import jadx.core.dex.trycatch.ExceptionHandler;
import jadx.core.utils.ErrorsCounter;
import jadx.core.utils.RegionUtils;
import jadx.core.utils.exceptions.CodegenException;
import jadx.core.utils.exceptions.JadxRuntimeException;

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
				} else if (cont instanceof TryCatchRegion) {
					makeTryCatch((TryCatchRegion) cont, code);
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
		declareVars(code, region);
		for (IContainer c : region.getSubBlocks()) {
			makeRegion(code, c);
		}
	}

	public void makeRegionIndent(CodeWriter code, IContainer region) throws CodegenException {
		code.incIndent();
		makeRegion(code, region);
		code.decIndent();
	}

	private void makeSimpleBlock(IBlock block, CodeWriter code) throws CodegenException {
		for (InsnNode insn : block.getInstructions()) {
			if (!insn.contains(AFlag.SKIP)) {
				makeInsn(insn, code);
			}
		}
		ForceReturnAttr retAttr = block.get(AType.FORCE_RETURN);
		if (retAttr != null) {
			makeInsn(retAttr.getReturnInsn(), code);
		}
	}

	private void makeIf(IfRegion region, CodeWriter code, boolean newLine) throws CodegenException {
		if (newLine) {
			code.startLineWithNum(region.getSourceLine());
		} else {
			code.attachSourceLine(region.getSourceLine());
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
		if (els.contains(AFlag.ELSE_IF_CHAIN) && els instanceof Region) {
			List<IContainer> subBlocks = ((Region) els).getSubBlocks();
			if (subBlocks.size() == 1) {
				IContainer elseBlock = subBlocks.get(0);
				if (elseBlock instanceof IfRegion) {
					declareVars(code, elseBlock);
					makeIf((IfRegion) elseBlock, code, false);
					return true;
				}
			}
		}
		return false;
	}

	private CodeWriter makeLoop(LoopRegion region, CodeWriter code) throws CodegenException {
		BlockNode header = region.getHeader();
		if (header != null) {
			List<InsnNode> headerInsns = header.getInstructions();
			if (headerInsns.size() > 1) {
				ErrorsCounter.methodError(mth, "Found not inlined instructions from loop header");
				int last = headerInsns.size() - 1;
				for (int i = 0; i < last; i++) {
					InsnNode insn = headerInsns.get(i);
					makeInsn(insn, code);
				}
			}
		}
		LoopLabelAttr labelAttr = region.getInfo().getStart().get(AType.LOOP_LABEL);
		if (labelAttr != null) {
			code.startLine(mgen.getNameGen().getLoopLabel(labelAttr)).add(':');
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
		LoopType type = region.getType();
		if (type != null) {
			if (type instanceof ForLoop) {
				ForLoop forLoop = (ForLoop) type;
				code.startLine("for (");
				makeInsn(forLoop.getInitInsn(), code, Flags.INLINE);
				code.add("; ");
				conditionGen.add(code, condition);
				code.add("; ");
				makeInsn(forLoop.getIncrInsn(), code, Flags.INLINE);
				code.add(") {");
				makeRegionIndent(code, region.getBody());
				code.startLine('}');
				return code;
			}
			if (type instanceof ForEachLoop) {
				ForEachLoop forEachLoop = (ForEachLoop) type;
				code.startLine("for (");
				declareVar(code, forEachLoop.getVarArg());
				code.add(" : ");
				addArg(code, forEachLoop.getIterableArg(), false);
				code.add(") {");
				makeRegionIndent(code, region.getBody());
				code.startLine('}');
				return code;
			}
			throw new JadxRuntimeException("Unknown loop type: " + type.getClass());
		}
		if (region.isConditionAtEnd()) {
			code.startLine("do {");
			makeRegionIndent(code, region.getBody());
			code.startLineWithNum(region.getConditionSourceLine());
			code.add("} while (");
			conditionGen.add(code, condition);
			code.add(");");
		} else {
			code.startLineWithNum(region.getConditionSourceLine());
			code.add("while (");
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
		addArg(code, arg, false);
		code.add(") {");
		code.incIndent();

		int size = sw.getKeys().size();
		for (int i = 0; i < size; i++) {
			List<Object> keys = sw.getKeys().get(i);
			IContainer c = sw.getCases().get(i);
			for (Object k : keys) {
				code.startLine("case ");
				if (k instanceof FieldNode) {
					FieldNode fn = (FieldNode) k;
					if (fn.getParentClass().isEnum()) {
						code.add(fn.getAlias());
					} else {
						staticField(code, fn.getFieldInfo());
						// print original value, sometimes replace with incorrect field
						FieldInitAttr valueAttr = fn.get(AType.FIELD_INIT);
						if (valueAttr != null && valueAttr.getValue() != null) {
							code.add(" /*").add(valueAttr.getValue().toString()).add("*/");
						}
					}
				} else if (k instanceof Integer) {
					code.add(TypeGen.literalToString((Integer) k, arg.getType(), mth));
				} else {
					throw new JadxRuntimeException("Unexpected key in switch: " + (k != null ? k.getClass() : null));
				}
				code.add(':');
			}
			makeRegionIndent(code, c);
		}
		if (sw.getDefaultCase() != null) {
			code.startLine("default:");
			makeRegionIndent(code, sw.getDefaultCase());
		}
		code.decIndent();
		code.startLine('}');
		return code;
	}

	private void makeTryCatch(TryCatchRegion region, CodeWriter code) throws CodegenException {
		code.startLine("try {");
		makeRegionIndent(code, region.getTryRegion());
		// TODO: move search of 'allHandler' to 'TryCatchRegion'
		ExceptionHandler allHandler = null;
		for (Map.Entry<ExceptionHandler, IContainer> entry : region.getCatchRegions().entrySet()) {
			ExceptionHandler handler = entry.getKey();
			if (handler.isCatchAll()) {
				if (allHandler != null) {
					LOG.warn("Several 'all' handlers in try/catch block in {}", mth);
				}
				allHandler = handler;
			} else {
				makeCatchBlock(code, handler);
			}
		}
		if (allHandler != null) {
			makeCatchBlock(code, allHandler);
		}
		IContainer finallyRegion = region.getFinallyRegion();
		if (finallyRegion != null) {
			code.startLine("} finally {");
			makeRegionIndent(code, finallyRegion);
		}
		code.startLine('}');
	}

	private void makeCatchBlock(CodeWriter code, ExceptionHandler handler) throws CodegenException {
		IContainer region = handler.getHandlerRegion();
		if (region == null) {
			return;
		}
		code.startLine("} catch (");
		InsnArg arg = handler.getArg();
		if (arg instanceof RegisterArg) {
			declareVar(code, (RegisterArg) arg);
		} else if (arg instanceof NamedArg) {
			if (handler.isCatchAll()) {
				code.add("Throwable");
			} else {
				useClass(code, handler.getCatchType());
			}
			code.add(' ');
			code.add(mgen.getNameGen().assignNamedArg((NamedArg) arg));
		}
		code.add(") {");
		makeRegionIndent(code, region);
	}
}
