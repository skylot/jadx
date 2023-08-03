package jadx.gui.plugins.script;

import org.fife.ui.autocomplete.AutoCompletion;
import org.jetbrains.annotations.NotNull;

import jadx.api.ICodeInfo;
import jadx.gui.settings.JadxSettings;
import jadx.gui.treemodel.JInputScript;
import jadx.gui.ui.action.JadxAutoCompletion;
import jadx.gui.ui.codearea.AbstractCodeArea;
import jadx.gui.ui.panel.ContentPanel;
import jadx.gui.utils.shortcut.ShortcutsController;

public class ScriptCodeArea extends AbstractCodeArea {

	private final JInputScript scriptNode;
	private final AutoCompletion autoCompletion;
	private final ShortcutsController shortcutsController;

	public ScriptCodeArea(ContentPanel contentPanel, JInputScript node) {
		super(contentPanel, node);
		scriptNode = node;

		setSyntaxEditingStyle(node.getSyntaxName());
		setCodeFoldingEnabled(true);
		setCloseCurlyBraces(true);

		shortcutsController = contentPanel.getTabbedPane().getMainWindow().getShortcutsController();
		JadxSettings settings = contentPanel.getTabbedPane().getMainWindow().getSettings();
		autoCompletion = addAutoComplete(settings);
	}

	private AutoCompletion addAutoComplete(JadxSettings settings) {
		ScriptCompleteProvider provider = new ScriptCompleteProvider(this);
		provider.setAutoActivationRules(false, ".");
		JadxAutoCompletion ac = new JadxAutoCompletion(provider);
		ac.setListCellRenderer(new ScriptCompletionRenderer(settings));
		ac.setAutoActivationEnabled(true);
		ac.setAutoCompleteSingleChoices(true);
		ac.install(this);
		shortcutsController.bindImmediate(ac);
		return ac;
	}

	@Override
	public @NotNull ICodeInfo getCodeInfo() {
		return node.getCodeInfo();
	}

	@Override
	public void load() {
		if (getText().isEmpty()) {
			setText(getCodeInfo().getCodeStr());
			setCaretPosition(0);
			setLoaded();
		}
	}

	@Override
	public void refresh() {
		setText(node.getCodeInfo().getCodeStr());
	}

	public void updateCode(String newCode) {
		int caretPos = getCaretPosition();
		setText(newCode);
		setCaretPosition(caretPos);
		scriptNode.setChanged(true);
	}

	public void save() {
		scriptNode.save(getText());
		scriptNode.setChanged(false);
	}

	public JInputScript getScriptNode() {
		return scriptNode;
	}

	@Override
	public void dispose() {
		shortcutsController.unbindActionsForComponent(this);
		autoCompletion.uninstall();
		super.dispose();
	}
}
