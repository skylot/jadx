package jadx.core.dex.visitors.usage;

import jadx.api.plugins.input.data.ICodeReader;
import jadx.api.plugins.input.insns.InsnData;
import jadx.api.plugins.input.insns.Opcode;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.dex.visitors.JadxVisitor;
import jadx.core.dex.visitors.OverrideMethodVisitor;
import jadx.core.dex.visitors.RenameVisitor;

@JadxVisitor(
		name = "UsageInfoVisitor",
		desc = "Scan class and methods to collect usage info and class dependencies",
		runAfter = {
				OverrideMethodVisitor.class, // add method override as use
				RenameVisitor.class // sort by alias name
		}
)
public class UsageInfoVisitor extends AbstractVisitor {

	@Override
	public void init(RootNode root) {
		UsageInfo usageInfo = new UsageInfo(root);
		for (ClassNode cls : root.getClasses()) {
			processClass(cls, usageInfo);
		}
		usageInfo.apply();
	}

	private static void processClass(ClassNode cls, UsageInfo usageInfo) {
		usageInfo.clsUse(cls, cls.getSuperClass());
		for (ArgType interfaceType : cls.getInterfaces()) {
			usageInfo.clsUse(cls, interfaceType);
		}
		for (FieldNode fieldNode : cls.getFields()) {
			usageInfo.clsUse(cls, fieldNode.getType());
		}
		// TODO: process annotations and generics
		for (MethodNode methodNode : cls.getMethods()) {
			processMethod(methodNode, usageInfo);
		}
	}

	private static void processMethod(MethodNode mth, UsageInfo usageInfo) {
		ClassNode cls = mth.getParentClass();
		usageInfo.clsUse(cls, mth.getReturnType());
		for (ArgType argType : mth.getMethodInfo().getArgumentsTypes()) {
			usageInfo.clsUse(cls, argType);
		}
		try {
			processInstructions(mth, usageInfo);
		} catch (Exception e) {
			mth.addError("Dependency scan failed", e);
		}
	}

	private static void processInstructions(MethodNode mth, UsageInfo usageInfo) {
		if (mth.isNoCode()) {
			return;
		}
		ICodeReader codeReader = mth.getCodeReader();
		if (codeReader == null) {
			return;
		}
		RootNode root = mth.root();
		codeReader.visitInstructions(insnData -> {
			try {
				processInsn(root, mth, insnData, usageInfo);
			} catch (Exception e) {
				mth.addError("Dependency scan failed at insn: " + insnData, e);
			}
		});
	}

	private static void processInsn(RootNode root, MethodNode mth, InsnData insnData, UsageInfo usageInfo) {
		if (insnData.getOpcode() == Opcode.UNKNOWN) {
			return;
		}
		switch (insnData.getIndexType()) {
			case TYPE_REF:
				insnData.decode();
				ArgType usedType = ArgType.parse(insnData.getIndexAsType());
				usageInfo.clsUse(mth, usedType);
				break;

			case FIELD_REF:
				insnData.decode();
				FieldNode fieldNode = root.resolveField(FieldInfo.fromData(root, insnData.getIndexAsField()));
				if (fieldNode != null) {
					usageInfo.fieldUse(mth, fieldNode);
				}
				break;

			case METHOD_REF:
				insnData.decode();
				MethodNode methodNode = root.resolveMethod(MethodInfo.fromRef(root, insnData.getIndexAsMethod()));
				if (methodNode != null) {
					usageInfo.methodUse(mth, methodNode);
				}
				break;
		}
	}
}
