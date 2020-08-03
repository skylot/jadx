package jadx.core.dex.nodes;

import java.util.List;

public interface IUsageInfoNode {
	List<? extends ICodeNode> getUseIn();
}
