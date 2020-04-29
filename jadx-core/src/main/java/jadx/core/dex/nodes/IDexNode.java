package jadx.core.dex.nodes;

import java.nio.file.Path;

public interface IDexNode {

	String typeName();

	RootNode root();

	Path getInputPath();
}
