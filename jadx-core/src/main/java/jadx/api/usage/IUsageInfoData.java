package jadx.api.usage;

import jadx.core.dex.nodes.ClassNode;

public interface IUsageInfoData {

	void apply();

	void applyForClass(ClassNode cls);

	void visitUsageData(IUsageInfoVisitor visitor);
}
