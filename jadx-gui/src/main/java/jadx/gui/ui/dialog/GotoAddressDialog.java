package jadx.gui.ui.dialog;

import javax.swing.JOptionPane;

import org.exbin.bined.CodeAreaCaretPosition;
import org.exbin.bined.swing.basic.CodeArea;

public class GotoAddressDialog {

	public void showSetSelectionDialog(CodeArea codeArea, String title) {
		Object o = JOptionPane.showInputDialog(
				codeArea, "Enter address range:", title,
				JOptionPane.QUESTION_MESSAGE, null, null,
				Long.toHexString(codeArea.getDataPosition()));
		if (o != null) {
			codeArea.setCaretPosition(Long.parseLong(o.toString(), 16));
			codeArea.revealCursor();
		}
	}
}
