package jadx.gui.ui.codearea;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import jadx.core.utils.GsonUtils;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.action.ActionModel;

public class JsonPrettifyAction extends JNodeAction {
	private static final long serialVersionUID = -2682529369671695550L;

	private static final Gson GSON = GsonUtils.buildGson();

	public JsonPrettifyAction(CodeArea codeArea) {
		super(ActionModel.JSON_PRETTIFY, codeArea);
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
