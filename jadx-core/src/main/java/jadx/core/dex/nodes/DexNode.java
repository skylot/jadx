package jadx.core.dex.nodes;

import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.utils.exceptions.DecodeException;
import jadx.core.utils.files.InputFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public class DexNode {

	public static final int NO_INDEX = -1;

	private final RootNode root;
	private final Dex dexBuf;
	private final List<ClassNode> classes = new ArrayList<ClassNode>();

	private final Map<Object, FieldNode> constFields = new HashMap<Object, FieldNode>();

	public DexNode(RootNode root, InputFile input) {
		this.root = root;
		this.dexBuf = input.getDexBuffer();
	}

	public void loadClasses() throws DecodeException {
		for (ClassDef cls : dexBuf.classDefs()) {
			classes.add(new ClassNode(this, cls));
		}
	}

	public List<ClassNode> getClasses() {
		return classes;
	}

	@Nullable
	public ClassNode resolveClass(ClassInfo clsInfo) {
		return root.resolveClass(clsInfo);
	}

	@Nullable
	public MethodNode resolveMethod(MethodInfo mth) {
		ClassNode cls = resolveClass(mth.getDeclClass());
		if (cls != null) {
			return cls.searchMethod(mth);
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

	public Map<Object, FieldNode> getConstFields() {
		return constFields;
	}

	// DexBuffer wrappers

	public String getString(int index) {
		return dexBuf.strings().get(index);
	}

	public ArgType getType(int index) {
		return ArgType.parse(getString(dexBuf.typeIds().get(index)));
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
		List<ArgType> args = new ArrayList<ArgType>(paramList.getTypes().length);
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

	public RootNode root() {
		return root;
	}

	@Override
	public String toString() {
		return "DEX";
	}
}
