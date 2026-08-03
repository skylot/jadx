package jadx.gui.utils.elf;

import java.util.List;

import org.exbin.auxiliary.binary_data.BinaryData;

/**
 * Interface defining a provider of ELF data. Currently, the only implementation is JElfParser, but
 * this is provided to ease alternate implementations.
 */
public interface ElfParser {

	/**
	 * Load the provided data, and parse basic ELF information.
	 * This will be called before any get* methods.
	 *
	 * @param data a view of the binary data comprising the ELF.
	 */
	public void parse(BinaryData data);

	/**
	 * Get a string representing the ABI / ELF extensions used.
	 */
	public String getABI();

	/**
	 * Get a string representing the `e_type` / object file type of the ELF object.
	 */
	public String getType();

	/**
	 * Get a string representing the `e_machine` / architecture of the ELF object.
	 */
	public String getArchitecture();

	/**
	 * Get a list of symbols defined in the ELF object.
	 *
	 * This should include both symbols from .symtab and .dynsym.
	 */
	public List<Symbol> getSymbols();

}
