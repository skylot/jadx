package jadx.gui.ui.action;

import java.awt.event.ActionEvent;

import jadx.gui.ui.codearea.CodeArea;
import jadx.gui.ui.dialog.SearchDialog;

public class CommentSearchAction extends CodeAreaAction {
	private static final long serialVersionUID = -3646341661734961590L;

	public CommentSearchAction(CodeArea codeArea) {
		super(ActionModel.CODE_COMMENT_SEARCH, codeArea);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		startSearch();
	}

	private void startSearch() {
		SearchDialog.searchInActiveTab(codeArea.getMainWindow(), SearchDialog.SearchPreset.COMMENT);
	}
}
