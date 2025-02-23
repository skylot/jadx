package jadx.zip;

import java.util.Set;

import jadx.zip.security.IJadxZipSecurity;
import jadx.zip.security.JadxZipSecurity;

public class ZipReaderOptions {

	public static ZipReaderOptions getDefault() {
		return new ZipReaderOptions(new JadxZipSecurity(), ZipReaderFlags.none());
	}

	private final IJadxZipSecurity zipSecurity;
	private final Set<ZipReaderFlags> flags;

	public ZipReaderOptions(IJadxZipSecurity zipSecurity, Set<ZipReaderFlags> flags) {
		this.zipSecurity = zipSecurity;
		this.flags = flags;
	}

	public IJadxZipSecurity getZipSecurity() {
		return zipSecurity;
	}

	public Set<ZipReaderFlags> getFlags() {
		return flags;
	}
}
