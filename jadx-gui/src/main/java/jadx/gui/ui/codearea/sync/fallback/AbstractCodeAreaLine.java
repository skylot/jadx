package jadx.gui.ui.codearea.sync.fallback;

import javax.swing.text.BadLocationException;

import org.jetbrains.annotations.Nullable;

import jadx.gui.ui.codearea.AbstractCodeArea;

abstract class AbstractCodeAreaLine {
	private final AbstractCodeArea area;
	private final int lineIndex;
	private final String line;

	protected AbstractCodeAreaLine(AbstractCodeArea area, int lineIndex) throws BadLocationException {
		this.area = area;
		this.lineIndex = lineIndex;
		this.line = this.area.getText().split("\\R")[lineIndex];
	}

	public AbstractCodeArea getArea() {
		return area;
	}

	public int getLineIndex() {
		return lineIndex;
	}

	public String getStr() {
		return line;
	}

	public String getTrimmedStr() {
		return line.trim();
	}

	public abstract AbstractCodeAreaLine getLineAt(int lineIndex) throws BadLocationException;

	public abstract boolean isClassDeclaration();

	public abstract boolean isMethodOrConstructorDeclaration();

	public abstract boolean isFieldDeclaration();

	@Nullable
	public abstract String extractDeclaredMethodName();

	@Nullable
	public abstract String extractDeclaredClassName();

	protected abstract MethodDeclaration createMethodDeclaration() throws FallbackSyncException;

	/**
	 * This could be itself or:
	 * - the enclosing method delcaration if line is in a method
	 * - the enclosing class declaration if line is a field declaration
	 */
	public IDeclaration getEnclosingScopeDeclaration() throws BadLocationException, FallbackSyncException {
		IDeclaration decl = this.getDeclaration();
		if (decl != null) {
			return decl;
		}
		for (int i = lineIndex - 1; i >= 0; i--) {
			AbstractCodeAreaLine line = getLineAt(i);
			boolean enclosingDecl = line.isScopeDeclarationLine();
			if (enclosingDecl) {
				return line.getDeclaration();
			}
		}
		throw new FallbackSyncException("No enclosing declaration found for " + this);
	}

	public boolean isScopeDeclarationLine() {
		return isClassDeclaration() || isMethodOrConstructorDeclaration();
	}

	public boolean isDeclarationLine() {
		return isScopeDeclarationLine() || isFieldDeclaration();
	}

	@Nullable
	public IDeclaration getDeclaration() throws FallbackSyncException {
		if (isClassDeclaration()) {
			return new ClassDeclaration(this);
		}
		if (isMethodOrConstructorDeclaration()) {
			return createMethodDeclaration();
		}
		return null;
	}

	@Override
	public String toString() {
		return line;
	}
}
