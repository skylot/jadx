package jadx.dex.nodes;

import jadx.dex.info.ClassInfo;
import jadx.dex.instructions.args.ArgType;
import jadx.utils.Utils;
import jadx.utils.exceptions.DecodeException;
import jadx.utils.files.InputFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.android.dx.io.ClassData;
import com.android.dx.io.ClassData.Method;
import com.android.dx.io.ClassDef;
import com.android.dx.io.Code;
import com.android.dx.io.DexBuffer;
import com.android.dx.io.DexBuffer.Section;
import com.android.dx.io.FieldId;
import com.android.dx.io.MethodId;
import com.android.dx.io.ProtoId;
import com.android.dx.merge.TypeList;

public class DexNode {

	public static final int NO_INDEX = -1;

	private final RootNode root;
	private final DexBuffer dexBuf;
	private final List<ClassNode> classes = new ArrayList<ClassNode>();
	private final String[] strings;

	public DexNode(RootNode root, InputFile input) throws IOException, DecodeException {
		this.root = root;
		this.dexBuf = input.getDexBuffer();

		List<String> stringList = dexBuf.strings();
		this.strings = stringList.toArray(new String[stringList.size()]);
	}

	public void loadClasses(RootNode root) throws DecodeException {
		for (ClassDef cls : dexBuf.classDefs()) {
			classes.add(new ClassNode(this, cls));
		}
	}

	public List<ClassNode> getClasses() {
		return classes;
	}

	public ClassNode resolveClass(ClassInfo clsInfo) {
		return root.resolveClass(clsInfo);
	}

	// DexBuffer wrappers

	public String getString(int index) {
		return strings[index];
	}

	public ArgType getType(int index) {
		return ArgType.parse(getString(dexBuf.typeIds().get(index)));
	}

	public List<String> getAllClassesNames() {
		List<Integer> types = dexBuf.typeIds();
		int size = types.size();
		List<String> list = new ArrayList<String>(size);
		for (Integer typeId : types) {
			String type = getString(typeId);
			if (type.length() > 0 && type.charAt(0) == 'L')
				list.add(Utils.cleanObjectName(type));
		}
		return list;
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
		return args;
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
