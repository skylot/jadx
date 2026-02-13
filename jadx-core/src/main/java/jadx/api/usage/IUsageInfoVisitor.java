package jadx.api.usage;

import java.util.List;

import jadx.api.plugins.input.data.IMethodRef;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;

public interface IUsageInfoVisitor {

	void visitClassDeps(ClassNode cls, List<ClassNode> deps);

	void visitClassUsage(ClassNode cls, List<ClassNode> usage);

	void visitClassUseInMethods(ClassNode cls, List<MethodNode> methods);

	void visitFieldsUsage(FieldNode fld, List<MethodNode> methods);

	void visitMethodsUsage(MethodNode mth, List<MethodNode> methods);

	void visitMethodsUses(MethodNode mth, List<MethodNode> methods);

	void visitUnresolvedMethodsUsage(MethodNode mth, List<IMethodRef> methods);

	void visitIsSelfCall(MethodNode mth, boolean isSelfCall);

	void visitComplete();
}
