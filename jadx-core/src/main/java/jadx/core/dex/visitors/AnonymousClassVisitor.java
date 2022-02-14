package jadx.core.dex.visitors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.FieldReplaceAttr;
import jadx.core.dex.attributes.nodes.SkipMethodArgsAttr;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.instructions.mods.ConstructorInsn;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.shrink.CodeShrinkVisitor;
import jadx.core.utils.exceptions.JadxException;

@JadxVisitor(
		name = "AnonymousClassVisitor",
		desc = "Prepare anonymous class for inline",
		runBefore = {
				ModVisitor.class,
				CodeShrinkVisitor.class
		}
)
public class AnonymousClassVisitor extends AbstractVisitor {

	@Override
	public boolean visit(ClassNode cls) throws JadxException {
		if (cls.contains(AType.ANONYMOUS_CLASS)) {
			for (MethodNode mth : cls.getMethods()) {
				if (mth.contains(AFlag.ANONYMOUS_CONSTRUCTOR)) {
					processAnonymousConstructor(mth);
					break;
				}
			}
		}
		return true;
	}

	private static void processAnonymousConstructor(MethodNode mth) {
		List<InsnNode> usedInsns = new ArrayList<>();
		Map<InsnArg, FieldNode> argsMap = getArgsToFieldsMapping(mth, usedInsns);
		if (argsMap.isEmpty()) {
			mth.add(AFlag.NO_SKIP_ARGS);
		} else {
			for (Map.Entry<InsnArg, FieldNode> entry : argsMap.entrySet()) {
				FieldNode field = entry.getValue();
				if (field == null) {
					continue;
				}
				InsnArg arg = entry.getKey();
				field.addAttr(new FieldReplaceAttr(arg));
				field.add(AFlag.DONT_GENERATE);
				if (arg.isRegister()) {
					arg.add(AFlag.SKIP_ARG);
					SkipMethodArgsAttr.skipArg(mth, ((RegisterArg) arg));
				}
			}
		}
		for (InsnNode usedInsn : usedInsns) {
			usedInsn.add(AFlag.DONT_GENERATE);
		}
	}

	private static Map<InsnArg, FieldNode> getArgsToFieldsMapping(MethodNode mth, List<InsnNode> usedInsns) {
		MethodInfo callMth = mth.getMethodInfo();
		ClassNode cls = mth.getParentClass();
		List<RegisterArg> argList = mth.getArgRegs();
		ClassNode outerCls = mth.getUseIn().get(0).getParentClass();
		int startArg = 0;
		if (callMth.getArgsCount() != 0 && callMth.getArgumentsTypes().get(0).equals(outerCls.getClassInfo().getType())) {
			startArg = 1;
		}
		Map<InsnArg, FieldNode> map = new LinkedHashMap<>();
		int argsCount = argList.size();
		for (int i = startArg; i < argsCount; i++) {
			RegisterArg arg = argList.get(i);
			InsnNode useInsn = getParentInsnSkipMove(arg);
			if (useInsn == null) {
				return Collections.emptyMap();
			}
			switch (useInsn.getType()) {
				case IPUT:
					FieldNode fieldNode = cls.searchField((FieldInfo) ((IndexInsnNode) useInsn).getIndex());
					if (fieldNode == null || !fieldNode.getAccessFlags().isSynthetic()) {
						return Collections.emptyMap();
					}
					map.put(arg, fieldNode);
					usedInsns.add(useInsn);
					break;

				case CONSTRUCTOR:
					ConstructorInsn superConstr = (ConstructorInsn) useInsn;
					if (!superConstr.isSuper()) {
						return Collections.emptyMap();
					}
					usedInsns.add(useInsn);
					break;

				default:
					return Collections.emptyMap();
			}
		}
		return map;
	}

	private static InsnNode getParentInsnSkipMove(RegisterArg arg) {
		SSAVar sVar = arg.getSVar();
		if (sVar.getUseCount() != 1) {
			return null;
		}
		RegisterArg useArg = sVar.getUseList().get(0);
		InsnNode parentInsn = useArg.getParentInsn();
		if (parentInsn == null) {
			return null;
		}
		if (parentInsn.getType() == InsnType.MOVE) {
			return getParentInsnSkipMove(parentInsn.getResult());
		}
		return parentInsn;
	}
}
