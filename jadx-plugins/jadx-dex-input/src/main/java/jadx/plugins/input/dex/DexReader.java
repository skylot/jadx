package jadx.plugins.input.dex;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

import jadx.api.plugins.input.data.IClassData;
import jadx.plugins.input.dex.sections.DexClassData;
import jadx.plugins.input.dex.sections.DexHeader;
import jadx.plugins.input.dex.sections.SectionReader;
import jadx.plugins.input.dex.sections.annotations.AnnotationsParser;

public class DexReader {
	private final int uniqId;
	private final String inputFileName;
	private final ByteBuffer buf;
	private final DexHeader header;

	public DexReader(int uniqId, String inputFileName, byte[] content) {
		this.uniqId = uniqId;
		this.inputFileName = inputFileName;
		this.buf = ByteBuffer.wrap(content);
		this.header = new DexHeader(new SectionReader(this, 0));
	}

	public void visitClasses(Consumer<IClassData> consumer) {
		int count = header.getClassDefsSize();
		if (count == 0) {
			return;
		}
		int classDefsOff = header.getClassDefsOff();
		SectionReader in = new SectionReader(this, classDefsOff);
		AnnotationsParser annotationsParser = new AnnotationsParser(in.copy(), in.copy());
		DexClassData classData = new DexClassData(in, annotationsParser);
		for (int i = 0; i < count; i++) {
			consumer.accept(classData);
			in.shiftOffset(DexClassData.SIZE);
		}
	}

	public ByteBuffer getBuf() {
		return buf;
	}

	public DexHeader getHeader() {
		return header;
	}

	public String getInputFileName() {
		return inputFileName;
	}

	public int getUniqId() {
		return uniqId;
	}

	@Override
	public String toString() {
		return inputFileName;
	}
}
