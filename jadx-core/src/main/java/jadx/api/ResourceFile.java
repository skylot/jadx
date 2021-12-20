package jadx.api;

import java.io.File;

import jadx.api.plugins.utils.ZipSecurity;
import jadx.core.xmlgen.ResContainer;
import jadx.core.xmlgen.entry.ResourceEntry;

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
	private String deobfName;

	public static ResourceFile createResourceFile(JadxDecompiler decompiler, File file, ResourceType type) {
		return new ResourceFile(decompiler, file.getAbsolutePath(), type);
	}

	public static ResourceFile createResourceFile(JadxDecompiler decompiler, String name, ResourceType type) {
		if (!ZipSecurity.isValidZipEntryName(name)) {
			return null;
		}
		return new ResourceFile(decompiler, name, type);
	}

	protected ResourceFile(JadxDecompiler decompiler, String name, ResourceType type) {
		this.decompiler = decompiler;
		this.name = name;
		this.type = type;
	}

	public String getOriginalName() {
		return name;
	}

	public String getDeobfName() {
		return deobfName != null ? deobfName : name;
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

	public void setAlias(ResourceEntry ri) {
		int index = name.lastIndexOf('.');
		deobfName = String.format("res/%s%s/%s%s",
				ri.getTypeName(),
				ri.getConfig(),
				ri.getKeyName(),
				index == -1 ? "" : name.substring(index));
	}

	public ZipRef getZipRef() {
		return zipRef;
	}

	@Override
	public String toString() {
		return "ResourceFile{name='" + name + '\'' + ", type=" + type + '}';
	}
}
