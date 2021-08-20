package jadx.plugins.input.java.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.input.data.AccessFlags;
import jadx.api.plugins.input.data.IClassData;
import jadx.api.plugins.input.data.IFieldData;
import jadx.api.plugins.input.data.IMethodData;
import jadx.api.plugins.input.data.ISeqConsumer;
import jadx.api.plugins.input.data.attributes.IJadxAttribute;
import jadx.api.plugins.utils.Utils;
import jadx.plugins.input.java.JavaClassReader;
import jadx.plugins.input.java.data.attributes.AttributesReader;
import jadx.plugins.input.java.data.attributes.IJavaAttribute;
import jadx.plugins.input.java.data.attributes.JavaAttrStorage;
import jadx.plugins.input.java.data.attributes.JavaAttrType;
import jadx.plugins.input.java.data.attributes.types.JavaAnnotationsAttr;
import jadx.plugins.input.java.utils.DisasmUtils;

public class JavaClassData implements IClassData {
	private final JavaClassReader clsReader;
	private final DataReader data;
	private final ClassOffsets offsets;
	private final ConstPoolReader constPoolReader;
	private final AttributesReader attributesReader;

	public JavaClassData(JavaClassReader clsReader) {
		this.clsReader = clsReader;
		this.data = new DataReader(clsReader.getData());
		this.offsets = new ClassOffsets(this.data);
		this.constPoolReader = new ConstPoolReader(clsReader, this, this.data.copy(), this.offsets);
		this.attributesReader = new AttributesReader(this, this.constPoolReader);
	}

	@Override
	public IClassData copy() {
		return this;
	}

	@Override
	public int getAccessFlags() {
		return data.absPos(offsets.getAccessFlagsOffset()).readU2();
	}

	@Override
	public String getType() {
		int idx = data.absPos(offsets.getClsTypeOffset()).readU2();
		return constPoolReader.getClass(idx);
	}

	@Override
	@Nullable
	public String getSuperType() {
		int idx = data.absPos(offsets.getSuperTypeOffset()).readU2();
		if (idx == 0) {
			return null;
		}
		return constPoolReader.getClass(idx);
	}

	@Override
	public List<String> getInterfacesTypes() {
		data.absPos(offsets.getInterfacesOffset());
		return data.readClassesList(constPoolReader);
	}

	@Override
	public String getInputFileName() {
		return this.clsReader.getFileName();
	}

	@Override
	public void visitFieldsAndMethods(ISeqConsumer<IFieldData> fieldsConsumer, ISeqConsumer<IMethodData> mthConsumer) {
		int clsIdx = data.absPos(offsets.getClsTypeOffset()).readU2();
		String classType = constPoolReader.getClass(clsIdx);
		DataReader reader = data.absPos(offsets.getFieldsOffset()).copy();
		int fieldsCount = reader.readU2();
		fieldsConsumer.init(fieldsCount);
		if (fieldsCount != 0) {
			JavaFieldData field = new JavaFieldData();
			field.setParentClassType(classType);
			for (int i = 0; i < fieldsCount; i++) {
				parseField(reader, field);
				fieldsConsumer.accept(field);
			}
		}

		int methodsCount = reader.readU2();
		mthConsumer.init(methodsCount);
		if (methodsCount != 0) {
			JavaMethodRef methodRef = new JavaMethodRef();
			methodRef.setParentClassType(classType);
			JavaMethodData method = new JavaMethodData(this, methodRef);
			for (int i = 0; i < methodsCount; i++) {
				parseMethod(reader, method, i);
				mthConsumer.accept(method);
			}
		}
	}

	private void parseField(DataReader reader, JavaFieldData field) {
		int accessFlags = reader.readU2();
		int nameIdx = reader.readU2();
		int typeIdx = reader.readU2();
		JavaAttrStorage attributes = attributesReader.load(reader);

		field.setAccessFlags(accessFlags);
		field.setName(constPoolReader.getUtf8(nameIdx));
		field.setType(constPoolReader.getUtf8(typeIdx));
		field.setAttributes(attributes);
	}

	private void parseMethod(DataReader reader, JavaMethodData method, int id) {
		int accessFlags = reader.readU2();
		int nameIdx = reader.readU2();
		int descriptorIdx = reader.readU2();
		JavaAttrStorage attributes = attributesReader.load(reader);

		JavaMethodRef methodRef = method.getMethodRef();
		methodRef.reset();
		methodRef.initUniqId(clsReader, id, false);
		methodRef.setName(constPoolReader.getUtf8(nameIdx));
		methodRef.setDescr(constPoolReader.getUtf8(descriptorIdx));

		if (methodRef.getName().equals("<init>")) {
			accessFlags |= AccessFlags.CONSTRUCTOR; // java bytecode don't use that flag
		}

		method.setData(accessFlags, attributes);
	}

	public DataReader getData() {
		return data;
	}

	@Override
	public List<IJadxAttribute> getAttributes() {
		data.absPos(offsets.getAttributesOffset());
		JavaAttrStorage attributes = attributesReader.load(data);
		int size = attributes.size();
		if (size == 0) {
			return Collections.emptyList();
		}
		List<IJadxAttribute> list = new ArrayList<>(size);
		Utils.addToList(list, JavaAnnotationsAttr.merge(attributes));
		Utils.addToList(list, attributes.get(JavaAttrType.INNER_CLASSES));
		Utils.addToList(list, attributes.get(JavaAttrType.SOURCE_FILE));
		Utils.addToList(list, attributes.get(JavaAttrType.SIGNATURE));
		return list;
	}

	public <T extends IJavaAttribute> T loadAttribute(DataReader reader, JavaAttrType<T> type) {
		reader.absPos(offsets.getAttributesOffset());
		return attributesReader.loadOne(type, reader);
	}

	@Override
	public String getDisassembledCode() {
		return DisasmUtils.get(data.getBytes());
	}

	public JavaClassReader getClsReader() {
		return clsReader;
	}

	public ClassOffsets getOffsets() {
		return offsets;
	}

	public ConstPoolReader getConstPoolReader() {
		return constPoolReader;
	}

	public AttributesReader getAttributesReader() {
		return attributesReader;
	}

	@Override
	public String toString() {
		return getInputFileName();
	}
}
