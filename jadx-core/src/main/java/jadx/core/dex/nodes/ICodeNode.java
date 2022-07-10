package jadx.core.dex.nodes;

import jadx.api.metadata.ICodeNodeRef;
import jadx.core.dex.attributes.IAttributeNode;
import jadx.core.dex.info.AccessInfo;

public interface ICodeNode extends IDexNode, IAttributeNode, IUsageInfoNode, ICodeNodeRef {
	AccessInfo getAccessFlags();

	void setAccessFlags(AccessInfo newAccessFlags);
}
