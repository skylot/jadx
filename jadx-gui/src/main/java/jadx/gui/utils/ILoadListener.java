package jadx.gui.utils;

public interface ILoadListener {

	/**
	 * Update files/project loaded state
	 *
	 * @return true to remove listener
	 */
	boolean update(boolean loaded);
}
