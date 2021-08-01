package jadx.plugins.input.dex.sections.annotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jadx.api.plugins.input.data.annotations.AnnotationVisibility;
import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.api.plugins.input.data.annotations.IAnnotation;
import jadx.api.plugins.input.data.annotations.JadxAnnotation;
import jadx.plugins.input.dex.DexException;
import jadx.plugins.input.dex.sections.SectionReader;

public class AnnotationsParser {
	private final SectionReader in;
	private final SectionReader ext;

	private int offset;
	private int fieldsCount;
	private int methodsCount;
	private int paramsRefCount;

	public AnnotationsParser(SectionReader in, SectionReader ext) {
		this.in = in;
		this.ext = ext;
	}

	public AnnotationsParser copy() {
		return new AnnotationsParser(in.copy(), ext.copy());
	}

	public void setOffset(int offset) {
		this.offset = offset;
		if (offset == 0) {
			this.fieldsCount = 0;
			this.methodsCount = 0;
			this.paramsRefCount = 0;
		} else {
			in.setOffset(offset);
			in.pos(4);
			this.fieldsCount = in.readInt();
			this.methodsCount = in.readInt();
			this.paramsRefCount = in.readInt();
		}
	}

	public List<IAnnotation> readClassAnnotations() {
		if (offset == 0) {
			return Collections.emptyList();
		}
		int classAnnotationsOffset = in.absPos(offset).readInt();
		return readAnnotationList(classAnnotationsOffset);
	}

	public Map<Integer, Integer> readFieldsAnnotationOffsetMap() {
		if (fieldsCount == 0) {
			return Collections.emptyMap();
		}
		in.pos(4 * 4);
		Map<Integer, Integer> map = new HashMap<>(fieldsCount);
		for (int i = 0; i < fieldsCount; i++) {
			int fieldIdx = in.readInt();
			int fieldAnnOffset = in.readInt();
			map.put(fieldIdx, fieldAnnOffset);
		}
		return map;
	}

	public Map<Integer, Integer> readMethodsAnnotationOffsetMap() {
		if (methodsCount == 0) {
			return Collections.emptyMap();
		}
		in.pos(4 * 4 + fieldsCount * 2 * 4);
		Map<Integer, Integer> map = new HashMap<>(methodsCount);
		for (int i = 0; i < methodsCount; i++) {
			int methodIdx = in.readInt();
			int methodAnnOffset = in.readInt();
			map.put(methodIdx, methodAnnOffset);
		}
		return map;
	}

	public Map<Integer, Integer> readMethodParamsAnnRefOffsetMap() {
		if (paramsRefCount == 0) {
			return Collections.emptyMap();
		}
		in.pos(4 * 4 + fieldsCount * 2 * 4 + methodsCount * 2 * 4);
		Map<Integer, Integer> map = new HashMap<>(paramsRefCount);
		for (int i = 0; i < paramsRefCount; i++) {
			int methodIdx = in.readInt();
			int methodAnnRefOffset = in.readInt();
			map.put(methodIdx, methodAnnRefOffset);
		}
		return map;
	}

	public List<IAnnotation> readAnnotationList(int offset) {
		if (offset == 0) {
			return Collections.emptyList();
		}
		in.absPos(offset);
		int size = in.readInt();
		if (size == 0) {
			return Collections.emptyList();
		}
		List<IAnnotation> list = new ArrayList<>(size);
		int pos = in.getAbsPos();
		for (int i = 0; i < size; i++) {
			in.absPos(pos + i * 4);
			int annOffset = in.readInt();
			in.absPos(annOffset);
			list.add(readAnnotation(in, ext, true));
		}
		return list;
	}

	public List<List<IAnnotation>> readAnnotationRefList(int offset) {
		if (offset == 0) {
			return Collections.emptyList();
		}
		in.absPos(offset);
		int size = in.readInt();
		if (size == 0) {
			return Collections.emptyList();
		}
		List<List<IAnnotation>> list = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			int refOff = in.readInt();
			int pos = in.getAbsPos();
			list.add(readAnnotationList(refOff));
			in.absPos(pos);
		}
		return list;
	}

	public static IAnnotation readAnnotation(SectionReader in, SectionReader ext, boolean readVisibility) {
		AnnotationVisibility visibility = null;
		if (readVisibility) {
			int v = in.readUByte();
			visibility = getVisibilityValue(v);
		}
		int typeIndex = in.readUleb128();
		int size = in.readUleb128();
		Map<String, EncodedValue> values = new LinkedHashMap<>(size);
		for (int i = 0; i < size; i++) {
			String name = ext.getString(in.readUleb128());
			values.put(name, EncodedValueParser.parseValue(in, ext));
		}
		String type = ext.getType(typeIndex);
		return new JadxAnnotation(visibility, type, values);
	}

	private static AnnotationVisibility getVisibilityValue(int value) {
		switch (value) {
			case 0:
				return AnnotationVisibility.BUILD;
			case 1:
				return AnnotationVisibility.RUNTIME;
			case 2:
				return AnnotationVisibility.SYSTEM;
			default:
				throw new DexException("Unknown annotation visibility value: " + value);
		}
	}

	public EncodedValue parseEncodedValue(SectionReader in) {
		return EncodedValueParser.parseValue(in, ext);
	}

	public List<EncodedValue> parseEncodedArray(SectionReader in) {
		return EncodedValueParser.parseEncodedArray(in, ext);
	}
}
