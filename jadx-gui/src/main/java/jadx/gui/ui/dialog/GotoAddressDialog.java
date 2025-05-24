package jadx.gui.ui.dialog;

import javax.swing.JOptionPane;

import org.exbin.bined.swing.section.SectCodeArea;

import jadx.gui.utils.HexUtils;
import jadx.gui.utils.NLS;

public class GotoAddressDialog {

	public void showSetSelectionDialog(SectCodeArea codeArea) {
		Object o = JOptionPane.showInputDialog(
				codeArea, NLS.str("hex_viewer.enter_address"), NLS.str("hex_viewer.goto_address"),
				JOptionPane.QUESTION_MESSAGE, null, null,
				Long.toHexString(codeArea.getDataPosition()));
		if (o != null) {
			boolean isValidAddress = HexUtils.isValidHexString(toString());
			if (!isValidAddress) {
				return;
			}

			codeArea.setActiveCaretPosition(Long.parseLong(o.toString(), 16));
			codeArea.validateCaret();
			codeArea.revealCursor();
		}
	}
}
