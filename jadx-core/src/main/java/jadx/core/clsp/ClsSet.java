package jadx.core.clsp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.plugins.utils.ZipSecurity;
import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.exceptions.DecodeException;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.utils.files.FileUtils;

/**
 * Classes list for import into classpath graph
 */
public class ClsSet {
	private static final Logger LOG = LoggerFactory.getLogger(ClsSet.class);

	private static final String CLST_EXTENSION = ".jcst";
	private static final String CLST_FILENAME = "core" + CLST_EXTENSION;
	private static final String CLST_PATH = "/clst/" + CLST_FILENAME;

	private static final String JADX_CLS_SET_HEADER = "jadx-cst";
	private static final int VERSION = 3;

	private static final String STRING_CHARSET = "US-ASCII";

	private static final ArgType[] EMPTY_ARGTYPE_ARRAY = new ArgType[0];

	private final RootNode root;

	public ClsSet(RootNode root) {
		this.root = root;
	}

	private enum TypeEnum {
		WILDCARD,
		GENERIC,
		GENERIC_TYPE_VARIABLE,
		OUTER_GENERIC,
		OBJECT,
		ARRAY,
		PRIMITIVE
	}

	private ClspClass[] classes;

	public void loadFromClstFile() throws IOException, DecodeException {
		long startTime = System.currentTimeMillis();
		try (InputStream input = ClsSet.class.getResourceAsStream(CLST_PATH)) {
			if (input == null) {
				throw new JadxRuntimeException("Can't load classpath file: " + CLST_PATH);
			}
			load(input);
		}
		if (LOG.isDebugEnabled()) {
			long time = System.currentTimeMillis() - startTime;
			int methodsCount = Stream.of(classes).mapToInt(clspClass -> clspClass.getMethodsMap().size()).sum();
			LOG.debug("Clst file loaded in {}ms, classes: {}, methods: {}", time, classes.length, methodsCount);
		}
	}

	public void loadFrom(RootNode root) {
		List<ClassNode> list = root.getClasses(true);
		Map<String, ClspClass> names = new HashMap<>(list.size());
		int k = 0;
		for (ClassNode cls : list) {
			ArgType clsType = cls.getClassInfo().getType();
			String clsRawName = clsType.getObject();
			cls.load();
			ClspClass nClass = new ClspClass(clsType, k);
			if (names.put(clsRawName, nClass) != null) {
				throw new JadxRuntimeException("Duplicate class: " + clsRawName);
			}
			k++;
			nClass.setTypeParameters(cls.getGenericTypeParameters());
			nClass.setMethods(getMethodsDetails(cls));
		}
		classes = new ClspClass[k];
		k = 0;
		for (ClassNode cls : list) {
			ClspClass nClass = getCls(cls, names);
			if (nClass == null) {
				throw new JadxRuntimeException("Missing class: " + cls);
			}
			nClass.setParents(makeParentsArray(cls));
			classes[k] = nClass;
			k++;
		}
	}

	private List<ClspMethod> getMethodsDetails(ClassNode cls) {
		List<MethodNode> methodsList = cls.getMethods();
		List<ClspMethod> methods = new ArrayList<>(methodsList.size());
		for (MethodNode mth : methodsList) {
			processMethodDetails(mth, methods);
		}
		return methods;
	}

	private void processMethodDetails(MethodNode mth, List<ClspMethod> methods) {
		AccessInfo accessFlags = mth.getAccessFlags();
		if (accessFlags.isPrivate() || accessFlags.isSynthetic() || accessFlags.isBridge()) {
			return;
		}
		ClspMethod clspMethod = new ClspMethod(mth.getMethodInfo(), mth.getArgTypes(),
				mth.getReturnType(), mth.getTypeParameters(),
				mth.getThrows(), accessFlags.rawValue());
		methods.add(clspMethod);
	}

	public static ArgType[] makeParentsArray(ClassNode cls) {
		ArgType superClass = cls.getSuperClass();
		if (superClass == null) {
			// cls is java.lang.Object
			return EMPTY_ARGTYPE_ARRAY;
		}
		ArgType[] parents = new ArgType[1 + cls.getInterfaces().size()];
		parents[0] = superClass;
		int k = 1;
		for (ArgType iface : cls.getInterfaces()) {
			parents[k] = iface;
			k++;
		}
		return parents;
	}

