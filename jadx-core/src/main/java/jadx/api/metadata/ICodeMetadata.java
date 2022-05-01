package jadx.api.metadata;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import jadx.api.metadata.impl.CodeMetadataStorage;

public interface ICodeMetadata {

	ICodeMetadata EMPTY = CodeMetadataStorage.empty();

	@Nullable
	ICodeAnnotation getAt(int position);

	@Nullable
	ICodeAnnotation getClosestUp(int position);

	/**
	 * Get current node at position (can be enclosing class or method)
	 */
	@Nullable
	ICodeNodeRef getNodeAt(int position);

	/**
	 * Any definition of class or method below position
	 */
	@Nullable
	ICodeNodeRef getNodeBelow(int position);

	Map<Integer, ICodeAnnotation> getAsMap();

	Map<Integer, Integer> getLineMapping();
}
