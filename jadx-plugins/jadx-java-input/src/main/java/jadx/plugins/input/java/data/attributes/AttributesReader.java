package jadx.plugins.input.java.data.attributes;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.plugins.input.java.data.ConstPoolReader;
import jadx.plugins.input.java.data.DataReader;
import jadx.plugins.input.java.data.JavaClassData;

public class AttributesReader {
	private static final Logger LOG = LoggerFactory.getLogger(AttributesReader.class);

	private final JavaClassData clsData;
	private final ConstPoolReader constPool;
	private final Map<Integer, JavaAttrType<?>> attrCache = new HashMap<>(JavaAttrType.size());

	public AttributesReader(JavaClassData clsData, ConstPoolReader constPoolReader) {
		this.clsData = clsData;
		this.constPool = constPoolReader;
	}

	public JavaAttrStorage load(DataReader reader) {
		int attributesCount = reader.readU2();
		if (attributesCount == 0) {
			return JavaAttrStorage.EMPTY;
		}
		JavaAttrStorage storage = new JavaAttrStorage();
		for (int i = 0; i < attributesCount; i++) {
			readAndAdd(storage, reader);
		}
		return storage;
	}

	private void readAndAdd(JavaAttrStorage storage, DataReader reader) {
		int nameIdx = reader.readU2();
		int len = reader.readU4();
		int end = reader.getOffset() + len;
		try {
			JavaAttrType<?> attrType = resolveAttrReader(nameIdx);
			if (attrType == null) {
				return;
			}
			IJavaAttributeReader attrReader = attrType.getReader();
			if (attrReader == null) {
				// ignore attribute
				return;
			}
			IJavaAttribute attrValue = attrReader.read(clsData, reader);
			if (attrValue != null) {
				storage.add(attrType, attrValue);
			}
		} catch (Exception e) {
			LOG.error("Failed to parse attribute: {}", constPool.getUtf8(nameIdx), e);
		} finally {
			reader.absPos(end);
		}
	}

	@SuppressWarnings("unchecked")
	@Nullable
	public <T extends IJavaAttribute> T loadOne(JavaAttrType<T> type, DataReader reader) {
		int attributesCount = reader.readU2();
		if (attributesCount == 0) {
			return null;
		}
		for (int i = 0; i < attributesCount; i++) {
			IJavaAttribute attr = readType(type, reader);
			if (attr != null) {
				return (T) attr;
			}
		}
		return null;
	}

	private IJavaAttribute readType(JavaAttrType<?> type, DataReader reader) {
		int nameIdx = reader.readU2();
		int len = reader.readU4();
		int end = reader.getOffset() + len;
		try {
			JavaAttrType<?> attrType = resolveAttrReader(nameIdx);
			if (attrType == null || attrType != type) {
				return null;
			}
			return attrType.getReader().read(clsData, reader);
		} catch (Exception e) {
			LOG.error("Failed to parse attribute: {}", constPool.getUtf8(nameIdx), e);
			return null;
		} finally {
			reader.absPos(end);
		}
	}

	private JavaAttrType<?> resolveAttrReader(int nameIdx) {
		return attrCache.computeIfAbsent(nameIdx, idx -> {
			String attrName = constPool.getUtf8(idx);
			JavaAttrType<?> attrType = JavaAttrType.byName(attrName);
			if (attrType == null) {
				LOG.warn("Unknown java class attribute type: {}", attrName);
				return null;
			}
			return attrType;
		});
	}
}
