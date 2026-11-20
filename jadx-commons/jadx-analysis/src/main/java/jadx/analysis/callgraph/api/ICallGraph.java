package jadx.analysis.callgraph.api;

import java.nio.file.Path;
import java.util.List;

public interface ICallGraph {

	List<ICallGraphEdge> edges();

	default boolean isUseAlias() {
		return false;
	}

	void writeDot(Path path);

	void writeJson(Path path);
}
