package jadx.gui.treemodel;

import jadx.api.JavaClass;
import jadx.api.JavaField;
import jadx.api.JavaMethod;
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
	private JClass jParrent;

	public JClass(JavaClass cls) {
		this.cls = cls;
	}

	public JavaClass getCls() {
		return cls;
	}

	@Override
	public void updateChilds() {
		JClass currentParent = jParrent == null ? this : jParrent;
		for (JavaClass javaClass : cls.getInnerClasses()) {
			JClass child = new JClass(javaClass);
			child.setJParent(currentParent);
			child.updateChilds();
			add(child);
		}
		for (JavaField f : cls.getFields()) {
			add(new JField(f, currentParent));
		}
		for (JavaMethod m : cls.getMethods()) {
			add(new JMethod(m, currentParent));
		}
	}

	public String getCode(){
		return cls.getCode();
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

	public void setJParent(JClass parent) {
		this.jParrent = parent;
	}

	@Override
	public JClass getJParent() {
		return jParrent;
	}

	@Override
	public int getLine() {
		return cls.getDecompiledLine();
	}

	@Override
	public String toString() {
		return cls.getShortName();
	}
}
