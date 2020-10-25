package jadx.core.xmlgen;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import jadx.api.ICodeInfo;
import jadx.api.ResourceFile;

public class ResContainer implements Comparable<ResContainer> {

	public enum DataType {
		TEXT, DECODED_DATA, RES_LINK, RES_TABLE
	}

	private final DataType dataType;
	private final String name;
	private final Object data;
	private final List<ResContainer> subFiles;

	public static ResContainer textResource(String name, ICodeInfo content) {
		return new ResContainer(name, Collections.emptyList(), content, DataType.TEXT);
	}

	public static ResContainer decodedData(String name, byte[] data) {
		return new ResContainer(name, Collections.emptyList(), data, DataType.DECODED_DATA);
	}

	public static ResContainer resourceFileLink(ResourceFile resFile) {
		return new ResContainer(resFile.getDeobfName(), Collections.emptyList(), resFile, DataType.RES_LINK);
	}

	public static ResContainer resourceTable(String name, List<ResContainer> subFiles, ICodeInfo rootContent) {
		return new ResContainer(name, subFiles, rootContent, DataType.RES_TABLE);
	}

	private ResContainer(String name, List<ResContainer> subFiles, Object data, DataType dataType) {
		this.name = Objects.requireNonNull(name);
		this.subFiles = Objects.requireNonNull(subFiles);
		this.data = Objects.requireNonNull(data);
		this.dataType = Objects.requireNonNull(dataType);
	}

	public String getName() {
		return name;
	}

	public String getFileName() {
		return name.replace('/', File.separatorChar);
	}

	public List<ResContainer> getSubFiles() {
		return subFiles;
	}

	public DataType getDataType() {
		return dataType;
	}

	public ICodeInfo getText() {
		return (ICodeInfo) data;
	}

	public byte[] getDecodedData() {
		return (byte[]) data;
	}

	public ResourceFile getResLink() {
		return (ResourceFile) data;
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
		return "Res{" + name + ", type=" + dataType + ", subFiles=" + subFiles + '}';
	}
}
