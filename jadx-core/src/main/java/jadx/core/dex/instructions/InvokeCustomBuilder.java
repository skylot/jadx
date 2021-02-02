package jadx.core.dex.instructions;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import jadx.api.plugins.input.data.ICallSite;
import jadx.api.plugins.input.data.IMethodHandle;
import jadx.api.plugins.input.data.IMethodProto;
import jadx.api.plugins.input.data.IMethodRef;
import jadx.api.plugins.input.data.MethodHandleType;
import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.api.plugins.input.insns.InsnData;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.NamedArg;
import jadx.core.dex.instructions.mods.ConstructorInsn;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class InvokeCustomBuilder {

	public static InsnNode build(MethodNode mth, InsnData insn, boolean isRange) {
		try {
			ICallSite callSite = insn.getIndexAsCallSite();
			callSite.load();
			List<EncodedValue> values = callSite.getValues();
			if (!checkLinkerMethod(values)) {
				throw new JadxRuntimeException("Failed to process invoke-custom instruction: " + callSite);
			}
			IMethodHandle callMthHandle = (IMethodHandle) values.get(4).getValue();
			if (callMthHandle.getType().isField()) {
				throw new JadxRuntimeException("Not yet supported");
			}
			return buildMethodCall(mth, insn, isRange, values, callMthHandle);
		} catch (Exception e) {
			throw new JadxRuntimeException("'invoke-custom' instruction processing error: " + e.getMessage(), e);
		}
	}

	@NotNull
	private static InvokeCustomNode buildMethodCall(MethodNode mth, InsnData insn, boolean isRange,
			List<EncodedValue> values, IMethodHandle callMthHandle) {
		RootNode root = mth.root();
		IMethodProto lambdaProto = (IMethodProto) values.get(2).getValue();
		MethodInfo lambdaInfo = MethodInfo.fromMethodProto(root, mth.getParentClass().getClassInfo(), "", lambdaProto);

		MethodHandleType methodHandleType = callMthHandle.getType();
		InvokeCustomNode invokeCustomNode = new InvokeCustomNode(lambdaInfo, insn, false, isRange);
		invokeCustomNode.setHandleType(methodHandleType);

		ClassInfo implCls = ClassInfo.fromType(root, lambdaInfo.getReturnType());
		String implName = (String) values.get(1).getValue();
		IMethodProto implProto = (IMethodProto) values.get(3).getValue();
		MethodInfo implMthInfo = MethodInfo.fromMethodProto(root, implCls, implName, implProto);
		invokeCustomNode.setImplMthInfo(implMthInfo);

		MethodInfo callMthInfo = MethodInfo.fromRef(root, callMthHandle.getMethodRef());
		InvokeNode invokeNode = buildInvokeNode(methodHandleType, invokeCustomNode, callMthInfo);

		if (methodHandleType == MethodHandleType.INVOKE_CONSTRUCTOR) {
			ConstructorInsn ctrInsn = new ConstructorInsn(mth, invokeNode);
			invokeCustomNode.setCallInsn(ctrInsn);
		} else {
			invokeCustomNode.setCallInsn(invokeNode);
		}

		MethodNode callMth = root.resolveMethod(callMthInfo);
		if (callMth != null) {
			invokeCustomNode.getCallInsn().addAttr(callMth);
			if (callMth.getAccessFlags().isSynthetic()
					&& callMth.getUseIn().size() <= 1
					&& callMth.getParentClass().equals(mth.getParentClass())) {
				// inline only synthetic methods from same class
				callMth.add(AFlag.DONT_GENERATE);
				invokeCustomNode.setInlineInsn(true);
			}
		}
		if (!invokeCustomNode.isInlineInsn()) {
			IMethodProto effectiveMthProto = (IMethodProto) values.get(5).getValue();
			List<ArgType> args = Utils.collectionMap(effectiveMthProto.getArgTypes(), ArgType::parse);
			boolean sameArgs = args.equals(callMthInfo.getArgumentsTypes());
			invokeCustomNode.setUseRef(sameArgs);
		}

		// prevent args inlining into not generated invoke custom node
		for (InsnArg arg : invokeCustomNode.getArguments()) {
			arg.add(AFlag.DONT_INLINE);
		}
		return invokeCustomNode;
	}

	@NotNull
	private static InvokeNode buildInvokeNode(MethodHandleType methodHandleType, InvokeCustomNode invokeCustomNode,
			MethodInfo callMthInfo) {
		InvokeType invokeType = convertInvokeType(methodHandleType);
		int callArgsCount = callMthInfo.getArgsCount();
		boolean instanceCall = invokeType != InvokeType.STATIC;
		if (instanceCall) {
			callArgsCount++;
		}
		InvokeNode invokeNode = new InvokeNode(callMthInfo, invokeType, callArgsCount);

		// copy insn args
		int argsCount = invokeCustomNode.getArgsCount();
		for (int i = 0; i < argsCount; i++) {
			InsnArg arg = invokeCustomNode.getArg(i);
			invokeNode.addArg(arg.duplicate());
		}
		if (callArgsCount > argsCount) {
			// fill remaining args with NamedArg
			int callArgNum = argsCount;
			if (instanceCall) {
				callArgNum--; // start from instance type
			}
			List<ArgType> callArgTypes = callMthInfo.getArgumentsTypes();
			for (int i = argsCount; i < callArgsCount; i++) {
				ArgType argType;
				if (callArgNum < 0) {
					// instance arg type
					argType = callMthInfo.getDeclClass().getType();
				} else {
					argType = callArgTypes.get(callArgNum++);
				}
				invokeNode.addArg(new NamedArg("v" + i, argType));
			}
		}
		return invokeNode;
	}

	/**
	 * Expect LambdaMetafactory.metafactory method
	 */
	private static boolean checkLinkerMethod(List<EncodedValue> values) {
		if (values.size() < 6) {
			return false;
		}
		IMethodHandle methodHandle = (IMethodHandle) values.get(0).getValue();
		if (methodHandle.getType() != MethodHandleType.INVOKE_STATIC) {
			return false;
		}
		IMethodRef methodRef = methodHandle.getMethodRef();
		if (!methodRef.getName().equals("metafactory")) {
			return false;
		}
		if (!methodRef.getParentClassType().equals("Ljava/lang/invoke/LambdaMetafactory;")) {
			return false;
		}
		return true;
	}

	private static InvokeType convertInvokeType(MethodHandleType type) {
		switch (type) {
			case INVOKE_STATIC:
				return InvokeType.STATIC;
			case INVOKE_INSTANCE:
				return InvokeType.VIRTUAL;
			case INVOKE_DIRECT:
			case INVOKE_CONSTRUCTOR:
				return InvokeType.DIRECT;
			case INVOKE_INTERFACE:
				return InvokeType.INTERFACE;

			default:
				throw new JadxRuntimeException("Unsupported method handle type: " + type);
		}
	}
}
