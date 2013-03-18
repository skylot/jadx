package jadx.dex.nodes;

import jadx.utils.exceptions.DecodeException;

public interface ILoadable {

	/**
	 * On demand loading
	 * 
	 * @throws DecodeException
	 */
	public void load() throws DecodeException;

	/**
	 * Free resources
	 */
	public void unload();

}
