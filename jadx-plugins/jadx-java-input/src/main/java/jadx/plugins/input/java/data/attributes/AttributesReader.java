package jadx.plugins.input.java.data.attributes;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.plugins.input.java.data.ConstPoolReader;
import jadx.plugins.input.java.data.DataReader;
import jadx.plugins.input.java.data.JavaClassData;

public class AttributesReader {
	private static final Logger LOG = LoggerFactory.getLogger(AttributesReader.class);

	private static final Predicate<JavaAttrType<?>> LOAD_ALL = type -> true;

	private final JavaClassData clsData;
	private final ConstPoolReader constPool;
	private final Map<Integer, JavaAttrType<?>> attrCache = new HashMap<>(JavaAttrType.size());

	public AttributesReader(JavaClassData clsData, ConstPoolReader constPoolReader) {
		this.clsData = clsData;
		this.constPool = constPoolReader;
	}

	public JavaAttrStorage loadAll(DataReader reader) {
		return loadAttributes(reader, LOAD_ALL);
	}

	public JavaAttrStorage loadMulti(DataReader reader, Set<JavaAttrType<?>> types) {
		return loadAttributes(reader, types::contains);
	}

	/**
	 * Load attributes into storage
	 *
	 * @param reader    - reader pos should be set to attributes section start
	 * @param condition - check if attribute should be parsed and added to storage
	 */
	private JavaAttrStorage loadAttributes(DataReader reader, Predicate<JavaAttrType<?>> condition) {
		int count = reader.readU2();
		if (count == 0) {
			return JavaAttrStorage.EMPTY;
		}
		JavaAttrStorage storage = new JavaAttrStorage();
		for (int i = 0; i < count; i++) {
			int nameIdx = reader.readU2();
			int len = reader.readU4();
			int end = reader.getOffset() + len;
			try {
				JavaAttrType<?> attrType = resolveAttrReader(nameIdx);
				if (attrType != null && condition.test(attrType)) {
					IJavaAttributeReader attrReader = attrType.getReader();
					if (attrReader != null) {
						IJavaAttribute attrValue = attrReader.read(clsData, reader);
						if (attrValue != null) {
							storage.add(attrType, attrValue);
						}
					}
				}
			} catch (Exception e) {
				LOG.error("Failed to parse attribute: {}", constPool.getUtf8(nameIdx), e);
			} finally {
				reader.absPos(end);
			}
		}
		return storage;
	}

	@SuppressWarnings("unchecked")
	public <T extends IJavaAttribute> @Nullable T loadOne(DataReader reader, JavaAttrType<T> type) {
		int count = reader.readU2();
		for (int i = 0; i < count; i++) {
			int nameIdx = reader.readU2();
			int len = reader.readU4();
			int end = reader.getOffset() + len;
			try {
				JavaAttrType<?> attrType = resolveAttrReader(nameIdx);
				if (attrType == type) {
					return (T) attrType.getReader().read(clsData, reader);
				}
			} catch (Exception e) {
				LOG.error("Failed to parse attribute: {}", constPool.getUtf8(nameIdx), e);
			} finally {
				reader.absPos(end);
			}
		}
		return null;
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
