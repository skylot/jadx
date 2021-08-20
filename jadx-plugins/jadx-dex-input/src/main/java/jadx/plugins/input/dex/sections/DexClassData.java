package jadx.plugins.input.dex.sections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.plugins.input.data.IClassData;
import jadx.api.plugins.input.data.IFieldData;
import jadx.api.plugins.input.data.IMethodData;
import jadx.api.plugins.input.data.ISeqConsumer;
import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.api.plugins.input.data.annotations.IAnnotation;
import jadx.api.plugins.input.data.attributes.IJadxAttribute;
import jadx.api.plugins.input.data.attributes.types.SourceFileAttr;
import jadx.plugins.input.dex.sections.annotations.AnnotationsParser;
import jadx.plugins.input.dex.utils.SmaliUtils;

public class DexClassData implements IClassData {
	private static final Logger LOG = LoggerFactory.getLogger(DexClassData.class);
	public static final int SIZE = 8 * 4;

	private final SectionReader in;
	private final AnnotationsParser annotationsParser;

	public DexClassData(SectionReader sectionReader, AnnotationsParser annotationsParser) {
		this.in = sectionReader;
		this.annotationsParser = annotationsParser;
	}

	@Override
	public IClassData copy() {
		return new DexClassData(in.copy(), annotationsParser.copy());
	}

	@Override
	public String getType() {
		int typeIdx = in.pos(0).readInt();
		String clsType = in.getType(typeIdx);
		if (clsType == null) {
			throw new NullPointerException("Unknown class type");
		}
		return clsType;
	}

	@Override
	public int getAccessFlags() {
		return in.pos(4).readInt();
	}

	@Nullable
	@Override
	public String getSuperType() {
		int typeIdx = in.pos(2 * 4).readInt();
		return in.getType(typeIdx);
	}

	@Override
	public List<String> getInterfacesTypes() {
		int offset = in.pos(3 * 4).readInt();
		if (offset == 0) {
			return Collections.emptyList();
		}
		return in.absPos(offset).readTypeList();
	}

	@Nullable
	private String getSourceFile() {
		int strIdx = in.pos(4 * 4).readInt();
		return in.getString(strIdx);
	}

	@Override
	public String getInputFileName() {
		return in.getDexReader().getInputFileName();
	}

	public int getAnnotationsOff() {
		return in.pos(5 * 4).readInt();
	}

	public int getClassDataOff() {
		return in.pos(6 * 4).readInt();
	}

	public int getStaticValuesOff() {
		return in.pos(7 * 4).readInt();
	}

	@Override
	public void visitFieldsAndMethods(ISeqConsumer<IFieldData> fieldConsumer, ISeqConsumer<IMethodData> mthConsumer) {
		int classDataOff = getClassDataOff();
		if (classDataOff == 0) {
			return;
		}
		SectionReader data = in.copy(classDataOff);
		int staticFieldsCount = data.readUleb128();
		int instanceFieldsCount = data.readUleb128();
		int directMthCount = data.readUleb128();
		int virtualMthCount = data.readUleb128();

		fieldConsumer.init(staticFieldsCount + instanceFieldsCount);
		mthConsumer.init(directMthCount + virtualMthCount);

		annotationsParser.setOffset(getAnnotationsOff());
		visitFields(fieldConsumer, data, staticFieldsCount, instanceFieldsCount);
		visitMethods(mthConsumer, data, directMthCount, virtualMthCount);
	}

	private void visitFields(Consumer<IFieldData> fieldConsumer, SectionReader data, int staticFieldsCount, int instanceFieldsCount) {
		Map<Integer, Integer> annotationOffsetMap = annotationsParser.readFieldsAnnotationOffsetMap();
		DexFieldData fieldData = new DexFieldData(annotationsParser);
		fieldData.setParentClassType(getType());
		readFields(fieldConsumer, data, fieldData, staticFieldsCount, annotationOffsetMap, true);
		readFields(fieldConsumer, data, fieldData, instanceFieldsCount, annotationOffsetMap, false);
	}

