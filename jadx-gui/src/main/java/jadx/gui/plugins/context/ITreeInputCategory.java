package jadx.gui.plugins.context;

import java.nio.file.Path;
import java.util.List;

import org.jetbrains.annotations.ApiStatus;

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
	 * Build node for filtered files
	 */
	JNode buildInputNode(List<Path> files);
}
