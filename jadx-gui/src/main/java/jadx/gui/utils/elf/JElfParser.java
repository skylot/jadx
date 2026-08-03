package jadx.gui.utils.elf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.exbin.auxiliary.binary_data.BinaryData;

import net.fornwall.jelf.ElfFile;
import net.fornwall.jelf.ElfSymbol;
import net.fornwall.jelf.ElfSymbolTableSection;

/**
 * A ElfParser implementation using the JElf library to extract information from shared objects.
 */
public class JElfParser implements ElfParser {

	ElfFile elf;

	@Override
	public void parse(BinaryData data) {
		BinaryDataBackingFile adapter = new BinaryDataBackingFile(data);
		elf = ElfFile.from(adapter);
	}

	@Override
	public String getABI() {

		int abi = elf.ei_osabi;

		String abiStr;

		// see https://gabi.xinuos.com/elf/b-osabi.html#id1
		switch (abi) {
			case ElfConstants.ELFOSABI_NONE:
				abiStr = "UNIX - System V";
				break;
			case ElfConstants.ELFOSABI_HPUX:
				abiStr = "UNIX - HP-UX";
				break;
			case ElfConstants.ELFOSABI_NETBSD:
				abiStr = "UNIX - NetBSD";
				break;
			case ElfConstants.ELFOSABI_GNU:
				abiStr = "UNIX - GNU";
				break;
			case ElfConstants.ELFOSABI_SOLARIS:
				abiStr = "UNIX - Solaris";
				break;
			case ElfConstants.ELFOSABI_AIX:
				abiStr = "UNIX - AIX";
				break;
			case ElfConstants.ELFOSABI_IRIX:
				abiStr = "UNIX - IRIX";
				break;
			case ElfConstants.ELFOSABI_FREEBSD:
				abiStr = "UNIX - FreeBSD";
				break;
			case ElfConstants.ELFOSABI_TRU64:
				abiStr = "UNIX - TRU64";
				break;
			case ElfConstants.ELFOSABI_MODESTO:
				abiStr = "Novell - Modesto";
				break;
			case ElfConstants.ELFOSABI_OPENBSD:
				abiStr = "UNIX - OpenBSD";
				break;
			case ElfConstants.ELFOSABI_OPENVMS:
				abiStr = "VMS - OpenVMS";
				break;
			case ElfConstants.ELFOSABI_NSK:
				abiStr = "HP - Non-Stop Kernel";
				break;
			case ElfConstants.ELFOSABI_AROS:
				abiStr = "AROS";
				break;
			case ElfConstants.ELFOSABI_FENIXOS:
				abiStr = "FenixOS";
				break;
			default:
				abiStr = "Unknown";
				break;
		}

		abiStr += " (0x";
		abiStr += Integer.toHexString(abi);
		abiStr += ")";
		return abiStr;
	}

	@Override
	public String getType() {
		int typ = elf.e_type;

		// see https://gabi.xinuos.com/elf/02-eheader.html#id6
		switch (typ) {
			case ElfConstants.ET_NONE:
				return "None";
			case ElfConstants.ET_REL:
				return "REL (Relocatable file)";
			case ElfConstants.ET_EXEC:
				return "EXEC (Executable file)";
			case ElfConstants.ET_DYN:
				return "DYN (Shared object file)";
			case ElfConstants.ET_CORE:
				return "CORE (Core file)";
			default:
				return "Unknown (0x" + Integer.toHexString(typ) + ")";
		}
	}

	@Override
	public String getArchitecture() {
		int arch = elf.e_machine;

		// see https://gabi.xinuos.com/elf/a-emachine.html
		// there are MANY architectures supported by binutils; this is only a partial list of ones that seem
		// plausibly relevant.
		switch (arch) {
			case ElfConstants.EM_386:
				return "Intel 80386";
			case ElfConstants.EM_PPC:
				return "PowerPC";
			case ElfConstants.EM_PPC64:
				return "PowerPC64";
			case ElfConstants.EM_ARM:
				return "ARM (AArch32)";
			case ElfConstants.EM_IA_64:
				return "Intel IA-64";
			case ElfConstants.EM_X86_64:
				return "AMD x86-64";
			case ElfConstants.EM_AARCH64:
				return "ARM (AArch64)";
			default:
				return "Unknown (0x" + Integer.toHexString(arch) + ")";
		}
	}

	@Override
	public List<Symbol> getSymbols() {

		ElfSymbolTableSection symtab = elf.getSymbolTableSection();
		ElfSymbolTableSection dynsym = elf.getDynamicSymbolTableSection();

		List<ElfSymbol> symbols = new ArrayList<ElfSymbol>();

		// if symtab is present, it should contain all symbols; fallback to dynsym (which _should_ always be
		// present, but only contains global symbols) when symtab is not present. If somehow neither are
		// present, there are no symbols to speak of.
		if (symtab != null) {
			symbols.addAll(Arrays.asList(symtab.symbols));
		} else if (dynsym != null) {
			symbols.addAll(Arrays.asList(dynsym.symbols));
		}

		// we use an interface to prevent tying ourselves too strictly to the 3rd-party elf parser.
		return symbols.stream().map(sym -> new JElfSymbolAdapter(sym)).collect(Collectors.toList());

	}

	/**
	 * Wrapper class to adapt the JElf representation of a symbol into our own, and provide string
	 * representations of the constant values defined in the ELF specification.
	 */
	public static class JElfSymbolAdapter implements Symbol {
		private ElfSymbol sym;

		JElfSymbolAdapter(ElfSymbol sym) {
			this.sym = sym;
		}

		@Override
		public String getName() {
			return sym.getName();
		}

		@Override
		public String getTypeString() {
			int typ = sym.getType();
			switch (typ) {
				case ElfConstants.STT_NOTYPE:
					return "NOTYPE";
				case ElfConstants.STT_OBJECT:
					return "OBJECT";
				case ElfConstants.STT_FUNC:
					return "FUNC";
				case ElfConstants.STT_SECTION:
					return "SECTION";
				case ElfConstants.STT_FILE:
					return "FILE";
				case ElfConstants.STT_COMMON:
					return "COMMON";
				case ElfConstants.STT_TLS:
					return "TLS";
				case ElfConstants.STT_RELC:
					return "RELC";
				case ElfConstants.STT_SRELC:
					return "SRELC";
				default:
					return "UNKNOWN (0x" + Integer.toHexString(typ) + ")";
			}
		}

		@Override
		public String getVisibilityString() {
			// JElf provides a getVisibility method, but the enum it returns is not public so we can't
			// meaningfully use it.
			int vis = sym.st_other;

			switch (vis) {
				case ElfConstants.STV_DEFAULT:
					return "DEFAULT";
				case ElfConstants.STV_INTERNAL:
					return "INTERNAL";
				case ElfConstants.STV_HIDDEN:
					return "HIDDEN";
				case ElfConstants.STV_PROTECTED:
					return "PROTECTED";
				default:
					return "UNKNOWN (0x" + Integer.toHexString(vis) + ")";
			}

		}

		@Override
		public String getBindingString() {
			int bin = sym.getBinding();

			switch (bin) {
				case ElfConstants.STB_LOCAL:
					return "LOCAL";
				case ElfConstants.STB_GLOBAL:
					return "GLOBAL";
				case ElfConstants.STB_WEAK:
					return "WEAK";
				default:
					return "UNKNOWN (0x" + Integer.toHexString(bin) + ")";

			}
		}

	}

}
