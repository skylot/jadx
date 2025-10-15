package jadx.core.dex.instructions.invokedynamic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import jadx.api.plugins.input.data.IMethodHandle;
import jadx.api.plugins.input.data.IMethodProto;
import jadx.api.plugins.input.data.IMethodRef;
import jadx.api.plugins.input.data.MethodHandleType;
import jadx.api.plugins.input.data.annotations.EncodedType;
import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.api.plugins.input.insns.InsnData;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.InvokeCustomNode;
import jadx.core.dex.instructions.InvokeNode;
import jadx.core.dex.instructions.InvokeType;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.NamedArg;
import jadx.core.dex.instructions.mods.ConstructorInsn;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class CustomLambdaCall {

	/**
	 * Expect LambdaMetafactory.metafactory method
	 */
	public static boolean isLambdaInvoke(List<EncodedValue> values) {
		if (values.size() < 6) {
			return false;
		}
		EncodedValue mthRef = values.get(0);
		if (mthRef.getType() != EncodedType.ENCODED_METHOD_HANDLE) {
			return false;
		}
		IMethodHandle methodHandle = (IMethodHandle) mthRef.getValue();
		if (methodHandle.getType() != MethodHandleType.INVOKE_STATIC) {
			return false;
		}
		IMethodRef methodRef = methodHandle.getMethodRef();
		if (!methodRef.getParentClassType().equals("Ljava/lang/invoke/LambdaMetafactory;")) {
			return false;
		}
		String mthName = methodRef.getName();
		return mthName.equals("metafactory") || mthName.equals("altMetafactory");
	}

	public static InvokeCustomNode buildLambdaMethodCall(MethodNode mth, InsnData insn, boolean isRange, List<EncodedValue> values) {
		IMethodHandle callMthHandle = (IMethodHandle) values.get(4).getValue();
		if (callMthHandle.getType().isField()) {
			throw new JadxRuntimeException("Not yet supported");
		}
		InvokeCustomNode resNode = buildMethodCall(mth, insn, isRange, values, callMthHandle);
		int resReg = insn.getResultReg();
		if (resReg != -1) {
			resNode.setResult(InsnArg.reg(resReg, mth.getReturnType()));
		}
		return resNode;
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

		// Parse marker interfaces for intersection types from altMetafactory args
		List<ArgType> markerInterfaces = parseMarkerInterfaces(root, values);
		if (!markerInterfaces.isEmpty()) {
			invokeCustomNode.setMarkerInterfaces(markerInterfaces);
		}

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
		InvokeType invokeType = InvokeCustomUtils.convertInvokeType(methodHandleType);
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
	 * Parse marker interfaces from altMetafactory additional arguments.
	 * 
	 * Bootstrap method arguments structure:
	 * Standard metafactory (6 args):
	 * - arg[0]: bootstrap method handle (INVOKE_STATIC LambdaMetafactory.metafactory)
	 * - arg[1]: SAM method name (String, e.g., "get")
	 * - arg[2]: SAM method signature (MethodType)
	 * - arg[3]: SAM method type (MethodType)
	 * - arg[4]: implementation method handle (MethodHandle)
	 * - arg[5]: instantiated method type (MethodType)
	 *
	 * altMetafactory (6+ args):
	 * - arg[0-5]: same as above
	 * - arg[6]: flags (int) - bit 1 (0x02) indicates marker interfaces present
	 * - arg[7]: marker interface count (int)
	 * - arg[8+]: marker interface types (Type[])
	 */
	private static List<ArgType> parseMarkerInterfaces(RootNode root, List<EncodedValue> values) {
		// alt Metafactory needs more than 6 args
		if (values.size() <= 6) {
			return Collections.emptyList();
		}

		try {
			// Check if this is altMetafactory by looking for integer flags at index 6
			EncodedValue flagsValue = values.get(6);
			if (flagsValue.getType() != EncodedType.ENCODED_INT) {
				return Collections.emptyList();
			}
			
			int flags = (Integer) flagsValue.getValue();
			// Bit 1 (0x02) in flags indicates marker interfaces are present
			boolean hasMarkers = (flags & 0x02) != 0;
			
			if (!hasMarkers || values.size() < 8) {
				return Collections.emptyList();
			}

			// arg[7] should be marker count
			EncodedValue markerCountValue = values.get(7);
			if (markerCountValue.getType() != EncodedType.ENCODED_INT) {
				return Collections.emptyList();
			}

			int markerCount = (Integer) markerCountValue.getValue();
			if (markerCount <= 0 || values.size() < 8 + markerCount) {
				return Collections.emptyList();
			}

			// Parse marker types from arg[8] onwards
			List<ArgType> markers = new ArrayList<>(markerCount);
			for (int i = 0; i < markerCount; i++) {
				EncodedValue markerValue = values.get(8 + i);
				if (markerValue.getType() == EncodedType.ENCODED_TYPE) {
					String typeStr = (String) markerValue.getValue();
					ArgType markerType = ArgType.parse(typeStr);
					if (markerType != null && !markerType.equals(ArgType.OBJECT)) {
						markers.add(markerType);
					}
				}
			}
			return markers;
		} catch (Exception e) {
			// If parsing fails, return empty list - don't break decompilation
			return Collections.emptyList();
		}
	}
}
