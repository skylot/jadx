package jadx.core.deobf;

import jadx.api.deobf.IAliasProvider;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.PackageNode;
import jadx.core.dex.nodes.RootNode;

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

		// Process current class and all super classes
		ClassNode currentCls = cls;
		outerLoop: while (currentCls != null) {
			if (currentCls.getSuperClass() != null) {
				String superClsName = currentCls.getSuperClass().getObject();
				if (superClsName.startsWith("android.app.")) {
					// e.g. Activity or Fragment
					result.append(superClsName.substring(12));
					break;
				} else if (superClsName.startsWith("android.os.")) {
					// e.g. AsyncTask
					result.append(superClsName.substring(11));
					break;
				}
			}
			for (ArgType intf : cls.getInterfaces()) {
				String intfClsName = intf.getObject();
				if (intfClsName.equals("java.lang.Runnable")) {
					result.append("Runnable");
					break outerLoop;
				} else if (intfClsName.startsWith("java.util.concurrent.")) {
					// e.g. Callable
					result.append(intfClsName.substring(21));
					break outerLoop;
				} else if (intfClsName.startsWith("android.view.")) {
					// e.g. View.OnClickListener
					result.append(intfClsName.substring(13));
					break outerLoop;
				} else if (intfClsName.startsWith("android.content.")) {
					// e.g. DialogInterface.OnClickListener
					result.append(intfClsName.substring(16));
					break outerLoop;
				}
			}
			if (currentCls.getSuperClass() == null) {
				break;
			}
			currentCls = cls.root().resolveClass(currentCls.getSuperClass());
		}
		return result.toString();
	}
}
