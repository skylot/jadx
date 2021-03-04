package jadx.api.data;

import org.jetbrains.annotations.Nullable;

public interface ICodeComment extends Comparable<ICodeComment> {

	IJavaNodeRef getNodeRef();

	String getComment();

	/**
	 * Instruction offset inside method
	 */
	int getOffset();

	enum AttachType {
		VAR_DECLARE
	}

	@Nullable
	AttachType getAttachType();
}
