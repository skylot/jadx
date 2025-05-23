package jadx.plugins.input.xapk.data;

import java.nio.file.Path;
import java.util.List;

public class XApkData {
	private final XApkManifest manifest;
	private final Path tmpDir;
	private final List<Path> files;
	private final List<Path> apks;

	public XApkData(XApkManifest manifest, Path tmpDir, List<Path> apks, List<Path> files) {
		this.manifest = manifest;
		this.tmpDir = tmpDir;
		this.apks = apks;
		this.files = files;
	}

	public List<Path> getApks() {
		return apks;
	}

	public List<Path> getFiles() {
		return files;
	}

	public XApkManifest getManifest() {
		return manifest;
	}

	public Path getTmpDir() {
		return tmpDir;
	}
}
