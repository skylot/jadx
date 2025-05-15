package jadx.gui.ui.cellrenders;

import java.util.Iterator;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import jadx.api.JavaMethod;
import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.gui.utils.Icons;
import jadx.gui.utils.OverlayIcon;
import jadx.gui.utils.UiUtils;

public class MethodRenderHelper {

	private static final ImageIcon ICON_METHOD_ABSTRACT = UiUtils.openSvgIcon("nodes/abstractMethod");
	private static final ImageIcon ICON_METHOD_PRIVATE = UiUtils.openSvgIcon("nodes/privateMethod");
	private static final ImageIcon ICON_METHOD_PROTECTED = UiUtils.openSvgIcon("nodes/protectedMethod");
	private static final ImageIcon ICON_METHOD_PUBLIC = UiUtils.openSvgIcon("nodes/publicMethod");
	private static final ImageIcon ICON_METHOD_CONSTRUCTOR = UiUtils.openSvgIcon("nodes/constructorMethod");
	private static final ImageIcon ICON_METHOD_SYNC = UiUtils.openSvgIcon("nodes/methodReference");

	public static Icon getIcon(JavaMethod mth) {
		AccessInfo accessFlags = mth.getAccessFlags();
		Icon icon = Icons.METHOD;
		if (accessFlags.isAbstract()) {
			icon = ICON_METHOD_ABSTRACT;
		}
		if (accessFlags.isConstructor()) {
			icon = ICON_METHOD_CONSTRUCTOR;
		}
		if (accessFlags.isPublic()) {
			icon = ICON_METHOD_PUBLIC;
		}
		if (accessFlags.isPrivate()) {
			icon = ICON_METHOD_PRIVATE;
		}
		if (accessFlags.isProtected()) {
			icon = ICON_METHOD_PROTECTED;
		}
		if (accessFlags.isSynchronized()) {
			icon = ICON_METHOD_SYNC;
		}

		OverlayIcon overIcon = new OverlayIcon(icon);
		if (accessFlags.isFinal()) {
			overIcon.add(Icons.FINAL);
		}
		if (accessFlags.isStatic()) {
			overIcon.add(Icons.STATIC);
		}

		return overIcon;
	}

	public static String makeBaseString(JavaMethod mth) {
		if (mth.isClassInit()) {
			return "{...}";
		}
		StringBuilder base = new StringBuilder();
		if (mth.isConstructor()) {
			base.append(mth.getDeclaringClass().getName());
		} else {
			base.append(mth.getName());
		}
		base.append('(');
		for (Iterator<ArgType> it = mth.getArguments().iterator(); it.hasNext();) {
			base.append(UiUtils.typeStr(it.next()));
			if (it.hasNext()) {
				base.append(", ");
			}
		}
		base.append(')');
		return base.toString();
	}
}
