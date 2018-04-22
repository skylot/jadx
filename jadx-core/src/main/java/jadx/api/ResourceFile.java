package jadx.api;

import java.io.File;

import jadx.core.utils.files.ZipSecurity;
import jadx.core.xmlgen.ResContainer;

public class ResourceFile {

	public static final class ZipRef {
		private final File zipFile;
		private final String entryName;

		public ZipRef(File zipFile, String entryName) {
			this.zipFile = zipFile;
			this.entryName = entryName;
		}

		public File getZipFile() {
			return zipFile;
		}

		public String getEntryName() {
			return entryName;
		}

		@Override
		public String toString() {
			return "ZipRef{" + zipFile + ", '" + entryName + "'}";
		}
	}

	private final JadxDecompiler decompiler;
	private final String name;
	private final ResourceType type;
	private ZipRef zipRef;

	protected ResourceFile(JadxDecompiler decompiler, String name, ResourceType type) {
		this.decompiler = decompiler;
		this.name = name;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public ResourceType getType() {
		return type;
	}

	public ResContainer loadContent() {
		return ResourcesLoader.loadContent(decompiler, this);
	}

	void setZipRef(ZipRef zipRef) {
		this.zipRef = zipRef;
	}

	public ZipRef getZipRef() {
		return zipRef;
	}

	@Override
	public String toString() {
		return "ResourceFile{name='" + name + '\'' + ", type=" + type + "}";
	}
	
	public static ResourceFile createResourceFileInstance(JadxDecompiler decompiler, String name, ResourceType type) {
		if(!ZipSecurity.isValidZipEntryName(name)) {
			return null;
		}
		return new ResourceFile(decompiler, name, type);
	}
}
