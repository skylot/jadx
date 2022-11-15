package jadx.core.deobf;

import jadx.api.deobf.IAliasProvider;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.PackageNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.StringUtils;

public class DeobfAliasProvider implements IAliasProvider {

	private int pkgIndex = 0;
	private int clsIndex = 0;
	private int fldIndex = 0;
	private int mthIndex = 0;

	private int maxLength;

	@Override
	public void init(RootNode root) {
		this.maxLength = root.getArgs().getDeobfuscationMaxLength();
	}

	@Override
	public void initIndexes(int pkg, int cls, int fld, int mth) {
		pkgIndex = pkg;
		clsIndex = cls;
		fldIndex = fld;
		mthIndex = mth;
	}

	@Override
	public String forPackage(PackageNode pkg) {
		return String.format("p%03d%s", pkgIndex++, prepareNamePart(pkg.getPkgInfo().getName()));
	}

	@Override
	public String forClass(ClassNode cls) {
		String prefix = makeClsPrefix(cls);
		return String.format("%sC%04d%s", prefix, clsIndex++, prepareNamePart(cls.getName()));
	}

	@Override
	public String forField(FieldNode fld) {
		return String.format("f%d%s", fldIndex++, prepareNamePart(fld.getName()));
	}

	@Override
	public String forMethod(MethodNode mth) {
		String prefix = mth.contains(AType.METHOD_OVERRIDE) ? "mo" : "m";
		return String.format("%s%d%s", prefix, mthIndex++, prepareNamePart(mth.getName()));
	}

	private String prepareNamePart(String name) {
		if (name.length() > maxLength) {
			return 'x' + Integer.toHexString(name.hashCode());
		}
		return NameMapper.removeInvalidCharsMiddle(name);
	}

	/**
	 * Generate a prefix for a class name that bases on certain class properties, certain
	 * extended superclasses or implemented interfaces.
	 */
	private String makeClsPrefix(ClassNode cls) {
		if (cls.isEnum()) {
			return "Enum";
		}
		StringBuilder result = new StringBuilder();
		if (cls.getAccessFlags().isInterface()) {
			result.append("Interface");
		} else if (cls.getAccessFlags().isAbstract()) {
			result.append("Abstract");
		}
		result.append(getBaseName(cls));
		return result.toString();
	}

	/**
	 * Process current class and all super classes to get meaningful parent name
	 */
	private static String getBaseName(ClassNode cls) {
		ClassNode currentCls = cls;
		while (currentCls != null) {
			ArgType superCls = currentCls.getSuperClass();
			if (superCls != null) {
				String superClsName = superCls.getObject();
				if (superClsName.startsWith("android.app.") // e.g. Activity or Fragment
						|| superClsName.startsWith("android.os.") // e.g. AsyncTask
				) {
					return getClsName(superClsName);
				}
			}
			for (ArgType interfaceType : cls.getInterfaces()) {
				String name = interfaceType.getObject();
				if (name.equals("java.lang.Runnable")) {
					return "Runnable";
				}
				if (name.startsWith("java.util.concurrent.") // e.g. Callable
						|| name.startsWith("android.view.") // e.g. View.OnClickListener
						|| name.startsWith("android.content.") // e.g. DialogInterface.OnClickListener
				) {
					return getClsName(name);
				}
			}
			if (superCls == null) {
				break;
			}
			currentCls = cls.root().resolveClass(superCls);
		}
		return "";
	}

	private static String getClsName(String name) {
		int pgkEnd = name.lastIndexOf('.');
		String clsName = name.substring(pgkEnd + 1);
		return StringUtils.removeChar(clsName, '$');
	}
}
