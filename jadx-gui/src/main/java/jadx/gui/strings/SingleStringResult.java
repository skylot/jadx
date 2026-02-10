package jadx.gui.strings;

import java.util.List;

import javax.swing.Icon;

import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.jetbrains.annotations.Nullable;

import jadx.api.ICodeInfo;
import jadx.api.JavaClass;
import jadx.api.JavaNode;
import jadx.api.JavaPackage;
import jadx.api.resources.ResourceContentType;
import jadx.gui.strings.pkg.PackageFilter;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.panel.ContentPanel;
import jadx.gui.ui.tab.TabbedPane;

public final class SingleStringResult extends StringResult {

	private final JNode underlying;
	private final transient JavaClass sourceClass;

	public SingleStringResult(String foundString, JavaClass sourceClass) {
		this(foundString, sourceClass, null);
	}

	public SingleStringResult(String foundString, JavaClass sourceClass, JNode underlying) {
		super(foundString);

		this.underlying = underlying;
		this.sourceClass = sourceClass;
	}

	@Override
	public boolean isIncludedForPackageFilters(final List<PackageFilter> packageFilters) {
		final JavaPackage pkg = getSourceClass().getJavaPackage();

		boolean matched = false;
		for (final PackageFilter filter : packageFilters) {
			if (filter.doesMatch(pkg)) {
				matched = true;
				break;
			}
		}
		return !matched;
	}

	@Override
	public Icon getIcon() {
		if (underlying == null) {
			return null;
		}

		return underlying.getIcon();
	}

	@Override
	public JClass getJParent() {
		if (underlying == null) {
			return null;
		}

		return underlying.getJParent();
	}

	@Override
	public String makeString() {
		if (underlying == null) {
			return super.makeString();
		}

		return underlying.makeString();
	}

	@Override
	public JavaNode getJavaNode() {
		if (underlying == null) {
			return null;
		}

		return underlying.getJavaNode();
	}

	@Override
	public @Nullable ContentPanel getContentPanel(TabbedPane tabbedPane) {
		if (underlying == null) {
			return null;
		}

		return underlying.getContentPanel(tabbedPane);
	}

	@Override
	public String getSyntaxName() {
		if (underlying == null) {
			return SyntaxConstants.SYNTAX_STYLE_NONE;
		}

		return underlying.getSyntaxName();
	}

	@Override
	public ICodeInfo getCodeInfo() {
		if (underlying == null) {
			return ICodeInfo.EMPTY;
		}

		return underlying.getCodeInfo();
	}

	@Override
	public ResourceContentType getContentType() {
		if (underlying == null) {
			return ResourceContentType.CONTENT_TEXT;
		}

		return underlying.getContentType();
	}

	public JavaClass getSourceClass() {
		return this.sourceClass;
	}

	public JNode getUnderlyingNode() {
		return this.underlying;
	}
}
