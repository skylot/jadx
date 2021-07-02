package jadx.gui.treemodel;

import javax.swing.*;

import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.jetbrains.annotations.Nullable;

import jadx.api.ICodeInfo;
import jadx.api.JavaClass;
import jadx.api.JavaField;
import jadx.api.JavaMethod;
import jadx.api.JavaNode;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.info.AccessInfo;
import jadx.gui.ui.ContentPanel;
import jadx.gui.ui.TabbedPane;
import jadx.gui.ui.codearea.ClassCodeContentPanel;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;

public class JClass extends JLoadableNode {
	private static final long serialVersionUID = -1239986875244097177L;

	private static final ImageIcon ICON_CLASS = UiUtils.openIcon("class_obj");
	private static final ImageIcon ICON_CLASS_DEFAULT = UiUtils.openIcon("class_default_obj");
	private static final ImageIcon ICON_CLASS_PRIVATE = UiUtils.openIcon("innerclass_private_obj");
	private static final ImageIcon ICON_CLASS_PROTECTED = UiUtils.openIcon("innerclass_protected_obj");
	private static final ImageIcon ICON_INTERFACE = UiUtils.openIcon("int_obj");
	private static final ImageIcon ICON_ENUM = UiUtils.openIcon("enum_obj");
	private static final ImageIcon ICON_ANNOTATION = UiUtils.openIcon("annotation_obj");

	private final transient JavaClass cls;
	private final transient JClass jParent;
	private transient boolean loaded;

	public JClass(JavaClass cls) {
		this.cls = cls;
		this.jParent = null;
		this.loaded = false;
	}

	public JClass(JavaClass cls, JClass parent) {
		this.cls = cls;
		this.jParent = parent;
		this.loaded = true;
	}

	public JavaClass getCls() {
		return cls;
	}

	@Override
	public void loadNode() {
		getRootClass().load();
	}

	@Override
	public boolean canRename() {
		return !cls.getClassNode().contains(AFlag.DONT_RENAME);
	}

	public synchronized void load() {
		if (loaded) {
			return;
		}
		cls.decompile();
		loaded = true;
		update();
	}

	public synchronized void reload() {
		cls.reload();
		loaded = true;
		update();
	}

	public synchronized void update() {
		removeAllChildren();
		if (!loaded) {
			add(new TextNode(NLS.str("tree.loading")));
		} else {
			for (JavaClass javaClass : cls.getInnerClasses()) {
				JClass innerCls = new JClass(javaClass, this);
				add(innerCls);
				innerCls.update();
			}
			for (JavaField f : cls.getFields()) {
				add(new JField(f, this));
			}
			for (JavaMethod m : cls.getMethods()) {
				add(new JMethod(m, this));
			}
		}
	}

	@Override
	public @Nullable ICodeInfo getCodeInfo() {
		return cls.getCodeInfo();
	}

	@Override
	public String getContent() {
		return cls.getCode();
	}

	@Override
	public ContentPanel getContentPanel(TabbedPane tabbedPane) {
		return new ClassCodeContentPanel(tabbedPane, this);
	}

	@Override
	public String getSmali() {
		return cls.getSmali();
	}

	@Override
	public String getSyntaxName() {
		return SyntaxConstants.SYNTAX_STYLE_JAVA;
	}

	@Override
	public Icon getIcon() {
		AccessInfo accessInfo = cls.getAccessInfo();
		if (accessInfo.isEnum()) {
			return ICON_ENUM;
		}
		if (accessInfo.isAnnotation()) {
			return ICON_ANNOTATION;
		}
		if (accessInfo.isInterface()) {
			return ICON_INTERFACE;
		}
		if (accessInfo.isProtected()) {
			return ICON_CLASS_PROTECTED;
		}
		if (accessInfo.isPrivate()) {
			return ICON_CLASS_PRIVATE;
		}
		if (accessInfo.isPublic()) {
			return ICON_CLASS;
		}
		return ICON_CLASS_DEFAULT;
	}

	@Override
	public JavaNode getJavaNode() {
		return cls;
	}

	@Override
	public JClass getJParent() {
		return jParent;
	}

	@Override
	public JClass getRootClass() {
		if (jParent == null) {
			return this;
		}
		return jParent.getRootClass();
	}

	@Override
	public String getName() {
		return cls.getName();
	}

	public String getFullName() {
		return cls.getFullName();
	}

	@Override
	public int getLine() {
		return cls.getDecompiledLine();
	}

	@Override
	public int hashCode() {
		return cls.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || obj instanceof JClass && cls.equals(((JClass) obj).cls);
	}

	@Override
	public String makeString() {
		return cls.getName();
	}

	@Override
	public String makeLongString() {
		return cls.getFullName();
	}
}
