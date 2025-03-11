package jadx.api;

import java.io.File;

import org.jetbrains.annotations.Nullable;

import jadx.core.xmlgen.ResContainer;
import jadx.core.xmlgen.entry.ResourceEntry;
import jadx.zip.IZipEntry;

public class ResourceFile {
	private final JadxDecompiler decompiler;
	private final String name;
	private final ResourceType type;

	private @Nullable IZipEntry zipEntry;
	private String deobfName;

	public static ResourceFile createResourceFile(JadxDecompiler decompiler, File file, ResourceType type) {
		return new ResourceFile(decompiler, file.getAbsolutePath(), type);
	}

	public static ResourceFile createResourceFile(JadxDecompiler decompiler, String name, ResourceType type) {
		if (!decompiler.getArgs().getSecurity().isValidEntryName(name)) {
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

	public void setDeobfName(String resFullName) {
		this.deobfName = resFullName;
	}

	public ResourceType getType() {
		return type;
	}

	public ResContainer loadContent() {
		return ResourcesLoader.loadContent(decompiler, this);
	}

	public boolean setAlias(ResourceEntry ri) {
		StringBuilder sb = new StringBuilder();
		sb.append("res/").append(ri.getTypeName()).append(ri.getConfig());
		sb.append("/").append(ri.getKeyName());
		int lastDot = name.lastIndexOf('.');
		if (lastDot != -1) {
			sb.append(name.substring(lastDot));
		}
		String alias = sb.toString();
		if (!alias.equals(name)) {
			setDeobfName(alias);
			return true;
		}
		return false;
	}

	public @Nullable IZipEntry getZipEntry() {
		return zipEntry;
	}

	void setZipEntry(@Nullable IZipEntry zipEntry) {
		this.zipEntry = zipEntry;
	}

	public JadxDecompiler getDecompiler() {
		return decompiler;
	}

	@Override
	public String toString() {
		return "ResourceFile{name='" + name + '\'' + ", type=" + type + '}';
	}
}