	private static ClspClass getCls(ClassNode cls, Map<String, ClspClass> names) {
		return getCls(cls.getRawName(), names);
	}

	private static ClspClass getCls(ArgType clsType, Map<String, ClspClass> names) {
		return getCls(clsType.getObject(), names);
	}

	private static ClspClass getCls(String fullName, Map<String, ClspClass> names) {
		ClspClass cls = names.get(fullName);
		if (cls == null) {
			LOG.debug("Class not found: {}", fullName);
		}
		return cls;
	}

	public void save(Path path) throws IOException {
		FileUtils.makeDirsForFile(path);
		String outputName = path.getFileName().toString();
		if (outputName.endsWith(CLST_EXTENSION)) {
			try (BufferedOutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(path))) {
				save(outputStream);
			}
		} else if (outputName.endsWith(".jar")) {
			Path temp = FileUtils.createTempFile(".zip");
			Files.copy(path, temp, StandardCopyOption.REPLACE_EXISTING);

			try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(path));
					ZipInputStream in = new ZipInputStream(Files.newInputStream(temp))) {
				String clst = CLST_PATH;
				boolean clstReplaced = false;
				ZipEntry entry = in.getNextEntry();
				while (entry != null) {
					String entryName = entry.getName();
					ZipEntry copyEntry = new ZipEntry(entryName);
					copyEntry.setLastModifiedTime(entry.getLastModifiedTime()); // preserve modified time
					out.putNextEntry(copyEntry);
					if (entryName.equals(clst)) {
						save(out);
						clstReplaced = true;
					} else {
						FileUtils.copyStream(in, out);
					}
					entry = in.getNextEntry();
				}
				if (!clstReplaced) {
					out.putNextEntry(new ZipEntry(clst));
					save(out);
				}
			}
		} else {
			throw new JadxRuntimeException("Unknown file format: " + outputName);
		}
	}

	private void save(OutputStream output) throws IOException {
		DataOutputStream out = new DataOutputStream(output);
		out.writeBytes(JADX_CLS_SET_HEADER);
		out.writeByte(VERSION);

		Map<String, ClspClass> names = new HashMap<>(classes.length);
		out.writeInt(classes.length);
		for (ClspClass cls : classes) {
			String clsName = cls.getName();
			writeString(out, clsName);
			names.put(clsName, cls);
		}
		for (ClspClass cls : classes) {
			writeArgTypesArray(out, cls.getParents(), names);
			writeArgTypesList(out, cls.getTypeParameters(), names);
			List<ClspMethod> methods = cls.getSortedMethodsList();
			out.writeShort(methods.size());
			for (ClspMethod method : methods) {
				writeMethod(out, method, names);
			}
		}
		int methodsCount = Stream.of(classes).mapToInt(c -> c.getMethodsMap().size()).sum();
		LOG.info("Classes: {}, methods: {}, file size: {} bytes", classes.length, methodsCount, out.size());
	}

	private static void writeMethod(DataOutputStream out, ClspMethod method, Map<String, ClspClass> names) throws IOException {
		MethodInfo methodInfo = method.getMethodInfo();
		writeString(out, methodInfo.getName());
		writeArgTypesList(out, methodInfo.getArgumentsTypes(), names);
		writeArgType(out, methodInfo.getReturnType(), names);

		writeArgTypesList(out, method.containsGenericArgs() ? method.getArgTypes() : Collections.emptyList(), names);
		writeArgType(out, method.getReturnType(), names);
		writeArgTypesList(out, method.getTypeParameters(), names);
		out.writeInt(method.getRawAccessFlags());
		writeArgTypesList(out, method.getThrows(), names);
	}

	private static void writeArgTypesList(DataOutputStream out, List<ArgType> list, Map<String, ClspClass> names) throws IOException {
		int size = list.size();
		writeUnsignedByte(out, size);
		if (size != 0) {
			for (ArgType type : list) {
				writeArgType(out, type, names);
			}
		}
	}

	private static void writeArgTypesArray(DataOutputStream out, @Nullable ArgType[] arr, Map<String, ClspClass> names) throws IOException {
		if (arr == null) {
			out.writeByte(-1);
			return;
		}
		int size = arr.length;
		out.writeByte(size);
		if (size != 0) {
			for (ArgType type : arr) {
				writeArgType(out, type, names);
			}
		}
	}

	private static void writeArgType(DataOutputStream out, ArgType argType, Map<String, ClspClass> names) throws IOException {
		if (argType == null) {
			out.writeByte(-1);
			return;
		}
		if (argType.isPrimitive()) {
			out.writeByte(TypeEnum.PRIMITIVE.ordinal());
			out.writeByte(argType.getPrimitiveType().getShortName().charAt(0));
		} else if (argType.getOuterType() != null) {
			out.writeByte(TypeEnum.OUTER_GENERIC.ordinal());
			writeArgType(out, argType.getOuterType(), names);
			writeArgType(out, argType.getInnerType(), names);
		} else if (argType.getWildcardType() != null) {
			out.writeByte(TypeEnum.WILDCARD.ordinal());
			ArgType.WildcardBound bound = argType.getWildcardBound();
			out.writeByte(bound.getNum());
			if (bound != ArgType.WildcardBound.UNBOUND) {
				writeArgType(out, argType.getWildcardType(), names);
			}
		} else if (argType.isGeneric()) {
			out.writeByte(TypeEnum.GENERIC.ordinal());
			out.writeInt(getCls(argType, names).getId());
			writeArgTypesList(out, argType.getGenericTypes(), names);
		} else if (argType.isGenericType()) {
			out.writeByte(TypeEnum.GENERIC_TYPE_VARIABLE.ordinal());
			writeString(out, argType.getObject());
			writeArgTypesList(out, argType.getExtendTypes(), names);
		} else if (argType.isObject()) {
			out.writeByte(TypeEnum.OBJECT.ordinal());
			out.writeInt(getCls(argType, names).getId());
		} else if (argType.isArray()) {
			out.writeByte(TypeEnum.ARRAY.ordinal());
			writeArgType(out, argType.getArrayElement(), names);
		} else {
			throw new JadxRuntimeException("Cannot save type: " + argType);
		}
	}

	private void load(File input) throws IOException, DecodeException {
		String name = input.getName();
		if (name.endsWith(CLST_EXTENSION)) {
			try (InputStream inputStream = new FileInputStream(input)) {
				load(inputStream);
			}
		} else if (name.endsWith(".jar")) {
			ZipSecurity.readZipEntries(input, (entry, in) -> {
				if (entry.getName().endsWith(CLST_EXTENSION)) {
					try {
						load(in);
					} catch (Exception e) {
						throw new JadxRuntimeException("Failed to load jadx class set");
					}
				}
			});
		} else {
			throw new JadxRuntimeException("Unknown file format: " + name);
		}
	}

	private void load(InputStream input) throws IOException, DecodeException {
		try (DataInputStream in = new DataInputStream(new BufferedInputStream(input))) {
			byte[] header = new byte[JADX_CLS_SET_HEADER.length()];
			int readHeaderLength = in.read(header);
			int version = in.readByte();
			if (readHeaderLength != JADX_CLS_SET_HEADER.length()
					|| !JADX_CLS_SET_HEADER.equals(new String(header, STRING_CHARSET))
					|| version != VERSION) {
				throw new DecodeException("Wrong jadx class set header");
			}
			int clsCount = in.readInt();
			classes = new ClspClass[clsCount];
			for (int i = 0; i < clsCount; i++) {
				String name = readString(in);
				classes[i] = new ClspClass(ArgType.object(name), i);
			}
			for (int i = 0; i < clsCount; i++) {
				ClspClass nClass = classes[i];
				ClassInfo clsInfo = ClassInfo.fromType(root, nClass.getClsType());
				nClass.setParents(readArgTypesArray(in));
				nClass.setTypeParameters(readArgTypesList(in));
				nClass.setMethods(readClsMethods(in, clsInfo));
			}
		}
	}

	private List<ClspMethod> readClsMethods(DataInputStream in, ClassInfo clsInfo) throws IOException {
		int mCount = in.readShort();
		List<ClspMethod> methods = new ArrayList<>(mCount);
		for (int j = 0; j < mCount; j++) {
			methods.add(readMethod(in, clsInfo));
		}
		return methods;
	}

	private ClspMethod readMethod(DataInputStream in, ClassInfo clsInfo) throws IOException {
		String name = readString(in);
		List<ArgType> argTypes = readArgTypesList(in);
		ArgType retType = readArgType(in);
		List<ArgType> genericArgTypes = readArgTypesList(in);
		if (genericArgTypes.isEmpty() || Objects.equals(genericArgTypes, argTypes)) {
			genericArgTypes = argTypes;
		}
		ArgType genericRetType = readArgType(in);
		if (Objects.equals(genericRetType, retType)) {
			genericRetType = retType;
		}
		List<ArgType> typeParameters = readArgTypesList(in);
		int accFlags = in.readInt();
		List<ArgType> throwList = readArgTypesList(in);
		MethodInfo methodInfo = MethodInfo.fromDetails(root, clsInfo, name, argTypes, retType);
		return new ClspMethod(methodInfo,
				genericArgTypes, genericRetType,
				typeParameters, throwList, accFlags);
	}

	private List<ArgType> readArgTypesList(DataInputStream in) throws IOException {
		int count = in.readByte();
		if (count == 0) {
			return Collections.emptyList();
		}
		List<ArgType> list = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			list.add(readArgType(in));
		}
		return list;
	}

	@Nullable
	private ArgType[] readArgTypesArray(DataInputStream in) throws IOException {
		int count = in.readByte();
		if (count == -1) {
			return null;
		}
		if (count == 0) {
			return EMPTY_ARGTYPE_ARRAY;
		}
		ArgType[] arr = new ArgType[count];
		for (int i = 0; i < count; i++) {
			arr[i] = readArgType(in);
		}
		return arr;
	}

	private ArgType readArgType(DataInputStream in) throws IOException {
		int ordinal = in.readByte();
		if (ordinal == -1) {
			return null;
		}
		if (ordinal >= TypeEnum.values().length) {
			throw new JadxRuntimeException("Incorrect ordinal for type enum: " + ordinal);
		}
		switch (TypeEnum.values()[ordinal]) {
			case WILDCARD:
				ArgType.WildcardBound bound = ArgType.WildcardBound.getByNum(in.readByte());
				if (bound == ArgType.WildcardBound.UNBOUND) {
					return ArgType.WILDCARD;
				}
				ArgType objType = readArgType(in);
				return ArgType.wildcard(objType, bound);

			case OUTER_GENERIC:
				ArgType outerType = readArgType(in);
				ArgType innerType = readArgType(in);
				return ArgType.outerGeneric(outerType, innerType);

			case GENERIC:
				ArgType clsType = classes[in.readInt()].getClsType();
				return ArgType.generic(clsType, readArgTypesList(in));

			case GENERIC_TYPE_VARIABLE:
				String typeVar = readString(in);
				List<ArgType> extendTypes = readArgTypesList(in);
				return ArgType.genericType(typeVar, extendTypes);

			case OBJECT:
				return classes[in.readInt()].getClsType();

			case ARRAY:
				return ArgType.array(readArgType(in));

			case PRIMITIVE:
				char shortName = (char) in.readByte();
				return ArgType.parse(shortName);

			default:
				throw new JadxRuntimeException("Unsupported Arg Type: " + ordinal);
		}
	}

	private static void writeString(DataOutputStream out, String name) throws IOException {
		byte[] bytes = name.getBytes(STRING_CHARSET);
		int len = bytes.length;
		if (len >= 0xFF) {
			throw new JadxRuntimeException("String is too long: " + name);
		}
		writeUnsignedByte(out, bytes.length);
		out.write(bytes);
	}

	private static String readString(DataInputStream in) throws IOException {
		int len = readUnsignedByte(in);
		return readString(in, len);
	}

	private static String readString(DataInputStream in, int len) throws IOException {
		byte[] bytes = new byte[len];
		int count = in.read(bytes);
		while (count != len) {
			int res = in.read(bytes, count, len - count);
			if (res == -1) {
				throw new IOException("String read error");
			} else {
				count += res;
			}
		}
		return new String(bytes, STRING_CHARSET);
	}

	private static void writeUnsignedByte(DataOutputStream out, int value) throws IOException {
		if (value < 0 || value >= 0xFF) {
			throw new JadxRuntimeException("Unsigned byte value is too big: " + value);
		}
		out.writeByte(value);
	}

	private static int readUnsignedByte(DataInputStream in) throws IOException {
		return ((int) in.readByte()) & 0xFF;
	}

	public int getClassesCount() {
		return classes.length;
	}

	public void addToMap(Map<String, ClspClass> nameMap) {
		for (ClspClass cls : classes) {
			nameMap.put(cls.getName(), cls);
		}
	}
}
