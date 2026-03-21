package jadx.core.dex.visitors.typeinference;

import org.jetbrains.annotations.Nullable;

/**
 * Callback to process and modify type update result
 */
@FunctionalInterface
public interface ITypeUpdateCallback {

	/**
	 * Called on type update result being calculated
	 *
	 * @param result - type update result
	 * @return modified result, can be null - will keep callback and wait for another result
	 */
	@Nullable
	TypeUpdateResult updateCallback(TypeUpdateResult result);
}
