package jadx.gui.plugins.context;

import java.util.function.Consumer;
import java.util.function.Function;

import javax.swing.KeyStroke;

import org.jetbrains.annotations.Nullable;

import jadx.api.metadata.ICodeNodeRef;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.codearea.CodeArea;
import jadx.gui.ui.codearea.JNodeAction;

public class CodePopupAction {
	private final String name;
	private final Function<ICodeNodeRef, Boolean> enabledCheck;
	private final String keyBinding;
	private final Consumer<ICodeNodeRef> action;

	public CodePopupAction(String name, Function<ICodeNodeRef, Boolean> enabled, String keyBinding, Consumer<ICodeNodeRef> action) {
		this.name = name;
		this.enabledCheck = enabled;
		this.keyBinding = keyBinding;
		this.action = action;
	}

	public JNodeAction buildAction(CodeArea codeArea) {
		return new NodeAction(this, codeArea);
	}

	private static class NodeAction extends JNodeAction {
		private final CodePopupAction data;

		public NodeAction(CodePopupAction data, CodeArea codeArea) {
			super(data.name, codeArea);
			if (data.keyBinding != null) {
				KeyStroke key = KeyStroke.getKeyStroke(data.keyBinding);
				if (key == null) {
					throw new IllegalArgumentException("Failed to parse key stroke: " + data.keyBinding);
				}
				addKeyBinding(key, data.name);
			}
			this.data = data;
		}

		@Override
		public boolean isActionEnabled(@Nullable JNode node) {
			if (node == null) {
				return false;
			}
			ICodeNodeRef codeNode = node.getCodeNodeRef();
			if (codeNode == null) {
				return false;
			}
			return data.enabledCheck.apply(codeNode);
		}

		@Override
		public void runAction(JNode node) {
			Runnable r = () -> data.action.accept(node.getCodeNodeRef());
			getCodeArea().getMainWindow().getBackgroundExecutor().execute(data.name, r);
		}
	}
}
