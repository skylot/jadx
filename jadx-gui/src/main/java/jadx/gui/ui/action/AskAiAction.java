package jadx.gui.ui.action;

import javax.swing.JOptionPane;

import jadx.gui.treemodel.JNode;
import jadx.gui.ui.ai.AiAssistantPanel;
import jadx.gui.ui.codearea.CodeArea;
import jadx.gui.ui.dialog.AiAssistantDialog;
import jadx.gui.utils.NLS;

/**
 * Sends the selected code (or the whole current class if nothing is selected)
 * to the AI Assistant panel together with a fixed "explain this code" question.
 */
public final class AskAiAction extends JNodeAction {
	private static final long serialVersionUID = 1L;

	public AskAiAction(CodeArea codeArea) {
		super(ActionModel.ASK_AI, codeArea);
	}

	@Override
	public void runAction(JNode node) {
		CodeArea codeArea = getCodeArea();
		if (!codeArea.getMainWindow().getSettings().getAiSettings().isEnabled()) {
			JOptionPane.showMessageDialog(codeArea.getMainWindow(), NLS.str("ai_assistant.not_enabled"),
					NLS.str("preferences.ai"), JOptionPane.WARNING_MESSAGE);
			return;
		}
		String selected = codeArea.getSelectedText();
		String code = selected != null && !selected.isEmpty() ? selected : codeArea.getText();

		AiAssistantDialog dialog = AiAssistantDialog.open(codeArea.getMainWindow());
		AiAssistantPanel panel = dialog.getPanel();
		panel.askAboutCode(NLS.str("popup.ask_ai_question"), code);
	}
}
