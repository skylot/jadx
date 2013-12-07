package jadx.core.dex.nodes;

import jadx.core.utils.exceptions.DecodeException;

public interface ILoadable {

	/**
	 * On demand loading
	 *
	 * @throws DecodeException
	 */
	void load() throws DecodeException;

	/**
	 * Free resources
	 */
	void unload();

}
