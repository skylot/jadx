package jadx.gui.ui.codearea;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import jadx.gui.treemodel.JNode;
import jadx.gui.utils.NLS;

public class JsonPrettifyAction extends JNodeAction {

	private static final long serialVersionUID = -2682529369671695550L;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public JsonPrettifyAction(CodeArea codeArea) {
		super(NLS.str("popup.json_prettify"), codeArea);
	}

	@Override
	public void runAction(JNode node) {
		String originString = getCodeArea().getCodeInfo().getCodeStr();
		JsonElement je = JsonParser.parseString(originString);
		String prettyString = GSON.toJson(je);
		getCodeArea().setText(prettyString);
	}

	@Override
	public boolean isActionEnabled(JNode node) {
		return true;
	}
}
