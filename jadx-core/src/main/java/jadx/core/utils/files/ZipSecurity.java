package jadx.core.utils.files;

import java.io.File;
import java.util.zip.ZipEntry;

public class ZipSecurity {
	// size of uncompressed zip entry shouldn't be bigger of compressed in MAX_SIZE_DIFF times
	private static final int MAX_SIZE_DIFF = 5;
	
	private static boolean isInSubDirectory(File base, File file) {
	    if (file == null) {
	        return false;
	    }
	    if (file.equals(base)) {
	        return true;
	    }

	    return isInSubDirectory(base, file.getParentFile());
	}
	
	// checks that entry name contains no any traversals
	// and prevents cases like "../classes.dex", to limit output only to the specified directory
	public static boolean isValidZipEntryName(String entryName) {
		try {
			File currentPath = new File(".").getCanonicalFile();
			File canonical = new File(currentPath, entryName).getCanonicalFile();
			return isInSubDirectory(currentPath, canonical);
		}
		catch(Exception e) {
			return false;
		}
	}
	
	public static boolean isZipBomb(ZipEntry entry) {
		long compressedSize   = entry.getCompressedSize();
		long uncompressedSize = entry.getSize();
		if(compressedSize < 0 || uncompressedSize < 0) {
			return true;
		}
		return compressedSize * MAX_SIZE_DIFF < uncompressedSize;
	}
	
	public static boolean isValidZipEntry(ZipEntry entry) {
		return isValidZipEntryName(entry.getName())
				&& !isZipBomb(entry);
	}
}
