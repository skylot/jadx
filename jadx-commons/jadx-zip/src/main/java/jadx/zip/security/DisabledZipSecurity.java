package jadx.zip.security;

import java.io.File;

import jadx.zip.IZipEntry;

public class DisabledZipSecurity implements IJadxZipSecurity {

	public static final DisabledZipSecurity INSTANCE = new DisabledZipSecurity();

	@Override
	public boolean isValidEntry(IZipEntry entry) {
		return true;
	}

	@Override
	public boolean isValidEntryName(String entryName) {
		return true;
	}

	@Override
	public boolean isInSubDirectory(File baseDir, File file) {
		return true;
	}

	@Override
	public boolean useLimitedDataStream() {
		return false;
	}

	@Override
	public int getMaxEntriesCount() {
		return -1;
	}
}
