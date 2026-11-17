package jadx.gui.ui.codearea.mode;

import javax.swing.Icon;

import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.jetbrains.annotations.Nullable;

import jadx.api.DecompilationMode;
import jadx.api.ICodeInfo;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;

public class JCodeMode extends JNode {

	private final JClass jCls;
	private final DecompilationMode mode;

	private @Nullable ICodeInfo codeInfo;

	public JCodeMode(JClass jClass, DecompilationMode mode) {
		this.jCls = jClass;
		this.mode = mode;
	}

	@Override
	public JClass getJParent() {
		return jCls.getJParent();
	}

	@Override
	public Icon getIcon() {
		return jCls.getIcon();
	}

	@Override
	public String makeString() {
		return jCls.makeString();
	}

	@Override
	public ICodeInfo getCodeInfo() {
		if (codeInfo != null) {
			return codeInfo;
		}
		codeInfo = jCls.getCls().decompileWithMode(mode);
		return codeInfo;
	}

	@Override
	public String getSyntaxName() {
		return SyntaxConstants.SYNTAX_STYLE_JAVA;
	}

	@Override
	public String getName() {
		return jCls.getName();
	}
}
