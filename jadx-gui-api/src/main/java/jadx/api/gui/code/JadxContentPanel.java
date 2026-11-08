package jadx.api.gui.code;

import java.util.Objects;

import javax.swing.JPanel;

import org.jetbrains.annotations.Nullable;

import jadx.api.gui.tree.ITreeNode;

/**
 * Base implementation of content panel:
 * shown in base tabbed pane and open on tree node click
 */
public abstract class JadxContentPanel extends JPanel {

	private final ITreeNode node;

	public JadxContentPanel(ITreeNode node) {
		this.node = Objects.requireNonNull(node);
	}

	/**
	 * Related tree node
	 */
	public ITreeNode getNode() {
		return node;
	}

	/**
	 * Method called on settings update to apply new values
	 */
	public void loadSettings() {
	}

	public @Nullable String getTooltip() {
		return null;
	}

	/**
	 * Method called on panel dispose,
	 * to prevent memory leaks make sure to free all:
	 * - resources
	 * - listeners
	 * - references to nodes or decompiler
	 */
	public void dispose() {
	}
}
