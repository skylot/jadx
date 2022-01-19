package jadx.core.dex.visitors.kotlin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JadxArgs.UseKotlinMethodsForVarNames;
import jadx.api.plugins.input.data.attributes.JadxAttrType;
import jadx.core.deobf.NameMapper;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.ConstStringNode;
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.InvokeNode;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.dex.visitors.InitCodeVariables;
import jadx.core.dex.visitors.JadxVisitor;
import jadx.core.dex.visitors.debuginfo.DebugInfoApplyVisitor;
import jadx.core.dex.visitors.rename.CodeRenameVisitor;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxException;

@JadxVisitor(
		name = "ProcessKotlinInternals",
		desc = "Use variable names from Kotlin intrinsic1 methods",
		runAfter = {
				InitCodeVariables.class,
				DebugInfoApplyVisitor.class
		},
		runBefore = {
				CodeRenameVisitor.class
		}
)
public class ProcessKotlinInternals extends AbstractVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(ProcessKotlinInternals.class);

	private static final String KOTLIN_INTERNAL_PKG = "kotlin.jvm.internal.";
	private static final String KOTLIN_INTRINSICS_CLS = KOTLIN_INTERNAL_PKG + "Intrinsics";
	private static final String KOTLIN_VARNAME_SOURCE_MTH1 = "(Ljava/lang/Object;Ljava/lang/String;)V";
	private static final String KOTLIN_VARNAME_SOURCE_MTH2 = "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;)V";

	private @Nullable ClassInfo kotlinIntrinsicsCls;
	private Set<MethodInfo> kotlinVarNameSourceMethods;
	private boolean hideInsns;

	@Override
	public void init(RootNode root) throws JadxException {
		ClassNode kotlinCls = searchKotlinIntrinsicsClass(root);
		if (kotlinCls != null) {
			kotlinIntrinsicsCls = kotlinCls.getClassInfo();
			kotlinVarNameSourceMethods = collectMethods(kotlinCls);
			LOG.debug("Kotlin Intrinsics class: {}, methods: {}", kotlinCls, kotlinVarNameSourceMethods.size());
		} else {
			kotlinIntrinsicsCls = null;
			LOG.debug("Kotlin Intrinsics class not found");
		}
		hideInsns = root.getArgs().getUseKotlinMethodsForVarNames() == UseKotlinMethodsForVarNames.APPLY_AND_HIDE;
	}

	@Override
	public boolean visit(ClassNode cls) {
		if (kotlinIntrinsicsCls == null) {
			return false;
		}
		for (MethodNode mth : cls.getMethods()) {
			processMth(mth);
		}
		return true;
	}

	private void processMth(MethodNode mth) {
		if (mth.isNoCode()) {
			return;
		}
		for (BlockNode block : mth.getBasicBlocks()) {
			for (InsnNode insn : block.getInstructions()) {
				if (insn.getType() == InsnType.INVOKE) {
					try {
						processInvoke(mth, insn);
					} catch (Exception e) {
						mth.addWarnComment("Failed to extract var names", e);
					}
				}
			}
		}
	}

	private void processInvoke(MethodNode mth, InsnNode insn) {
		int argsCount = insn.getArgsCount();
		if (argsCount < 2) {
			return;
		}
		MethodInfo invokeMth = ((InvokeNode) insn).getCallMth();
		if (!kotlinVarNameSourceMethods.contains(invokeMth)) {
			return;
		}
		InsnArg firstArg = insn.getArg(0);
		if (!firstArg.isRegister()) {
			return;
		}
		RegisterArg varArg = (RegisterArg) firstArg;
		boolean renamed = false;
		if (argsCount == 2) {
			String str = getConstString(mth, insn, 1);
			if (str != null) {
				renamed = checkAndRename(varArg, str);
			}
		} else if (argsCount == 3) {
			// TODO: use second arg for rename class
			String str = getConstString(mth, insn, 2);
			if (str != null) {
				renamed = checkAndRename(varArg, str);
			}
		}
		if (renamed && hideInsns) {
			insn.add(AFlag.DONT_GENERATE);
		}
	}

	private boolean checkAndRename(RegisterArg arg, String str) {
		String name = trimName(str);
		if (NameMapper.isValidAndPrintable(name)) {
			arg.getSVar().getCodeVar().setName(name);
			return true;
		}
		return false;
	}

	@Nullable
	private String getConstString(MethodNode mth, InsnNode insn, int arg) {
		InsnArg strArg = insn.getArg(arg);
		if (!strArg.isInsnWrap()) {
			return null;
		}
		InsnNode constInsn = ((InsnWrapArg) strArg).getWrapInsn();
		InsnType insnType = constInsn.getType();
		if (insnType == InsnType.CONST_STR) {
			return ((ConstStringNode) constInsn).getString();
		}
		if (insnType == InsnType.SGET) {
			// revert const field inline :(
			FieldInfo fieldInfo = (FieldInfo) ((IndexInsnNode) constInsn).getIndex();
			FieldNode fieldNode = mth.root().resolveField(fieldInfo);
			if (fieldNode != null) {
				String str = (String) fieldNode.get(JadxAttrType.CONSTANT_VALUE).getValue();
				InsnArg newArg = InsnArg.wrapArg(new ConstStringNode(str));
				insn.replaceArg(strArg, newArg);
				return str;
			}
		}
		return null;
	}

	private String trimName(String str) {
		if (str.startsWith("$this$")) {
			return str.substring(6);
		}
		if (str.startsWith("$")) {
			return str.substring(1);
		}
		return str;
	}

	@Nullable
	private static ClassNode searchKotlinIntrinsicsClass(RootNode root) {
		ClassNode kotlinCls = root.resolveClass(KOTLIN_INTRINSICS_CLS);
		if (kotlinCls != null) {
			return kotlinCls;
		}
		List<ClassNode> candidates = new ArrayList<>();
		for (ClassNode cls : root.getClasses()) {
			if (isKotlinIntrinsicsClass(cls)) {
				candidates.add(cls);
			}
		}
		return Utils.getOne(candidates);
	}

	private static boolean isKotlinIntrinsicsClass(ClassNode cls) {
		if (!cls.getClassInfo().getFullName().startsWith(KOTLIN_INTERNAL_PKG)) {
			return false;
		}
		if (cls.getMethods().size() < 5) {
			return false;
		}
		int mthCount = 0;
		for (MethodNode mth : cls.getMethods()) {
			if (mth.getAccessFlags().isStatic()
					&& mth.getMethodInfo().getShortId().endsWith(KOTLIN_VARNAME_SOURCE_MTH1)) {
				mthCount++;
			}
		}
		return mthCount > 2;
	}

	private Set<MethodInfo> collectMethods(ClassNode kotlinCls) {
		Set<MethodInfo> set = new HashSet<>();
		for (MethodNode mth : kotlinCls.getMethods()) {
			if (!mth.getAccessFlags().isStatic()) {
				continue;
			}
			String shortId = mth.getMethodInfo().getShortId();
			if (shortId.endsWith(KOTLIN_VARNAME_SOURCE_MTH1) || shortId.endsWith(KOTLIN_VARNAME_SOURCE_MTH2)) {
				set.add(mth.getMethodInfo());
			}
		}
		return set;
	}
}
