package jadx.gui.treemodel;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;

import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.jetbrains.annotations.NotNull;

import jadx.api.ICodeInfo;
import jadx.api.JavaClass;
import jadx.api.JavaField;
import jadx.api.JavaMethod;
import jadx.api.JavaNode;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.info.AccessInfo;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.TabbedPane;
import jadx.gui.ui.codearea.ClassCodeContentPanel;
import jadx.gui.ui.dialog.RenameDialog;
import jadx.gui.ui.panel.ContentPanel;
import jadx.gui.utils.CacheObject;
import jadx.gui.utils.Icons;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;

public class JClass extends JLoadableNode {
	private static final long serialVersionUID = -1239986875244097177L;

	private static final ImageIcon ICON_CLASS_ABSTRACT = UiUtils.openSvgIcon("nodes/abstractClass");
	private static final ImageIcon ICON_CLASS_PUBLIC = UiUtils.openSvgIcon("nodes/publicClass");
	private static final ImageIcon ICON_CLASS_PRIVATE = UiUtils.openSvgIcon("nodes/privateClass");
	private static final ImageIcon ICON_CLASS_PROTECTED = UiUtils.openSvgIcon("nodes/protectedClass");
	private static final ImageIcon ICON_INTERFACE = UiUtils.openSvgIcon("nodes/interface");
	private static final ImageIcon ICON_ENUM = UiUtils.openSvgIcon("nodes/enum");
	private static final ImageIcon ICON_ANNOTATION = UiUtils.openSvgIcon("nodes/annotationtype");

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

	private synchronized void load() {
		if (loaded) {
			return;
		}
		cls.decompile();
		loaded = true;
		update();
	}

	public synchronized ICodeInfo reload(CacheObject cache) {
		cache.getNodeCache().removeWholeClass(cls);
		ICodeInfo codeInfo = cls.reload();
		loaded = true;
		update();
		return codeInfo;
	}

	public synchronized void unload(CacheObject cache) {
		cache.getNodeCache().removeWholeClass(cls);
		cls.unload();
		loaded = false;
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
	public ICodeInfo getCodeInfo() {
		return cls.getCodeInfo();
	}

	@Override
	public ContentPanel getContentPanel(TabbedPane tabbedPane) {
		return new ClassCodeContentPanel(tabbedPane, this);
	}

	public String getSmali() {
		return cls.getSmali();
	}

	@Override
	public String getSyntaxName() {
		return SyntaxConstants.SYNTAX_STYLE_JAVA;
	}

	@Override
	public JPopupMenu onTreePopupMenu(MainWindow mainWindow) {
		return RenameDialog.buildRenamePopup(mainWindow, this);
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
		if (accessInfo.isAbstract()) {
			return ICON_CLASS_ABSTRACT;
		}
		if (accessInfo.isProtected()) {
			return ICON_CLASS_PROTECTED;
		}
		if (accessInfo.isPrivate()) {
			return ICON_CLASS_PRIVATE;
		}
		if (accessInfo.isPublic()) {
			return ICON_CLASS_PUBLIC;
		}
		return Icons.CLASS;
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

	public int compareToCls(@NotNull JClass otherCls) {
		return this.getCls().getRawName().compareTo(otherCls.getCls().getRawName());
	}

	@Override
	public int compareTo(@NotNull JNode other) {
		if (other instanceof JClass) {
			return compareToCls((JClass) other);
		}
		if (other instanceof JMethod) {
			int cmp = compareToCls(other.getJParent());
			if (cmp != 0) {
				return cmp;
			}
			return -1;
		}
		return super.compareTo(other);
	}
}
