package jadx.core.dex.visitors;

import jadx.core.codegen.TypeGen;
import jadx.core.deobf.NameMapper;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.nodes.EnumClassAttr;
import jadx.core.dex.attributes.nodes.EnumClassAttr.EnumField;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.mods.ConstructorInsn;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.DexNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.ErrorsCounter;
import jadx.core.utils.InsnUtils;
import jadx.core.utils.exceptions.JadxException;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JadxVisitor(
		name = "EnumVisitor",
		desc = "Restore enum classes",
		runAfter = {CodeShrinker.class, ModVisitor.class}
)
public class EnumVisitor extends AbstractVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(EnumVisitor.class);

	@Override
	public boolean visit(ClassNode cls) throws JadxException {
		if (!cls.isEnum()) {
			return true;
		}
		// search class init method
		MethodNode staticMethod = null;
		for (MethodNode mth : cls.getMethods()) {
			MethodInfo mi = mth.getMethodInfo();
			if (mi.isClassInit()) {
				staticMethod = mth;
				break;
			}
		}
		if (staticMethod == null) {
			ErrorsCounter.classError(cls, "Enum class init method not found");
			return true;
		}

		ArgType clsType = cls.getClassInfo().getType();
		String enumConstructor = "<init>(Ljava/lang/String;I)V";
		// TODO: detect these methods by analyzing method instructions
		String valuesOfMethod = "valueOf(Ljava/lang/String;)" + TypeGen.signature(clsType);
		String valuesMethod = "values()" + TypeGen.signature(ArgType.array(clsType));

		// collect enum fields, remove synthetic
		List<FieldNode> enumFields = new ArrayList<FieldNode>();
		for (FieldNode f : cls.getFields()) {
			if (f.getAccessFlags().isEnum()) {
				enumFields.add(f);
				f.add(AFlag.DONT_GENERATE);
			} else if (f.getAccessFlags().isSynthetic()) {
				f.add(AFlag.DONT_GENERATE);
			}
		}

		// remove synthetic methods
		for (MethodNode mth : cls.getMethods()) {
			MethodInfo mi = mth.getMethodInfo();
			if (mi.isClassInit()) {
				continue;
			}
			String shortId = mi.getShortId();
			boolean isSynthetic = mth.getAccessFlags().isSynthetic();
			if (mi.isConstructor() && !isSynthetic) {
				if (shortId.equals(enumConstructor)) {
					mth.add(AFlag.DONT_GENERATE);
				}
			} else if (isSynthetic
					|| shortId.equals(valuesMethod)
					|| shortId.equals(valuesOfMethod)) {
				mth.add(AFlag.DONT_GENERATE);
			}
		}

		EnumClassAttr attr = new EnumClassAttr(enumFields.size());
		cls.addAttr(attr);

		attr.setStaticMethod(staticMethod);
		ClassInfo classInfo = cls.getClassInfo();

		// move enum specific instruction from static method to separate list
		BlockNode staticBlock = staticMethod.getBasicBlocks().get(0);
		List<InsnNode> enumPutInsns = new ArrayList<InsnNode>();
		List<InsnNode> list = staticBlock.getInstructions();
		int size = list.size();
		for (int i = 0; i < size; i++) {
			InsnNode insn = list.get(i);
			if (insn.getType() != InsnType.SPUT) {
				continue;
			}
			FieldInfo f = (FieldInfo) ((IndexInsnNode) insn).getIndex();
			if (!f.getDeclClass().equals(classInfo)) {
				continue;
			}
			FieldNode fieldNode = cls.searchField(f);
			if (fieldNode != null && isEnumArrayField(classInfo, fieldNode)) {
				if (i == size - 1) {
					staticMethod.add(AFlag.DONT_GENERATE);
				} else {
					list.subList(0, i + 1).clear();
				}
				break;
			} else {
				enumPutInsns.add(insn);
			}
		}

		for (InsnNode putInsn : enumPutInsns) {
			ConstructorInsn co = getConstructorInsn(putInsn);
			if (co == null || co.getArgsCount() < 2) {
				continue;
			}
			ClassInfo clsInfo = co.getClassType();
			ClassNode constrCls = cls.dex().resolveClass(clsInfo);
			if (constrCls == null) {
				continue;
			}
			if (!clsInfo.equals(classInfo) && !constrCls.getAccessFlags().isEnum()) {
				continue;
			}
			FieldInfo fieldInfo = (FieldInfo) ((IndexInsnNode) putInsn).getIndex();
			String name = getConstString(cls.dex(), co.getArg(0));
			if (name != null
					&& !fieldInfo.getAlias().equals(name)
					&& NameMapper.isValidIdentifier(name)) {
				// LOG.debug("Rename enum field: '{}' to '{}' in {}", fieldInfo.getName(), name, cls);
				fieldInfo.setAlias(name);
			}

			EnumField field = new EnumField(fieldInfo, co, 2);
			attr.getFields().add(field);

			if (!co.getClassType().equals(classInfo)) {
				// enum contains additional methods
				for (ClassNode innerCls : cls.getInnerClasses()) {
					processEnumInnerCls(co, field, innerCls);
				}
			}
		}
		return false;
	}

	private static void processEnumInnerCls(ConstructorInsn co, EnumField field, ClassNode innerCls) {
		if (!innerCls.getClassInfo().equals(co.getClassType())) {
			return;
		}
		// remove constructor, because it is anonymous class
		for (MethodNode innerMth : innerCls.getMethods()) {
			if (innerMth.getAccessFlags().isConstructor()) {
				innerMth.add(AFlag.DONT_GENERATE);
			}
		}
		field.setCls(innerCls);
		innerCls.add(AFlag.DONT_GENERATE);
	}

	private boolean isEnumArrayField(ClassInfo classInfo, FieldNode fieldNode) {
		if (fieldNode.getAccessFlags().isSynthetic()) {
			ArgType fType = fieldNode.getType();
			if (fType.isArray() && fType.getArrayRootElement().equals(classInfo.getType())) {
				return true;
			}
		}
		return false;
	}

	private ConstructorInsn getConstructorInsn(InsnNode putInsn) {
		if (putInsn.getArgsCount() != 1) {
			return null;
		}
		InsnArg arg = putInsn.getArg(0);
		if (arg.isInsnWrap()) {
			InsnNode wrapInsn = ((InsnWrapArg) arg).getWrapInsn();
			if (wrapInsn.getType() == InsnType.CONSTRUCTOR) {
				return (ConstructorInsn) wrapInsn;
			}
		}
		return null;
	}

	private String getConstString(DexNode dex, InsnArg arg) {
		if (arg.isInsnWrap()) {
			InsnNode constInsn = ((InsnWrapArg) arg).getWrapInsn();
			Object constValue = InsnUtils.getConstValueByInsn(dex, constInsn);
			if (constValue instanceof String) {
				return (String) constValue;
			}
		}
		return null;
	}
}
