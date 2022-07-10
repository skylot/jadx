package jadx.gui.search;

import org.jetbrains.annotations.Nullable;

import jadx.gui.jobs.Cancelable;
import jadx.gui.jobs.ITaskProgress;
import jadx.gui.treemodel.JNode;

public interface ISearchProvider extends ITaskProgress {

	/**
	 * Return next result or null if search complete
	 */
	@Nullable
	JNode next(Cancelable cancelable);
}
