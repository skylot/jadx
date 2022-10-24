package jadx.core.dex.visitors.usage;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.plugins.input.data.ICallSite;
import jadx.api.plugins.input.data.ICodeReader;
import jadx.api.plugins.input.data.IMethodHandle;
import jadx.api.plugins.input.data.IMethodRef;
import jadx.api.plugins.input.insns.InsnData;
import jadx.api.plugins.input.insns.Opcode;
import jadx.api.plugins.input.insns.custom.ICustomPayload;
import jadx.api.usage.IUsageInfoCache;
import jadx.api.usage.IUsageInfoData;
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
import jadx.core.dex.visitors.rename.RenameVisitor;
import jadx.core.utils.ListUtils;
import jadx.core.utils.input.InsnDataUtils;

@JadxVisitor(
		name = "UsageInfoVisitor",
		desc = "Scan class and methods to collect usage info and class dependencies",
		runAfter = {
				OverrideMethodVisitor.class, // add method override as use
				RenameVisitor.class // sort by alias name
		}
)
public class UsageInfoVisitor extends AbstractVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(UsageInfoVisitor.class);

	@Override
	public void init(RootNode root) {
		IUsageInfoCache usageCache = root.getArgs().getUsageInfoCache();
		IUsageInfoData usageInfoData = usageCache.get(root);
		if (usageInfoData != null) {
			try {
				apply(usageInfoData);
				return;
			} catch (Exception e) {
				LOG.error("Failed to apply cached usage data", e);
			}
		}
		IUsageInfoData collectedInfoData = buildUsageData(root);
		usageCache.set(root, collectedInfoData);
		apply(collectedInfoData);
	}

	private static void apply(IUsageInfoData usageInfoData) {
		long start = System.currentTimeMillis();
		usageInfoData.apply();
		if (LOG.isDebugEnabled()) {
			LOG.debug("Apply usage data in {}ms", System.currentTimeMillis() - start);
		}
	}

	private static IUsageInfoData buildUsageData(RootNode root) {
		UsageInfo usageInfo = new UsageInfo(root);
		for (ClassNode cls : root.getClasses()) {
			processClass(cls, usageInfo);
		}
		return usageInfo;
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
				FieldNode fieldNode = root.resolveField(FieldInfo.fromRef(root, insnData.getIndexAsField()));
				if (fieldNode != null) {
					usageInfo.fieldUse(mth, fieldNode);
				}
				break;

			case METHOD_REF: {
				insnData.decode();
				IMethodRef mthRef;
				ICustomPayload payload = insnData.getPayload();
				if (payload != null) {
					mthRef = ((IMethodRef) payload);
				} else {
					mthRef = insnData.getIndexAsMethod();
				}
				MethodNode methodNode = root.resolveMethod(MethodInfo.fromRef(root, mthRef));
				if (methodNode != null) {
					usageInfo.methodUse(mth, methodNode);
				}
				break;
			}

			case CALL_SITE: {
				insnData.decode();
				ICallSite callSite = InsnDataUtils.getCallSite(insnData);
				IMethodHandle methodHandle = InsnDataUtils.getMethodHandleAt(callSite, 4);
				if (methodHandle != null) {
					IMethodRef mthRef = methodHandle.getMethodRef();
					MethodNode mthNode = root.resolveMethod(MethodInfo.fromRef(root, mthRef));
					if (mthNode != null) {
						usageInfo.methodUse(mth, mthNode);
					}
				}
				break;
			}
		}
	}

	public static void replaceMethodUsage(MethodNode mergeIntoMth, MethodNode sourceMth) {
		List<MethodNode> mergedUsage = ListUtils.distinctMergeSortedLists(mergeIntoMth.getUseIn(), sourceMth.getUseIn());
		mergedUsage.remove(sourceMth);
		mergeIntoMth.setUseIn(mergedUsage);
		sourceMth.setUseIn(Collections.emptyList());
	}

	@Override
	public String toString() {
		return "UsageInfoVisitor";
	}
}
