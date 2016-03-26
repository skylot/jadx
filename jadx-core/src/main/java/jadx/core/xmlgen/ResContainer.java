package jadx.core.xmlgen;

import jadx.core.codegen.CodeWriter;
import jadx.core.utils.exceptions.JadxRuntimeException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ResContainer implements Comparable<ResContainer> {

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
		ResContainer resContainer = new ResContainer(name, Collections.<ResContainer>emptyList());
		resContainer.content = content;
		return resContainer;
	}

	public static ResContainer singleImageFile(String name, InputStream content) {
		ResContainer resContainer = new ResContainer(name, Collections.<ResContainer>emptyList());
		try {
			resContainer.image = ImageIO.read(content);
		} catch (Exception e) {
			throw new JadxRuntimeException("Image load error", e);
		}
		return resContainer;
	}

	public static ResContainer multiFile(String name) {
		return new ResContainer(name, new ArrayList<ResContainer>());
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
	public String toString() {
		return "Res{" + name + ", subFiles=" + subFiles + "}";
	}
}
