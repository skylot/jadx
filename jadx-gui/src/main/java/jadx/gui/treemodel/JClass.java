package jadx.gui.treemodel;

import jadx.api.JavaClass;
import jadx.core.dex.info.AccessInfo;
import jadx.gui.Utils;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.tree.DefaultMutableTreeNode;

public class JClass extends DefaultMutableTreeNode implements JNode {

	private static final ImageIcon ICON_CLASS = Utils.openIcon("class_obj");
	private static final ImageIcon ICON_CLASS_DEFAULT = Utils.openIcon("class_default_obj");
	private static final ImageIcon ICON_CLASS_PRIVATE = Utils.openIcon("innerclass_private_obj");
	private static final ImageIcon ICON_CLASS_PROTECTED = Utils.openIcon("innerclass_protected_obj");
	private static final ImageIcon ICON_INTERFACE = Utils.openIcon("int_obj");
	private static final ImageIcon ICON_ENUM = Utils.openIcon("enum_obj");
	private static final ImageIcon ICON_ANNOTATION = Utils.openIcon("annotation_obj");

	private final JavaClass cls;

	public JClass(JavaClass cls) {
		this.cls = cls;
		updateChilds();
	}

	public JavaClass getCls() {
		return cls;
	}

	@Override
	public void updateChilds() {
//		for (JavaClass javaClass : cls.getInnerClasses()) {
//			add(new JClass(javaClass));
//		}
	}

	@Override
	public Icon getIcon() {
		AccessInfo accessInfo = cls.getAccessInfo();

		if (accessInfo.isEnum()) {
			return ICON_ENUM;
		} else if (accessInfo.isAnnotation()) {
			return ICON_ANNOTATION;
		} else if (accessInfo.isInterface()) {
			return ICON_INTERFACE;
		} else if (accessInfo.isProtected()) {
			return ICON_CLASS_PROTECTED;
		} else if (accessInfo.isPrivate()) {
			return ICON_CLASS_PRIVATE;
		} else if (accessInfo.isPublic()) {
			return ICON_CLASS;
		} else {
			return ICON_CLASS_DEFAULT;
		}
	}

	@Override
	public String toString() {
		return cls.getShortName();
	}
}
