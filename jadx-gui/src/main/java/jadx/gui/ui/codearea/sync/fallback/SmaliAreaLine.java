package jadx.gui.ui.codearea.sync.fallback;

import javax.swing.text.BadLocationException;

import org.jetbrains.annotations.Nullable;

import jadx.gui.ui.codearea.SmaliArea;

public class SmaliAreaLine extends AbstractCodeAreaLine {
	public SmaliAreaLine(SmaliArea area, int lineIndex) throws BadLocationException {
		super(area, lineIndex);
	}

	@Override
	public AbstractCodeAreaLine getLineAt(int lineIndex) throws BadLocationException {
		return new SmaliAreaLine((SmaliArea) getArea(), lineIndex);
	}

	@Override
	public boolean isClassDeclaration() {
		return getTrimmedStr().startsWith("Class: ") || getTrimmedStr().startsWith(".class ");
	}

	@Override
	public boolean isMethodOrConstructorDeclaration() {
		return getTrimmedStr().startsWith(".method");
	}

	@Override
	public boolean isFieldDeclaration() {
		return getTrimmedStr().startsWith(".field");
	}

	@Override
	public final @Nullable String extractDeclaredClassName() {
		if (!isClassDeclaration()) {
			return null;
		}
		String[] parts = getTrimmedStr().split("\\s+");
		for (String part : parts) {
			if (part.startsWith("L") && part.endsWith(";")) {
				String fileClassName;
				if (part.contains("/")) {
					fileClassName = part.substring(part.lastIndexOf('/') + 1, part.length() - 1);
				} else {
					fileClassName = part.substring(1, part.length() - 1); // remove leading 'L' and trailing ';'
				}
				if (fileClassName.contains("$")) { // inner class
					return fileClassName.substring(fileClassName.lastIndexOf('$') + 1);
				}
				return fileClassName;
			}
		}
		return null;
	}

	@Override
	public final @Nullable String extractDeclaredMethodName() {
		if (!isMethodOrConstructorDeclaration()) {
			return null;
		}
		int parenIndex = getTrimmedStr().indexOf('(');
		if (parenIndex > 0) {
			String beforeParen = getTrimmedStr().substring(0, parenIndex).trim();
			String[] tokens = beforeParen.split("\\s+");
			return tokens[tokens.length - 1];
		}
		return null;
	}

	@Override
	protected MethodDeclaration createMethodDeclaration() throws FallbackSyncException {
		return MethodDeclaration.create(this);
	}
}
