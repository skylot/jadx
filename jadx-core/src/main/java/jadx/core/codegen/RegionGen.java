package jadx.core.codegen;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.fldinit.FieldInitAttr;
import jadx.core.dex.attributes.nodes.DeclareVariablesAttr;
import jadx.core.dex.attributes.nodes.ForceReturnAttr;
import jadx.core.dex.attributes.nodes.LoopLabelAttr;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.instructions.SwitchInsn;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.CodeVar;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.NamedArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.*;
import jadx.core.dex.regions.Region;
import jadx.core.dex.regions.SwitchRegion;
import jadx.core.dex.regions.SwitchRegion.CaseInfo;
import jadx.core.dex.regions.SynchronizedRegion;
import jadx.core.dex.regions.TryCatchRegion;
import jadx.core.dex.regions.conditions.IfCondition;
import jadx.core.dex.regions.conditions.IfRegion;
import jadx.core.dex.regions.loops.ForEachLoop;
import jadx.core.dex.regions.loops.ForLoop;
import jadx.core.dex.regions.loops.LoopRegion;
import jadx.core.dex.regions.loops.LoopType;
import jadx.core.dex.trycatch.ExceptionHandler;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.CodeGenUtils;
import jadx.core.utils.RegionUtils;
import jadx.core.utils.exceptions.CodegenException;
import jadx.core.utils.exceptions.JadxRuntimeException;

import static jadx.core.dex.nodes.VariableNode.*;

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
			for (CodeVar v : declVars.getVars()) {
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
		if (block.contains(AFlag.DONT_GENERATE)) {
			return;
		}

		for (InsnNode insn : block.getInstructions()) {
			if (!insn.contains(AFlag.DONT_GENERATE)) {
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
		if (attachInsns) {
			List<BlockNode> conditionBlocks = region.getConditionBlocks();
			if (!conditionBlocks.isEmpty()) {
				BlockNode blockNode = conditionBlocks.get(0);
				InsnNode lastInsn = BlockUtils.getLastInsn(blockNode);
				if (lastInsn != null) {
					code.attachLineAnnotation(lastInsn);
				}
			}
		}
		boolean comment = region.contains(AFlag.COMMENT_OUT);
		if (comment) {
			code.add("// ");
		}

		code.add("if (");
		new ConditionGen(this).add(code, region.getCondition());
		code.add(") {");
		makeRegionIndent(code, region.getThenRegion());
		if (comment) {
			code.startLine("// }");
		} else {
			code.startLine('}');
		}

		IContainer els = region.getElseRegion();
		if (RegionUtils.notEmpty(els)) {
			code.add(" else ");
			if (connectElseIf(code, els)) {
				return;
			}
			code.add('{');
			makeRegionIndent(code, els);
			if (comment) {
				code.startLine("// }");
			} else {
				code.startLine('}');
			}
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
		SwitchInsn insn = (SwitchInsn) BlockUtils.getLastInsn(sw.getHeader());
		Objects.requireNonNull(insn, "Switch insn not found in header");
		InsnArg arg = insn.getArg(0);
		code.startLine("switch (");
		addArg(code, arg, false);
		code.add(") {");
		code.incIndent();

		for (CaseInfo caseInfo : sw.getCases()) {
			List<Object> keys = caseInfo.getKeys();
			IContainer c = caseInfo.getContainer();
			for (Object k : keys) {
				if (k == SwitchRegion.DEFAULT_CASE_KEY) {
					code.startLine("default:");
				} else {
					code.startLine("case ");
					addCaseKey(code, arg, k);
					code.add(':');
				}
			}
			makeRegionIndent(code, c);
		}
		code.decIndent();
		code.startLine('}');
		return code;
	}

	private void addCaseKey(CodeWriter code, InsnArg arg, Object k) {
		if (k instanceof FieldNode) {
			FieldNode fn = (FieldNode) k;
			if (fn.getParentClass().isEnum()) {
				code.add(fn.getAlias());
			} else {
				staticField(code, fn.getFieldInfo());
				// print original value, sometimes replaced with incorrect field
				FieldInitAttr valueAttr = fn.get(AType.FIELD_INIT);
				if (valueAttr != null && valueAttr.isConst()) {
					Object value = valueAttr.getEncodedValue().getValue();
					if (value != null) {
						code.add(" /* ").add(value.toString()).add(" */");
					}
				}
			}
		} else if (k instanceof Integer) {
			code.add(TypeGen.literalToString((Integer) k, arg.getType(), mth, fallback));
		} else {
			throw new JadxRuntimeException("Unexpected key in switch: " + (k != null ? k.getClass() : null));
		}
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
		if (handler.isCatchAll()) {
			useClass(code, ArgType.THROWABLE);
		} else {
			Iterator<ClassInfo> it = handler.getCatchTypes().iterator();
			if (it.hasNext()) {
				useClass(code, it.next());
			}
			while (it.hasNext()) {
				code.add(" | ");
				useClass(code, it.next());
			}
		}
		code.add(' ');
		InsnArg arg = handler.getArg();
		if (arg == null) {
			code.add("unknown"); // throwing exception is too late at this point
		} else if (arg instanceof RegisterArg) {
			String name;
			CodeVar codeVar = CodeGenUtils.getCodeVar((RegisterArg) arg);
			if (codeVar != null) {
				VariableNode node = mth.declareVar(codeVar, mgen.getNameGen(), VarKind.CATCH_ARG);
				if (node != null) {
					code.attachDefinition(node);
					name = node.getName();
					codeVar.setName(name);
				} else {
					name = mgen.getNameGen().assignArg(codeVar);
				}
			} else {
				RegisterArg reg = (RegisterArg) arg;
				name = mgen.getNameGen().assignArg(reg.getSVar().getCodeVar());
			}
			code.add(name);
		} else if (arg instanceof NamedArg) {
			VariableNode node = mth.declareVar((NamedArg) arg, mgen.getNameGen(), VarKind.CATCH_ARG);
			if (node != null) {
				code.add(node.getName());
			} else {
				code.add(mgen.getNameGen().assignNamedArg((NamedArg) arg));
			}
		} else {
			throw new JadxRuntimeException("Unexpected arg type in catch block: " + arg + ", class: " + arg.getClass().getSimpleName());
		}
		code.add(") {");
		makeRegionIndent(code, region);
	}
}
