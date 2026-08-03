package jadx.gui.utils.elf;

/**
 * Abstraction of a ELF symbol.
 */
public interface Symbol {

	/**
	 * Get the name of the symbol (st_name).
	 */
	public String getName();

	/**
	 * Get a string representing the visibility of the symbol (based on st_other).
	 */
	public String getVisibilityString();

	/**
	 * Get a string representing the type of the symbol (based on st_info).
	 */
	public String getTypeString();

	/**
	 * Get a string representing the binding type of the symbol (based on st_info).
	 */
	String getBindingString();

}
