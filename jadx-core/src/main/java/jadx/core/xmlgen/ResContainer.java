package jadx.core.xmlgen;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.codegen.CodeWriter;
import jadx.core.utils.android.Res9patchStreamDecoder;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class ResContainer implements Comparable<ResContainer> {

	private static final Logger LOG = LoggerFactory.getLogger(ResContainer.class);

	private final String name;
	private final List<ResContainer> subFiles;

	@Nullable
	private CodeWriter content;
	@Nullable
	private BufferedImage image;

	private ResContainer(String name, List<ResContainer> subFiles) {
		this.name = name;
		this.subFiles = subFiles;
	}

	public static ResContainer singleFile(String name, CodeWriter content) {
		ResContainer resContainer = new ResContainer(name, Collections.emptyList());
		resContainer.content = content;
		return resContainer;
	}

	public static ResContainer singleImageFile(String name, InputStream content) {
		ResContainer resContainer = new ResContainer(name, Collections.emptyList());
		InputStream newContent = content;
		if (name.endsWith(".9.png")) {
			Res9patchStreamDecoder decoder = new Res9patchStreamDecoder();
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			try {
				decoder.decode(content, os);
			} catch (Exception e) {
				LOG.error("Failed to decode 9-patch png image, path: {}", name, e);
			}
			newContent = new ByteArrayInputStream(os.toByteArray());
		}
		try {
			resContainer.image = ImageIO.read(newContent);
		} catch (Exception e) {
			throw new JadxRuntimeException("Image load error", e);
		}
		return resContainer;
	}

	public static ResContainer multiFile(String name) {
		return new ResContainer(name, new ArrayList<>());
	}

	public String getName() {
		return name;
	}

	public String getFileName() {
		return name.replace("/", File.separator);
	}

	@Nullable
	public CodeWriter getContent() {
		return content;
	}

	public void setContent(@Nullable CodeWriter content) {
		this.content = content;
	}

	@Nullable
	public BufferedImage getImage() {
		return image;
	}

	public List<ResContainer> getSubFiles() {
		return subFiles;
	}

	@Override
	public int compareTo(@NotNull ResContainer o) {
		return name.compareTo(o.name);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ResContainer)) {
			return false;
		}
		ResContainer that = (ResContainer) o;
		return name.equals(that.name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public String toString() {
		return "Res{" + name + ", subFiles=" + subFiles + "}";
	}
}