	private void readFields(Consumer<IFieldData> fieldConsumer, SectionReader data, DexFieldData fieldData, int count,
			Map<Integer, Integer> annOffsetMap, boolean staticFields) {
		List<EncodedValue> constValues = staticFields ? getStaticFieldInitValues(data.copy()) : null;
		int fieldId = 0;
		for (int i = 0; i < count; i++) {
			fieldId += data.readUleb128();
			int accFlags = data.readUleb128();
			in.fillFieldData(fieldData, fieldId);
			fieldData.setAccessFlags(accFlags);
			fieldData.setAnnotationsOffset(getOffsetFromMap(fieldId, annOffsetMap));
			fieldData.setConstValue(staticFields && i < constValues.size() ? constValues.get(i) : null);
			fieldConsumer.accept(fieldData);
		}
	}

	private void visitMethods(Consumer<IMethodData> mthConsumer, SectionReader data, int directMthCount, int virtualMthCount) {
		DexMethodData methodData = new DexMethodData(annotationsParser);
		methodData.setMethodRef(new DexMethodRef());
		Map<Integer, Integer> annotationOffsetMap = annotationsParser.readMethodsAnnotationOffsetMap();
		Map<Integer, Integer> paramsAnnOffsetMap = annotationsParser.readMethodParamsAnnRefOffsetMap();

		readMethods(mthConsumer, data, methodData, directMthCount, annotationOffsetMap, paramsAnnOffsetMap);
		readMethods(mthConsumer, data, methodData, virtualMthCount, annotationOffsetMap, paramsAnnOffsetMap);
	}

	private void readMethods(Consumer<IMethodData> mthConsumer, SectionReader data, DexMethodData methodData, int count,
			Map<Integer, Integer> annotationOffsetMap, Map<Integer, Integer> paramsAnnOffsetMap) {
		DexCodeReader dexCodeReader = new DexCodeReader(in.copy());
		int mthIdx = 0;
		for (int i = 0; i < count; i++) {
			mthIdx += data.readUleb128();
			int accFlags = data.readUleb128();
			int codeOff = data.readUleb128();

			DexMethodRef methodRef = methodData.getMethodRef();
			methodRef.reset();
			in.initMethodRef(mthIdx, methodRef);
			methodData.setAccessFlags(accFlags);
			if (codeOff == 0) {
				methodData.setCodeReader(null);
			} else {
				dexCodeReader.setMthId(mthIdx);
				dexCodeReader.setOffset(codeOff);
				methodData.setCodeReader(dexCodeReader);
			}
			methodData.setAnnotationsOffset(getOffsetFromMap(mthIdx, annotationOffsetMap));
			methodData.setParamAnnotationsOffset(getOffsetFromMap(mthIdx, paramsAnnOffsetMap));
			mthConsumer.accept(methodData);
		}
	}

	private static int getOffsetFromMap(int idx, Map<Integer, Integer> annOffsetMap) {
		Integer offset = annOffsetMap.get(idx);
		return offset != null ? offset : 0;
	}

	private List<EncodedValue> getStaticFieldInitValues(SectionReader reader) {
		int staticValuesOff = getStaticValuesOff();
		if (staticValuesOff == 0) {
			return Collections.emptyList();
		}
		reader.absPos(staticValuesOff);
		return annotationsParser.parseEncodedArray(reader);
	}

	private List<IAnnotation> getAnnotations() {
		annotationsParser.setOffset(getAnnotationsOff());
		return annotationsParser.readClassAnnotations();
	}

	@Override
	public List<IJadxAttribute> getAttributes() {
		List<IJadxAttribute> list = new ArrayList<>();
		String sourceFile = getSourceFile();
		if (sourceFile != null && !sourceFile.isEmpty()) {
			list.add(new SourceFileAttr(sourceFile));
		}
		DexAnnotationsConvert.forClass(getType(), list, getAnnotations());
		return list;
	}

	public int getClassDefOffset() {
		return in.pos(0).getAbsPos();
	}

	@Override
	public String getDisassembledCode() {
		byte[] dexBuf = in.getDexReader().getBuf().array();
		return SmaliUtils.getSmaliCode(dexBuf, getClassDefOffset());
	}

	@Override
	public String toString() {
		return getType();
	}
}
