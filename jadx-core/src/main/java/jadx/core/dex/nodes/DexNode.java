package jadx.core.dex.nodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.android.dex.ClassData;
import com.android.dex.ClassData.Method;
import com.android.dex.ClassDef;
import com.android.dex.Code;
import com.android.dex.Dex;
import com.android.dex.Dex.Section;
import com.android.dex.FieldId;
import com.android.dex.MethodId;
import com.android.dex.ProtoId;
import com.android.dex.TypeList;

import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.utils.files.DexFile;

public class DexNode implements IDexNode {

	public static final int NO_INDEX = -1;

	private final RootNode root;
	private final Dex dexBuf;
	private final DexFile file;
	private final int dexId;

	private final List<ClassNode> classes = new ArrayList<>();
	private final Map<ClassInfo, ClassNode> clsMap = new HashMap<>();
	private final ArgType[] typesCache;

	public DexNode(RootNode root, DexFile input, int dexId) {
		this.root = root;
		this.file = input;
		this.dexBuf = input.getDexBuf();
		this.dexId = dexId;
		this.typesCache = new ArgType[dexBuf.typeIds().size()];
	}

	public void loadClasses() {
		for (ClassDef cls : dexBuf.classDefs()) {
			addClassNode(new ClassNode(this, cls));
		}
		// sort classes by name, expect top classes before inner
		classes.sort(Comparator.comparing(ClassNode::getFullName));
	}

	public void addClassNode(ClassNode clsNode) {
		classes.add(clsNode);
		clsMap.put(clsNode.getClassInfo(), clsNode);
	}

	void initInnerClasses() {
		// move inner classes
		List<ClassNode> inner = new ArrayList<>();
		for (ClassNode cls : classes) {
			if (cls.getClassInfo().isInner()) {
				inner.add(cls);
			}
		}
		List<ClassNode> updated = new ArrayList<>();
		for (ClassNode cls : inner) {
			ClassInfo clsInfo = cls.getClassInfo();
			ClassNode parent = resolveClass(clsInfo.getParentClass());
			if (parent == null) {
				clsMap.remove(clsInfo);
				clsInfo.notInner(root);
				clsMap.put(clsInfo, cls);
				updated.add(cls);
			} else {
				parent.addInnerClass(cls);
			}
		}
		// reload names for inner classes of updated parents
		for (ClassNode updCls : updated) {
			for (ClassNode innerCls : updCls.getInnerClasses()) {
				innerCls.getClassInfo().updateNames(root);
			}
		}
	}

	public List<ClassNode> getClasses() {
		return classes;
	}

	@Nullable
	ClassNode resolveClassLocal(ClassInfo clsInfo) {
		return clsMap.get(clsInfo);
	}

	@Nullable
	public ClassNode resolveClass(ClassInfo clsInfo) {
		ClassNode classNode = resolveClassLocal(clsInfo);
		if (classNode != null) {
			return classNode;
		}
		return root.resolveClass(clsInfo);
	}

	@Nullable
	public ClassNode resolveClass(@NotNull ArgType type) {
		if (type.isGeneric()) {
			type = ArgType.object(type.getObject());
		}
		return resolveClass(ClassInfo.fromType(root, type));
	}

	@Nullable
	public MethodNode resolveMethod(@NotNull MethodInfo mth) {
		ClassNode cls = resolveClass(mth.getDeclClass());
		if (cls != null) {
			return cls.searchMethod(mth);
		}
		return null;
	}

	@Nullable
	MethodNode deepResolveMethod(@NotNull ClassNode cls, String signature) {
		for (MethodNode m : cls.getMethods()) {
			if (m.getMethodInfo().getShortId().startsWith(signature)) {
				return m;
			}
		}
		MethodNode found;
		ArgType superClass = cls.getSuperClass();
		if (superClass != null) {
			ClassNode superNode = resolveClass(superClass);
			if (superNode != null) {
				found = deepResolveMethod(superNode, signature);
				if (found != null) {
					return found;
				}
			}
		}
		for (ArgType iFaceType : cls.getInterfaces()) {
			ClassNode iFaceNode = resolveClass(iFaceType);
			if (iFaceNode != null) {
				found = deepResolveMethod(iFaceNode, signature);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}

	@Nullable
	public FieldNode resolveField(FieldInfo field) {
		ClassNode cls = resolveClass(field.getDeclClass());
		if (cls != null) {
			return cls.searchField(field);
		}
		return null;
	}

	@Nullable
	FieldNode deepResolveField(@NotNull ClassNode cls, FieldInfo fieldInfo) {
		FieldNode field = cls.searchFieldByNameAndType(fieldInfo);
		if (field != null) {
			return field;
		}
		ArgType superClass = cls.getSuperClass();
		if (superClass != null) {
			ClassNode superNode = resolveClass(superClass);
			if (superNode != null) {
				FieldNode found = deepResolveField(superNode, fieldInfo);
				if (found != null) {
					return found;
				}
			}
		}
		for (ArgType iFaceType : cls.getInterfaces()) {
			ClassNode iFaceNode = resolveClass(iFaceType);
			if (iFaceNode != null) {
				FieldNode found = deepResolveField(iFaceNode, fieldInfo);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}

	public DexFile getDexFile() {
		return file;
	}

	// DexBuffer wrappers

	public String getString(int index) {
		if (index == DexNode.NO_INDEX) {
			return null;
		}
		return dexBuf.strings().get(index);
	}

	public ArgType getType(int index) {
		if (index == DexNode.NO_INDEX) {
			return null;
		}
		ArgType type = typesCache[index];
		if (type != null) {
			return type;
		}
		// no synchronization because exactly one ArgType instance not needed, just reduce instances count
		// note: same types but different instances will exist in other dex nodes
		ArgType parsedType = ArgType.parse(getString(dexBuf.typeIds().get(index)));
		typesCache[index] = parsedType;
		return parsedType;
	}

	public MethodId getMethodId(int mthIndex) {
		return dexBuf.methodIds().get(mthIndex);
	}

	public FieldId getFieldId(int fieldIndex) {
		return dexBuf.fieldIds().get(fieldIndex);
	}

	public ProtoId getProtoId(int protoIndex) {
		return dexBuf.protoIds().get(protoIndex);
	}

	public ClassData readClassData(ClassDef cls) {
		return dexBuf.readClassData(cls);
	}

	public List<ArgType> readParamList(int parametersOffset) {
		TypeList paramList = dexBuf.readTypeList(parametersOffset);
		List<ArgType> args = new ArrayList<>(paramList.getTypes().length);
		for (short t : paramList.getTypes()) {
			args.add(getType(t));
		}
		return Collections.unmodifiableList(args);
	}

	public Code readCode(Method mth) {
		return dexBuf.readCode(mth);
	}

	public Section openSection(int offset) {
		return dexBuf.open(offset);
	}

	public boolean checkOffset(int dataOffset) {
		return dataOffset >= 0 && dataOffset < dexBuf.getLength();
	}

	@Override
	public RootNode root() {
		return root;
	}

	@Override
	public DexNode dex() {
		return this;
	}

	@Override
	public String typeName() {
		return "dex";
	}

	public int getDexId() {
		return dexId;
	}

	@Override
	public String toString() {
		return "DEX: " + file;
	}
}
