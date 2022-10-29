package jadx.gui.plugins.script;

import javax.swing.JList;

import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.CompletionCellRenderer;

import jadx.gui.settings.JadxSettings;

import static jadx.gui.utils.UiUtils.escapeHtml;
import static jadx.gui.utils.UiUtils.fadeHtml;
import static jadx.gui.utils.UiUtils.wrapHtml;

public class ScriptCompletionRenderer extends CompletionCellRenderer {

	public ScriptCompletionRenderer(JadxSettings settings) {
		setDisplayFont(settings.getFont());
	}

	@Override
	protected void prepareForOtherCompletion(JList list, Completion c, int index, boolean selected, boolean hasFocus) {
		ScriptCompletionData cmpl = (ScriptCompletionData) c;
		setText(wrapHtml(escapeHtml(cmpl.getInputText()) + "  "
				+ fadeHtml(escapeHtml(cmpl.getSummary()))));
	}
}
