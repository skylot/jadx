package jadx.api.plugins.pass;

import java.util.List;

public interface JadxPassInfo {

	/**
	 * Add this to 'run after' list to place pass before others
	 */
	String START = "start";

	/**
	 * Add this to 'run before' list to place pass at end
	 */
	String END = "end";

	/**
	 * Pass short id, should be unique.
	 */
	String getName();

	/**
	 * Pass description
	 */
	String getDescription();

	/**
	 * This pass will be executed after these passes.
	 * Passes names list.
	 */
	List<String> runAfter();

	/**
	 * This pass will be executed before these passes.
	 * Passes names list.
	 */
	List<String> runBefore();
}
