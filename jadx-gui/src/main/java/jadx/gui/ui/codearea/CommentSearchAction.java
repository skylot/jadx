package jadx.gui.ui.codearea;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import jadx.gui.ui.dialog.SearchDialog;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;

import static javax.swing.KeyStroke.getKeyStroke;

public class CommentSearchAction extends AbstractAction {
	private static final long serialVersionUID = -3646341661734961590L;

	private final CodeArea codeArea;

	public CommentSearchAction(CodeArea codeArea) {
		super(NLS.str("popup.search_comment") + " (Ctrl + ;)");
		this.codeArea = codeArea;

		KeyStroke key = getKeyStroke(KeyEvent.VK_SEMICOLON, UiUtils.ctrlButton());
		UiUtils.addKeyBinding(codeArea, key, "popup.search_comment", this::startSearch);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		startSearch();
	}

	private void startSearch() {
		SearchDialog.searchInActiveTab(codeArea.getMainWindow(), SearchDialog.SearchPreset.COMMENT);
	}
}
