package jadx.api;

import java.io.File;

import org.jetbrains.annotations.Nullable;

import jadx.core.deobf.FileTypeDetector;
import jadx.core.utils.StringUtils;
import jadx.core.utils.exceptions.JadxException;
import jadx.core.xmlgen.ResContainer;
import jadx.core.xmlgen.entry.ResourceEntry;
import jadx.zip.IZipEntry;

public class ResourceFile {
	private final JadxDecompiler decompiler;
	private final String name;
	private ResourceType type;

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

	public boolean setAlias(ResourceEntry entry, boolean useHeders) {
		StringBuilder sb = new StringBuilder();
		sb.append("res/").append(entry.getTypeName()).append(entry.getConfig());
		sb.append("/").append(entry.getKeyName());

		if (useHeders) {
			try {
				int maxBytesToReadLimit = 4096;
				byte[] bytes = ResourcesLoader.decodeStream(this, (size, is) -> {
					int bytesToRead;
					if (size > 0) {
						bytesToRead = (int) Math.min(size, maxBytesToReadLimit);
					} else if (size == 0) {
						bytesToRead = 0;
					} else {
						bytesToRead = maxBytesToReadLimit;
					}
					if (bytesToRead == 0) {
						return new byte[0];
					}
					return is.readNBytes(bytesToRead);
				});

				String fileExtension = FileTypeDetector.detectFileExtension(bytes);
				if (!StringUtils.isEmpty(fileExtension)) {
					sb.append(fileExtension);
				} else {
					sb.append(getExtFromName(name));
				}
			} catch (JadxException ignored) {
			}
		} else {
			sb.append(getExtFromName(name));
		}
		String alias = sb.toString();
		if (!alias.equals(name)) {
			setDeobfName(alias);
			type = ResourceType.getFileType(alias);
			return true;
		}
		return false;
	}

	private String getExtFromName(String name) {
		// the image .9.png extension always saved, when resource shrinking by aapt2
		if (name.contains(".9.png")) {
			return ".9.png";
		}

		int lastDot = name.lastIndexOf('.');
		if (lastDot != -1) {
			return name.substring(lastDot);
		}

		return "";
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
