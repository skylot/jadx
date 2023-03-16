package jadx.core.dex.info;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import jadx.api.JadxArgs;
import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.api.plugins.input.data.attributes.JadxAttrType;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.instructions.args.PrimitiveType;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.RootNode;

public class ConstStorage {

	private static final class ValueStorage {
		private final Map<Object, FieldNode> values = new ConcurrentHashMap<>();
		private final Set<Object> duplicates = new HashSet<>();

		public Map<Object, FieldNode> getValues() {
			return values;
		}

		public FieldNode get(Object key) {
			return values.get(key);
		}

		/**
		 * @return true if this value is duplicated
		 */
		public boolean put(Object value, FieldNode fld) {
			if (duplicates.contains(value)) {
				values.remove(value);
				return true;
			}
			FieldNode prev = values.put(value, fld);
			if (prev != null) {
				values.remove(value);
				duplicates.add(value);
				return true;
			}
			return false;
		}

		public boolean contains(Object value) {
			return duplicates.contains(value) || values.containsKey(value);
		}

		void removeForCls(ClassNode cls) {
			Iterator<Entry<Object, FieldNode>> it = values.entrySet().iterator();
			while (it.hasNext()) {
				Entry<Object, FieldNode> entry = it.next();
				FieldNode field = entry.getValue();
				if (field.getParentClass().equals(cls)) {
					it.remove();
				}
			}
		}
	}

	private final boolean replaceEnabled;
	private final ValueStorage globalValues = new ValueStorage();
	private final Map<ClassNode, ValueStorage> classes = new HashMap<>();

	private Map<Integer, String> resourcesNames = new HashMap<>();

	public ConstStorage(JadxArgs args) {
		this.replaceEnabled = args.isReplaceConsts();
	}

	public void processConstFields(ClassNode cls, List<FieldNode> staticFields) {
		if (!replaceEnabled || staticFields.isEmpty()) {
			return;
		}
		for (FieldNode f : staticFields) {
			Object value = getFieldConstValue(f);
			if (value != null) {
				addConstField(cls, f, value, f.getAccessFlags().isPublic());
			}
		}
	}

	public static @Nullable Object getFieldConstValue(FieldNode fld) {
		AccessInfo accFlags = fld.getAccessFlags();
		if (accFlags.isStatic() && accFlags.isFinal()) {
			EncodedValue constVal = fld.get(JadxAttrType.CONSTANT_VALUE);
			if (constVal != null) {
				return constVal.getValue();
			}
		}
		return null;
	}

	public void removeForClass(ClassNode cls) {
		classes.remove(cls);
		globalValues.removeForCls(cls);
	}

	private void addConstField(ClassNode cls, FieldNode fld, Object value, boolean isPublic) {
		if (isPublic) {
			globalValues.put(value, fld);
		} else {
			getClsValues(cls).put(value, fld);
		}
	}

	private ValueStorage getClsValues(ClassNode cls) {
		return classes.computeIfAbsent(cls, c -> new ValueStorage());
	}

	@Nullable
	public FieldNode getConstField(ClassNode cls, Object value, boolean searchGlobal) {
		if (!replaceEnabled) {
			return null;
		}
		RootNode root = cls.root();
		if (value instanceof Integer) {
			FieldNode rField = getResourceField((Integer) value, root);
			if (rField != null) {
				return rField;
			}
		}
		boolean foundInGlobal = globalValues.contains(value);
		if (foundInGlobal && !searchGlobal) {
			return null;
		}
		ClassNode current = cls;
		while (current != null) {
			ValueStorage classValues = classes.get(current);
			if (classValues != null) {
				FieldNode field = classValues.get(value);
				if (field != null) {
					if (foundInGlobal) {
						return null;
					}
					return field;
				}
			}
			ClassInfo parentClass = current.getClassInfo().getParentClass();
			if (parentClass == null) {
				break;
			}
			current = root.resolveClass(parentClass);
		}
		if (searchGlobal) {
			return globalValues.get(value);
		}
		return null;
	}

	@Nullable
	private FieldNode getResourceField(Integer value, RootNode root) {
		String str = resourcesNames.get(value);
		if (str == null) {
			return null;
		}
		ClassNode appResClass = root.getAppResClass();
		if (appResClass == null) {
			return null;
		}
		String[] parts = str.split("/", 2);
		if (parts.length != 2) {
			return null;
		}
		String typeName = parts[0];
		String fieldName = parts[1];
		for (ClassNode innerClass : appResClass.getInnerClasses()) {
			if (innerClass.getClassInfo().getShortName().equals(typeName)) {
				return innerClass.searchFieldByName(fieldName);
			}
		}
		appResClass.addWarn("Not found resource field with id: " + value + ", name: " + str.replace('/', '.'));
		return null;
	}

	@Nullable
	public FieldNode getConstFieldByLiteralArg(ClassNode cls, LiteralArg arg) {
		if (!replaceEnabled) {
			return null;
		}
		PrimitiveType type = arg.getType().getPrimitiveType();
		if (type == null) {
			return null;
		}
		long literal = arg.getLiteral();
		switch (type) {
			case BOOLEAN:
				return getConstField(cls, literal == 1, false);
			case CHAR:
				return getConstField(cls, (char) literal, Math.abs(literal) > 10);
			case BYTE:
				return getConstField(cls, (byte) literal, Math.abs(literal) > 10);
			case SHORT:
				return getConstField(cls, (short) literal, Math.abs(literal) > 100);
			case INT:
				return getConstField(cls, (int) literal, Math.abs(literal) > 100);
			case LONG:
				return getConstField(cls, literal, Math.abs(literal) > 1000);
			case FLOAT:
				float f = Float.intBitsToFloat((int) literal);
				return getConstField(cls, f, Float.compare(f, 0) == 0);
			case DOUBLE:
				double d = Double.longBitsToDouble(literal);
				return getConstField(cls, d, Double.compare(d, 0) == 0);

			default:
				return null;
		}
	}

	public void setResourcesNames(Map<Integer, String> resourcesNames) {
		this.resourcesNames = resourcesNames;
	}

	public Map<Integer, String> getResourcesNames() {
		return resourcesNames;
	}

	public Map<Object, FieldNode> getGlobalConstFields() {
		return globalValues.getValues();
	}

	public boolean isReplaceEnabled() {
		return replaceEnabled;
	}
}
