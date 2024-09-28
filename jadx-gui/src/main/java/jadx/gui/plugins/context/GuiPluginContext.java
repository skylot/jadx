package jadx.gui.plugins.context;

import java.awt.Container;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;
import jadx.api.JavaNode;
import jadx.api.metadata.ICodeNodeRef;
import jadx.api.plugins.events.IJadxEvents;
import jadx.api.plugins.events.types.NodeRenamedByUser;
import jadx.api.plugins.gui.ISettingsGroup;
import jadx.api.plugins.gui.JadxGuiContext;
import jadx.api.plugins.gui.JadxGuiSettings;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.plugins.PluginContext;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.codearea.AbstractCodeArea;
import jadx.gui.ui.codearea.AbstractCodeContentPanel;
import jadx.gui.ui.codearea.CodeArea;
import jadx.gui.ui.panel.ContentPanel;
import jadx.gui.utils.JNodeCache;
import jadx.gui.utils.UiUtils;

public class GuiPluginContext implements JadxGuiContext {
	private static final Logger LOG = LoggerFactory.getLogger(GuiPluginContext.class);

	private final CommonGuiPluginsContext commonContext;
	private final PluginContext pluginContext;

	private @Nullable ISettingsGroup customSettingsGroup;

	public GuiPluginContext(CommonGuiPluginsContext commonContext, PluginContext pluginContext) {
		this.commonContext = commonContext;
		this.pluginContext = pluginContext;
	}

	public CommonGuiPluginsContext getCommonContext() {
		return commonContext;
	}

	public PluginContext getPluginContext() {
		return pluginContext;
	}

	@Override
	public JFrame getMainFrame() {
		return commonContext.getMainWindow();
	}

	@Override
	public void uiRun(Runnable runnable) {
		UiUtils.uiRun(runnable);
	}

	@Override
	public void addMenuAction(String name, Runnable action) {
		commonContext.addMenuAction(name, action);
	}

	@Override
	public void addPopupMenuAction(String name, @Nullable Function<ICodeNodeRef, Boolean> enabled,
			@Nullable String keyBinding, Consumer<ICodeNodeRef> action) {
		commonContext.getCodePopupActionList().add(new CodePopupAction(name, enabled, keyBinding, action));
	}

	@Override
	public boolean registerGlobalKeyBinding(String id, String keyBinding, Runnable action) {
		KeyStroke keyStroke = KeyStroke.getKeyStroke(keyBinding);
		if (keyStroke == null) {
			throw new IllegalArgumentException("Failed to parse key binding: " + keyBinding);
		}
		JPanel mainPanel = (JPanel) commonContext.getMainWindow().getContentPane();
		Object prevBinding = mainPanel.getInputMap().get(keyStroke);
		if (prevBinding != null) {
			return false;
		}
		UiUtils.addKeyBinding(mainPanel, keyStroke, id, action);
		return true;
	}

	@Override
	public void copyToClipboard(String str) {
		UiUtils.copyToClipboard(str);
	}

	@Override
	public JadxGuiSettings settings() {
		return new GuiSettingsContext(this);
	}

	void setCustomSettings(ISettingsGroup customSettingsGroup) {
		this.customSettingsGroup = customSettingsGroup;
	}

	public @Nullable ISettingsGroup getCustomSettingsGroup() {
		return customSettingsGroup;
	}

	@Nullable
	private CodeArea getCodeArea() {
		Container contentPane = commonContext.getMainWindow().getTabbedPane().getSelectedContentPanel();
		if (contentPane instanceof AbstractCodeContentPanel) {
			AbstractCodeArea codeArea = ((AbstractCodeContentPanel) contentPane).getCodeArea();
			if (codeArea instanceof CodeArea) {
				return (CodeArea) codeArea;
			}
		}
		return null;
	}

	@Override
	public ICodeNodeRef getNodeUnderCaret() {
		CodeArea codeArea = getCodeArea();
		if (codeArea != null) {
			JNode nodeUnderCaret = codeArea.getNodeUnderCaret();
			if (nodeUnderCaret != null) {
				return nodeUnderCaret.getCodeNodeRef();
			}
		}
		return null;
	}

	@Override
	public ICodeNodeRef getNodeUnderMouse() {
		CodeArea codeArea = getCodeArea();
		if (codeArea != null) {
			JNode nodeUnderMouse = codeArea.getNodeUnderMouse();
			if (nodeUnderMouse != null) {
				return nodeUnderMouse.getCodeNodeRef();
			}
		}
		return null;
	}

	@Override
	public ICodeNodeRef getEnclosingNodeUnderCaret() {
		CodeArea codeArea = getCodeArea();
		if (codeArea != null) {
			JNode nodeUnderMouse = codeArea.getEnclosingNodeUnderCaret();
			if (nodeUnderMouse != null) {
				return nodeUnderMouse.getCodeNodeRef();
			}
		}
		return null;
	}

	@Override
	public ICodeNodeRef getEnclosingNodeUnderMouse() {
		CodeArea codeArea = getCodeArea();
		if (codeArea != null) {
			JNode nodeUnderMouse = codeArea.getEnclosingNodeUnderMouse();
			if (nodeUnderMouse != null) {
				return nodeUnderMouse.getCodeNodeRef();
			}
		}
		return null;
	}

	@Override
	public boolean open(ICodeNodeRef ref) {
		JNodeCache cache = commonContext.getMainWindow().getWrapper().getCache().getNodeCache();
		JNode node;
		if (ref instanceof ClassNode) {
			node = cache.makeFrom(((ClassNode) ref).getJavaNode());
		} else if (ref instanceof MethodNode) {
			node = cache.makeFrom(((MethodNode) ref).getJavaNode());
		} else if (ref instanceof FieldNode) {
			node = cache.makeFrom(((FieldNode) ref).getJavaNode());
		} else {
			// Package node - cannot jump to it
			// TODO: Var node - might be possible
			return false;
		}

		commonContext.getMainWindow().getTabsController().codeJump(node);
		return true;
	}

	@Override
	public void reloadActiveTab() {
		UiUtils.uiRun(() -> {
			CodeArea codeArea = getCodeArea();
			if (codeArea != null) {
				codeArea.refreshClass();
			}
		});
	}

	@Override
	public void reloadAllTabs() {
		UiUtils.uiRun(() -> {
			for (ContentPanel contentPane : commonContext.getMainWindow().getTabbedPane().getTabs()) {
				if (contentPane instanceof AbstractCodeContentPanel) {
					AbstractCodeArea codeArea = ((AbstractCodeContentPanel) contentPane).getCodeArea();
					if (codeArea instanceof CodeArea) {
						((CodeArea) codeArea).refreshClass();
					}
				}
			}
		});
	}

	@Override
	public void applyNodeRename(ICodeNodeRef nodeRef) {
		JadxDecompiler decompiler = commonContext.getMainWindow().getWrapper().getDecompiler();
		JavaNode javaNode = decompiler.getJavaNodeByRef(nodeRef);
		if (javaNode == null) {
			throw new JadxRuntimeException("Failed to resolve node ref: " + nodeRef);
		}
		String newName;
		if (javaNode instanceof JavaClass) {
			// package can have alias
			newName = javaNode.getFullName();
		} else {
			newName = javaNode.getName();
		}
		IJadxEvents events = commonContext.getMainWindow().events();
		events.send(new NodeRenamedByUser(nodeRef, "", newName));
	}
}
