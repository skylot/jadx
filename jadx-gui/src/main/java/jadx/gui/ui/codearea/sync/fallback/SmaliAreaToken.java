package jadx.gui.ui.codearea.sync.fallback;

import javax.swing.text.BadLocationException;

import jadx.gui.ui.codearea.SmaliArea;

public class SmaliAreaToken extends AbstractCodeAreaToken {
	public SmaliAreaToken(SmaliArea area, int at) throws BadLocationException, FallbackSyncException {
		super(area, at);
	}

	@Override
	public boolean isFieldReference() throws BadLocationException {
		return area.getText(this.startPos - 2, 2).equals("->");
	}

	@Override
	public boolean isClassField() throws BadLocationException {
		AbstractCodeAreaLine line = this.getLine();
		boolean startsWithField = line.isFieldDeclaration();
		if (startsWithField) {
			String tokenStr = getStr();
			String trimmedLine = line.getTrimmedStr();
			int lineTokenStartPos = trimmedLine.indexOf(tokenStr);
			int lineTokenAfterPos = lineTokenStartPos + this.length;
			for (int i = lineTokenAfterPos; i < trimmedLine.length(); ++i) {
				char c = trimmedLine.charAt(i);
				switch (c) {
					case ' ':
						break;
					case ':':
						return true;
					default:
						return false;
				}
			}
		}
		return false;
	}

	@Override
	public AbstractCodeAreaLine getLine() throws BadLocationException {
		return new SmaliAreaLine((SmaliArea) area, area.getLineOfOffset(getAtPos()));
	}
}
