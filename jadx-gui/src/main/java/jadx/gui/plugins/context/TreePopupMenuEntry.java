package jadx.gui.plugins.context;

import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.swing.JMenuItem;

import org.jetbrains.annotations.Nullable;

import jadx.api.gui.tree.ITreeNode;

public class TreePopupMenuEntry {
	private final String name;
	private final Predicate<ITreeNode> addPredicate;
	private final Consumer<ITreeNode> action;

	public TreePopupMenuEntry(String name, Predicate<ITreeNode> addPredicate, Consumer<ITreeNode> action) {
		this.name = name;
		this.addPredicate = addPredicate;
		this.action = action;
	}

	public @Nullable JMenuItem buildEntry(ITreeNode node) {
		if (!addPredicate.test(node)) {
			return null;
		}
		JMenuItem menuItem = new JMenuItem(name);
		menuItem.addActionListener(ev -> action.accept(node));
		return menuItem;
	}
}
