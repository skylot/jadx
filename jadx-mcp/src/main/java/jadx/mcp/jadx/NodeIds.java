package jadx.mcp.jadx;

import jadx.api.JavaClass;
import jadx.api.JavaField;
import jadx.api.JavaMethod;
import jadx.api.JavaNode;
import jadx.api.JavaPackage;
import jadx.api.JavaVariable;
import jadx.core.dex.nodes.ClassNode;

/**
 * Stable node identifiers based on original (raw) names, not affected by renames and deobfuscation.
 * <ul>
 * <li>class: {@code com.example.Outer$Inner}</li>
 * <li>method: {@code com.example.Outer$Inner->doWork(ILjava/lang/String;)V}</li>
 * <li>field: {@code com.example.Outer->counter:I}</li>
 * <li>package: {@code com.example}</li>
 * <li>variable: {@code com.example.Outer->doWork(I)V#r2v1}</li>
 * </ul>
 */
public final class NodeIds {
	public static final String MEMBER_SEPARATOR = "->";
	public static final String VAR_SEPARATOR = "#";

	private NodeIds() {
	}

	public static String classId(ClassNode cls) {
		return cls.getClassInfo().getRawName();
	}

	public static String classId(JavaClass cls) {
		return classId(cls.getClassNode());
	}

	public static String methodId(JavaMethod mth) {
		return classId(mth.getDeclaringClass()) + MEMBER_SEPARATOR + mth.getMethodNode().getMethodInfo().getShortId();
	}

	public static String fieldId(JavaField fld) {
		return classId(fld.getDeclaringClass()) + MEMBER_SEPARATOR + fld.getFieldNode().getFieldInfo().getShortId();
	}

	public static String packageId(JavaPackage pkg) {
		return pkg.getRawFullName();
	}

	public static String variableId(JavaVariable var) {
		return methodId(var.getMth()) + VAR_SEPARATOR + varShortId(var.getReg(), var.getSsa());
	}

	public static String varShortId(int reg, int ssa) {
		return "r" + reg + 'v' + ssa;
	}

	public static String kind(JavaNode node) {
		if (node instanceof JavaClass) {
			return "class";
		}
		if (node instanceof JavaMethod) {
			return "method";
		}
		if (node instanceof JavaField) {
			return "field";
		}
		if (node instanceof JavaPackage) {
			return "package";
		}
		if (node instanceof JavaVariable) {
			return "variable";
		}
		return "unknown";
	}

	public static String id(JavaNode node) {
		if (node instanceof JavaClass) {
			return classId((JavaClass) node);
		}
		if (node instanceof JavaMethod) {
			return methodId((JavaMethod) node);
		}
		if (node instanceof JavaField) {
			return fieldId((JavaField) node);
		}
		if (node instanceof JavaPackage) {
			return packageId((JavaPackage) node);
		}
		if (node instanceof JavaVariable) {
			return variableId((JavaVariable) node);
		}
		return node.getFullName();
	}
}
