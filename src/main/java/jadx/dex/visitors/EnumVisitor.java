package jadx.dex.visitors;

import jadx.dex.attributes.AttributeFlag;
import jadx.dex.attributes.EnumClassAttr;
import jadx.dex.attributes.EnumClassAttr.EnumField;
import jadx.dex.info.ClassInfo;
import jadx.dex.info.FieldInfo;
import jadx.dex.info.MethodInfo;
import jadx.dex.instructions.IndexInsnNode;
import jadx.dex.instructions.InsnType;
import jadx.dex.instructions.args.InsnArg;
import jadx.dex.instructions.args.RegisterArg;
import jadx.dex.instructions.mods.ConstructorInsn;
import jadx.dex.nodes.BlockNode;
import jadx.dex.nodes.ClassNode;
import jadx.dex.nodes.FieldNode;
import jadx.dex.nodes.InsnNode;
import jadx.dex.nodes.MethodNode;
import jadx.utils.exceptions.JadxException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnumVisitor extends AbstractVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(EnumVisitor.class);

	@Override
	public boolean visit(ClassNode cls) throws JadxException {
		if (!cls.getAccessFlags().isEnum()
				|| !cls.getSuperClass().getFullName().equals("java.lang.Enum"))
			return true;

		// collect enum fields, remove synthetic
		List<FieldNode> enumFields = new ArrayList<FieldNode>();
		for (Iterator<FieldNode> it = cls.getFields().iterator(); it.hasNext(); ) {
			FieldNode f = it.next();
			if (f.getAccessFlags().isEnum()) {
				enumFields.add(f);
				it.remove();
			} else if (f.getAccessFlags().isSynthetic()) {
				it.remove();
			}
		}

		MethodNode staticMethod = null;

		// remove synthetic methods
		for (Iterator<MethodNode> it = cls.getMethods().iterator(); it.hasNext(); ) {
			MethodNode mth = it.next();
			MethodInfo mi = mth.getMethodInfo();
			if (mi.isClassInit()) {
				staticMethod = mth;
			} else if (mi.isConstructor() && !mth.getAccessFlags().isSynthetic()) {
				if (mi.getShortId().equals("<init>(Ljava/lang/String;I)"))
					it.remove();
			} else if (mth.getAccessFlags().isSynthetic()
					|| mi.getShortId().equals("values()")
					|| mi.getShortId().equals("valueOf(Ljava/lang/String;)")) {
				it.remove();
			}
		}

		EnumClassAttr attr = new EnumClassAttr(enumFields.size());
		cls.getAttributes().add(attr);

		if (staticMethod == null) {
			LOG.warn("Enum class init method not found: {}", cls);
			// for this broken enum puts found fields and mark as inconsistent
			cls.getAttributes().add(AttributeFlag.INCONSISTENT_CODE);
			for (FieldNode field : enumFields) {
				attr.getFields().add(new EnumField(field.getName(), 0));
			}
			return false;
		}
		attr.setStaticMethod(staticMethod);

		// move enum specific instruction from static method to separate list
		BlockNode staticBlock = staticMethod.getBasicBlocks().get(0);
		List<InsnNode> insns = new ArrayList<InsnNode>();
		List<InsnNode> list = staticBlock.getInstructions();
		int size = list.size();
		for (int i = 0; i < size; i++) {
			InsnNode insn = list.get(i);
			insns.add(insn);
			if (insn.getType() == InsnType.SPUT) {
				IndexInsnNode fp = (IndexInsnNode) insn;
				FieldInfo f = (FieldInfo) fp.getIndex();
				if (f.getName().equals("$VALUES")) {
					if (i == size - 1)
						cls.getMethods().remove(staticMethod);
					else
						list.subList(0, i + 1).clear();
					break;
				}
			}
		}

		for (InsnNode insn : insns) {
			if (insn.getType() == InsnType.CONSTRUCTOR) {
				ConstructorInsn co = (ConstructorInsn) insn;

				if (insn.getArgsCount() < 2)
					continue;

				ClassInfo clsInfo = co.getClassType();
				ClassNode constrCls = cls.dex().resolveClass(clsInfo);
				if (constrCls == null)
					continue;

				if (!clsInfo.equals(cls.getClassInfo()) && !constrCls.getAccessFlags().isEnum())
					continue;

				RegisterArg nameArg = (RegisterArg) insn.getArg(0);
				// InsnArg pos = insn.getArg(1);
				// TODO add check: pos == j
				String name = (String) nameArg.getConstValue();

				EnumField field = new EnumField(name, insn.getArgsCount() - 2);
				attr.getFields().add(field);
				for (int i = 2; i < insn.getArgsCount(); i++) {
					InsnArg constrArg;
					InsnArg iArg = insn.getArg(i);
					if (iArg.isLiteral()) {
						constrArg = iArg;
					} else {
						constrArg = CodeShrinker.inlineArgument(staticMethod, (RegisterArg) iArg);
						if (constrArg == null)
							throw new JadxException("Can't inline constructor arg in enum: " + cls);
					}
					field.getArgs().add(constrArg);
				}

				if (co.getClassType() != cls.getClassInfo()) {
					// enum contains additional methods
					for (ClassNode innerCls : cls.getInnerClasses()) {
						if (innerCls.getClassInfo().equals(co.getClassType())) {
							// remove constructor, because it is anonymous class
							for (Iterator<?> mit = innerCls.getMethods().iterator(); mit.hasNext(); ) {
								MethodNode innerMth = (MethodNode) mit.next();
								if (innerMth.getAccessFlags().isConstructor())
									mit.remove();
							}
							field.setCls(innerCls);
							innerCls.getAttributes().add(AttributeFlag.DONT_GENERATE);
						}
					}
				}
			}
		}
		return false;
	}
}
