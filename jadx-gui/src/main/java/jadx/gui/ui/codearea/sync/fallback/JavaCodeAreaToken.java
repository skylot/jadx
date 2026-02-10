package jadx.gui.ui.codearea.sync.fallback;

import javax.swing.text.BadLocationException;

import jadx.gui.ui.codearea.CodeArea;

public class JavaCodeAreaToken extends AbstractCodeAreaToken {
	public JavaCodeAreaToken(CodeArea area, int at) throws BadLocationException, FallbackSyncException {
		super(area, at);
	}

	@Override
	public boolean isClassField() throws BadLocationException {
		AbstractCodeAreaLine line = getLine();
		if (!line.isFieldDeclaration()) {
			return false;
		}
		// assignment immediately follows the token
		if (line.getStr().contains("=")) {
			return area.getText(this.startPos + this.length, 2).equals(" =");
		}
		// ends with ';'
		return area.getText(this.startPos + this.length, 1).equals(";");
	}

	@Override
	public boolean isFieldReference() throws BadLocationException {
		return area.getText(this.startPos - 5, 5).equals("this.");
	}

	@Override
	public AbstractCodeAreaLine getLine() throws BadLocationException {
		return new JavaCodeAreaLine((CodeArea) area, area.getLineOfOffset(getAtPos()));
	}
}
