package jadx.core.dex.visitors;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.InvokeNode;
import jadx.core.dex.instructions.InvokeType;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.visitors.regions.variables.ProcessVariables;
import jadx.core.dex.visitors.shrink.CodeShrinkVisitor;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.exceptions.JadxException;

/**
 * Remove primitives boxing
 * i.e convert 'Integer.valueOf(1)' to '1'
 */
@JadxVisitor(
		name = "DeboxingVisitor",
		desc = "Remove primitives boxing",
		runBefore = {
				CodeShrinkVisitor.class,
				ProcessVariables.class
		}
)
public class DeboxingVisitor extends AbstractVisitor {

	private Set<MethodInfo> valueOfMths;

	@Override
	public void init(RootNode root) {
		valueOfMths = new HashSet<>();
		valueOfMths.add(valueOfMth(root, ArgType.INT, "java.lang.Integer"));
		valueOfMths.add(valueOfMth(root, ArgType.BOOLEAN, "java.lang.Boolean"));
		valueOfMths.add(valueOfMth(root, ArgType.BYTE, "java.lang.Byte"));
		valueOfMths.add(valueOfMth(root, ArgType.SHORT, "java.lang.Short"));
		valueOfMths.add(valueOfMth(root, ArgType.CHAR, "java.lang.Character"));
		valueOfMths.add(valueOfMth(root, ArgType.LONG, "java.lang.Long"));
	}

	private static MethodInfo valueOfMth(RootNode root, ArgType argType, String clsName) {
		ArgType boxType = ArgType.object(clsName);
		ClassInfo boxCls = ClassInfo.fromType(root, boxType);
		return MethodInfo.fromDetails(root, boxCls, "valueOf", Collections.singletonList(argType), boxType);
	}

	@Override
	public void visit(MethodNode mth) throws JadxException {
		if (mth.isNoCode()) {
			return;
		}
		boolean replaced = false;
		for (BlockNode blockNode : mth.getBasicBlocks()) {
			List<InsnNode> insnList = blockNode.getInstructions();
			int count = insnList.size();
			for (int i = 0; i < count; i++) {
				InsnNode insnNode = insnList.get(i);
				if (insnNode.getType() == InsnType.INVOKE) {
					InsnNode replaceInsn = checkForReplace(((InvokeNode) insnNode));
					if (replaceInsn != null) {
						BlockUtils.replaceInsn(mth, blockNode, i, replaceInsn);
						replaced = true;
					}
				}
			}
		}
		if (replaced) {
			ConstInlineVisitor.process(mth);
		}
	}

	private InsnNode checkForReplace(InvokeNode insnNode) {
		if (insnNode.getInvokeType() != InvokeType.STATIC
				|| insnNode.getResult() == null) {
			return null;
		}
		MethodInfo callMth = insnNode.getCallMth();
		if (valueOfMths.contains(callMth)) {
			RegisterArg resArg = insnNode.getResult();
			InsnArg arg = insnNode.getArg(0);
			if (arg.isLiteral()) {
				ArgType primitiveType = callMth.getArgumentsTypes().get(0);
				ArgType boxType = callMth.getReturnType();
				if (isNeedExplicitCast(resArg, primitiveType, boxType)) {
					arg.add(AFlag.EXPLICIT_PRIMITIVE_TYPE);
				}
				arg.setType(primitiveType);
				boolean forbidInline;
				if (canChangeTypeToPrimitive(resArg, boxType)) {
					resArg.setType(primitiveType);
					forbidInline = false;
				} else {
					forbidInline = true;
				}

				InsnNode constInsn = new InsnNode(InsnType.CONST, 1);
				constInsn.addArg(arg);
				constInsn.setResult(resArg);
				if (forbidInline) {
					constInsn.add(AFlag.DONT_INLINE);
				}
				return constInsn;
			}
		}
		return null;
	}

	private boolean isNeedExplicitCast(RegisterArg resArg, ArgType primitiveType, ArgType boxType) {
		if (primitiveType == ArgType.LONG) {
			return true;
		}
		if (primitiveType != ArgType.INT) {
			Set<ArgType> useTypes = collectUseTypes(resArg);
			useTypes.add(resArg.getType());
			useTypes.remove(boxType);
			useTypes.remove(primitiveType);
			return !useTypes.isEmpty();
		}
		return false;
	}

	private boolean canChangeTypeToPrimitive(RegisterArg arg, ArgType boxType) {
		for (SSAVar ssaVar : arg.getSVar().getCodeVar().getSsaVars()) {
			if (ssaVar.isTypeImmutable()) {
				return false;
			}
			InsnNode assignInsn = ssaVar.getAssignInsn();
			if (assignInsn == null) {
				// method arg
				return false;
			}
			InsnType assignInsnType = assignInsn.getType();
			if (assignInsnType == InsnType.CONST || assignInsnType == InsnType.MOVE) {
				if (assignInsn.getArg(0).getType().isObject()) {
					return false;
				}
			}
			ArgType initType = assignInsn.getResult().getInitType();
			if (initType.isObject() && !initType.equals(boxType)) {
				// some of related vars have another object type
				return false;
			}

			for (RegisterArg useArg : ssaVar.getUseList()) {
				InsnNode parentInsn = useArg.getParentInsn();
				if (parentInsn == null) {
					return false;
				}
				if (parentInsn.getType() == InsnType.INVOKE) {
					InvokeNode invokeNode = (InvokeNode) parentInsn;
					if (useArg.equals(invokeNode.getInstanceArg())) {
						return false;
					}
				}
			}
		}
		return true;
	}

	private Set<ArgType> collectUseTypes(RegisterArg arg) {
		Set<ArgType> types = new HashSet<>();
		for (RegisterArg useArg : arg.getSVar().getUseList()) {
			types.add(useArg.getType());
			types.add(useArg.getInitType());
		}
		return types;
	}
}
