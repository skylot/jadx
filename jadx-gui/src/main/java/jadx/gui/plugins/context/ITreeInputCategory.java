package jadx.gui.plugins.context;

import java.nio.file.Path;
import java.util.List;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import jadx.gui.treemodel.JNode;

/**
 * Custom category for 'Inputs' tree section
 */
@ApiStatus.Experimental
public interface ITreeInputCategory {

	/**
	 * Check if file should be moved into this category
	 */
	boolean filesFilter(Path file);

	/**
	 * Build node for filtered files.
	 * Can be called with empty list (empty category might be useful)
	 *
	 * @return category node or null if not needed
	 */
	@Nullable
	JNode buildInputNode(List<Path> files);
}
