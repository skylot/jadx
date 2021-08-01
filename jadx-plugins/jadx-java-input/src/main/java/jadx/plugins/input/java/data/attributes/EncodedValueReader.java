package jadx.plugins.input.java.data.attributes;

import java.util.ArrayList;
import java.util.List;

import jadx.api.plugins.input.data.annotations.AnnotationVisibility;
import jadx.api.plugins.input.data.annotations.EncodedType;
import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.api.plugins.input.data.impl.JadxFieldRef;
import jadx.plugins.input.java.data.ConstPoolReader;
import jadx.plugins.input.java.data.DataReader;
import jadx.plugins.input.java.data.JavaClassData;
import jadx.plugins.input.java.data.attributes.types.JavaAnnotationsAttr;
import jadx.plugins.input.java.utils.JavaClassParseException;

public class EncodedValueReader {

	public static EncodedValue read(JavaClassData clsData, DataReader reader) {
		ConstPoolReader constPool = clsData.getConstPoolReader();
		char tag = (char) reader.readU1();
		switch (tag) {
			case 'B':
				return new EncodedValue(EncodedType.ENCODED_BYTE, (byte) constPool.getInt(reader.readU2()));
			case 'C':
				return new EncodedValue(EncodedType.ENCODED_CHAR, (char) constPool.getInt(reader.readU2()));
			case 'D':
				return new EncodedValue(EncodedType.ENCODED_DOUBLE, constPool.getDouble(reader.readU2()));
			case 'F':
				return new EncodedValue(EncodedType.ENCODED_FLOAT, constPool.getFloat(reader.readU2()));
			case 'I':
				return new EncodedValue(EncodedType.ENCODED_INT, constPool.getInt(reader.readU2()));
			case 'J':
				return new EncodedValue(EncodedType.ENCODED_LONG, constPool.getLong(reader.readU2()));
			case 'S':
				return new EncodedValue(EncodedType.ENCODED_SHORT, (short) constPool.getInt(reader.readU2()));
			case 'Z':
				return new EncodedValue(EncodedType.ENCODED_BOOLEAN, 1 == constPool.getInt(reader.readU2()));
			case 's':
				return new EncodedValue(EncodedType.ENCODED_STRING, constPool.getUtf8(reader.readU2()));

			case 'e':
				String cls = constPool.getUtf8(reader.readU2());
				String name = constPool.getUtf8(reader.readU2());
				return new EncodedValue(EncodedType.ENCODED_ENUM, new JadxFieldRef(cls, name, cls));

			case 'c':
				return new EncodedValue(EncodedType.ENCODED_TYPE, constPool.getUtf8(reader.readU2()));

			case '@':
				return new EncodedValue(EncodedType.ENCODED_ANNOTATION,
						JavaAnnotationsAttr.readAnnotation(AnnotationVisibility.RUNTIME, clsData, reader));

			case '[':
				int len = reader.readU2();
				List<EncodedValue> values = new ArrayList<>(len);
				for (int i = 0; i < len; i++) {
					values.add(read(clsData, reader));
				}
				return new EncodedValue(EncodedType.ENCODED_ARRAY, values);

			default:
				throw new JavaClassParseException("Unknown element value tag: " + tag);
		}
	}
}
