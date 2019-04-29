package jadx.core.clsp;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.exceptions.DecodeException;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.utils.files.FileUtils;
import jadx.core.utils.files.ZipSecurity;

/**
 * Classes list for import into classpath graph
 */
public class ClsSet {
	private static final Logger LOG = LoggerFactory.getLogger(ClsSet.class);

	private static final String CLST_EXTENSION = ".jcst";
	private static final String CLST_FILENAME = "core" + CLST_EXTENSION;
	private static final String CLST_PKG_PATH = ClsSet.class.getPackage().getName().replace('.', '/');

	private static final String JADX_CLS_SET_HEADER = "jadx-cst";
	private static final int VERSION = 2;

	private static final String STRING_CHARSET = "US-ASCII";

	private static final NClass[] EMPTY_NCLASS_ARRAY = new NClass[0];

	private enum TypeEnum {
		WILDCARD, GENERIC, GENERIC_TYPE, OBJECT, ARRAY, PRIMITIVE
	}

	private NClass[] classes;

	public void load(RootNode root) {
		List<ClassNode> list = root.getClasses(true);
		Map<String, NClass> names = new HashMap<>(list.size());
		int k = 0;
		for (ClassNode cls : list) {
			String clsRawName = cls.getRawName();
			if (cls.getAccessFlags().isPublic()) {
				cls.load();
				NClass nClass = new NClass(clsRawName, k);
				if (names.put(clsRawName, nClass) != null) {
					throw new JadxRuntimeException("Duplicate class: " + clsRawName);
				}
				k++;
				nClass.setMethods(loadMethods(cls, nClass));
			} else {
				names.put(clsRawName, null);
			}
		}
		classes = new NClass[k];
		k = 0;
		for (ClassNode cls : list) {
			if (cls.getAccessFlags().isPublic()) {
				NClass nClass = getCls(cls.getRawName(), names);
				if (nClass == null) {
					throw new JadxRuntimeException("Missing class: " + cls);
				}
				nClass.setParents(makeParentsArray(cls, names));
				classes[k] = nClass;
				k++;
			}
		}
	}

	private NMethod[] loadMethods(ClassNode cls, NClass nClass) {
		List<NMethod> methods = new ArrayList<>();
		for (MethodNode m : cls.getMethods()) {
			if (!m.getAccessFlags().isPublic()
					&& !m.getAccessFlags().isProtected()) {
				continue;
			}

			List<ArgType> args = new ArrayList<>();

			boolean genericArg = false;
			for (RegisterArg r : m.getArguments(false)) {
				ArgType argType = r.getType();
				if (argType.isGeneric() || argType.isGenericType()) {
					args.add(argType);
					genericArg = true;
				} else {
					args.add(null);
				}
			}

			ArgType retType = m.getReturnType();
			if (!retType.isGeneric() && !retType.isGenericType()) {
				retType = null;
			}

			boolean varArgs = m.getAccessFlags().isVarArgs();

			if (genericArg || retType != null || varArgs) {
				methods.add(new NMethod(
						m.getMethodInfo().getShortId(),
						args.isEmpty()
								? new ArgType[0]
								: args.toArray(new ArgType[args.size()]),
						retType,
						varArgs));
			}
		}
		return methods.toArray(new NMethod[methods.size()]);
	}

	public static NClass[] makeParentsArray(ClassNode cls, Map<String, NClass> names) {
		List<NClass> parents = new ArrayList<>(1 + cls.getInterfaces().size());
		ArgType superClass = cls.getSuperClass();
		if (superClass != null) {
			NClass c = getCls(superClass.getObject(), names);
			if (c != null) {
				parents.add(c);
			}
		}
		for (ArgType iface : cls.getInterfaces()) {
			NClass c = getCls(iface.getObject(), names);
			if (c != null) {
				parents.add(c);
			}
		}
		int size = parents.size();
		if (size == 0) {
			return EMPTY_NCLASS_ARRAY;
		}
		return parents.toArray(new NClass[size]);
	}

	private static NClass getCls(String fullName, Map<String, NClass> names) {
		NClass cls = names.get(fullName);
		if (cls == null) {
			LOG.debug("Class not found: {}", fullName);
		}
		return cls;
	}

	void save(Path path) throws IOException {
		Files.createDirectories(path.getParent());
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
				String clst = CLST_PKG_PATH + '/' + CLST_FILENAME;
				out.putNextEntry(new ZipEntry(clst));
				save(out);
				ZipEntry entry = in.getNextEntry();
				while (entry != null) {
					if (!entry.getName().equals(clst)) {
						out.putNextEntry(new ZipEntry(entry.getName()));
						FileUtils.copyStream(in, out);
					}
					entry = in.getNextEntry();
				}
			}
		} else {
			throw new JadxRuntimeException("Unknown file format: " + outputName);
		}
	}

	public void save(OutputStream output) throws IOException {
		DataOutputStream out = new DataOutputStream(output);
		out.writeBytes(JADX_CLS_SET_HEADER);
		out.writeByte(VERSION);

		LOG.info("Classes count: {}", classes.length);
		Map<String, NClass> names = new HashMap<>(classes.length);
		out.writeInt(classes.length);
		for (NClass cls : classes) {
			writeString(out, cls.getName());
			names.put(cls.getName(), cls);
		}
		for (NClass cls : classes) {
			NClass[] parents = cls.getParents();
			out.writeByte(parents.length);
			for (NClass parent : parents) {
				out.writeInt(parent.getId());
			}
			NMethod[] methods = cls.getMethods();
			out.writeByte(methods.length);
			for (NMethod method : methods) {
				writeMethod(out, method, names);
			}
		}
	}

	private static void writeMethod(DataOutputStream out, NMethod method, Map<String, NClass> names) throws IOException {
		int argCount = 0;
		ArgType[] argTypes = method.getArgType();
		for (ArgType arg : argTypes) {
			if (arg != null) {
				argCount++;
			}
		}

		writeLongString(out, method.getShortId());
		out.writeByte(argCount);

		// last argument first
		for (int i = argTypes.length - 1; i >= 0; i--) {
			ArgType argType = argTypes[i];
			if (argType != null) {
				out.writeByte(i);
				writeArgType(out, argType, names);
			}
		}

		if (method.getReturnType() != null) {
			out.writeBoolean(true);
			writeArgType(out, method.getReturnType(), names);
		} else {
			out.writeBoolean(false);
		}

		out.writeBoolean(method.isVarArgs());
	}

	private static void writeArgType(DataOutputStream out, ArgType argType, Map<String, NClass> names) throws IOException {
		if (argType.getWildcardType() != null) {
			out.writeByte(TypeEnum.WILDCARD.ordinal());
			int bounds = argType.getWildcardBounds();
			out.writeByte(bounds);
			if (bounds != 0) {
				writeArgType(out, argType.getWildcardType(), names);
			}
		} else if (argType.isGeneric()) {
			out.writeByte(TypeEnum.GENERIC.ordinal());
			out.writeInt(names.get(argType.getObject()).getId());
			ArgType[] types = argType.getGenericTypes();
			if (types == null) {
				out.writeByte(0);
			} else {
				out.writeByte(types.length);
				for (ArgType type : types) {
					writeArgType(out, type, names);
				}
			}
		} else if (argType.isGenericType()) {
			out.writeByte(TypeEnum.GENERIC_TYPE.ordinal());
			writeString(out, argType.getObject());
		} else if (argType.isObject()) {
			out.writeByte(TypeEnum.OBJECT.ordinal());
			out.writeInt(names.get(argType.getObject()).getId());
		} else if (argType.isArray()) {
			out.writeByte(TypeEnum.ARRAY.ordinal());
			writeArgType(out, argType.getArrayElement(), names);
		} else if (argType.isPrimitive()) {
			out.writeByte(TypeEnum.PRIMITIVE.ordinal());
			out.writeByte(argType.getPrimitiveType().getShortName().charAt(0));
		} else {
			throw new JadxRuntimeException("Cannot save type: " + argType);
		}
	}

	public void load() throws IOException, DecodeException {
		try (InputStream input = getClass().getResourceAsStream(CLST_FILENAME)) {
			if (input == null) {
				throw new JadxRuntimeException("Can't load classpath file: " + CLST_FILENAME);
			}
			load(input);
		}
	}

	public void load(File input) throws IOException, DecodeException {
		String name = input.getName();
		try (InputStream inputStream = new FileInputStream(input)) {
			if (name.endsWith(CLST_EXTENSION)) {
				load(inputStream);
			} else if (name.endsWith(".jar")) {
				try (ZipInputStream in = new ZipInputStream(inputStream)) {
					ZipEntry entry = in.getNextEntry();
					while (entry != null) {
						if (entry.getName().endsWith(CLST_EXTENSION) && ZipSecurity.isValidZipEntry(entry)) {
							load(in);
						}
						entry = in.getNextEntry();
					}
				}
			} else {
				throw new JadxRuntimeException("Unknown file format: " + name);
			}
		}
	}

	public void load(InputStream input) throws IOException, DecodeException {
		try (DataInputStream in = new DataInputStream(input)) {
			byte[] header = new byte[JADX_CLS_SET_HEADER.length()];
			int readHeaderLength = in.read(header);
			int version = in.readByte();
			if (readHeaderLength != JADX_CLS_SET_HEADER.length()
					|| !JADX_CLS_SET_HEADER.equals(new String(header, STRING_CHARSET))
					|| version != VERSION) {
				throw new DecodeException("Wrong jadx class set header");
			}
			int count = in.readInt();
			classes = new NClass[count];
			for (int i = 0; i < count; i++) {
				String name = readString(in);
				classes[i] = new NClass(name, i);
			}
			for (int i = 0; i < count; i++) {
				int pCount = in.readByte();
				NClass[] parents = new NClass[pCount];
				for (int j = 0; j < pCount; j++) {
					parents[j] = classes[in.readInt()];
				}
				classes[i].setParents(parents);

				int mCount = in.readByte();
				NMethod[] methods = new NMethod[mCount];
				for (int j = 0; j < mCount; j++) {
					methods[j] = readMethod(in);
				}
				classes[i].setMethods(methods);
			}
		}
	}

	private NMethod readMethod(DataInputStream in) throws IOException {
		String shortId = readLongString(in);
		int argCount = in.readByte();
		ArgType[] argTypes = null;
		for (int i = 0; i < argCount; i++) {
			int index = in.readByte();
			ArgType argType = readArgType(in);
			if (argTypes == null) {
				argTypes = new ArgType[index + 1];
			}
			argTypes[index] = argType;
		}
		ArgType retType = in.readBoolean() ? readArgType(in) : null;
		boolean varArgs = in.readBoolean();
		return new NMethod(shortId, argTypes, retType, varArgs);
	}

	private ArgType readArgType(DataInputStream in) throws IOException {
		int ordinal = in.readByte();
		switch (TypeEnum.values()[ordinal]) {
			case WILDCARD:
				int bounds = in.readByte();
				return bounds == 0
						? ArgType.wildcard()
						: ArgType.wildcard(readArgType(in), bounds);
			case GENERIC:
				String obj = classes[in.readInt()].getName();
				int typeLength = in.readByte();
				ArgType[] generics;
				if (typeLength == 0) {
					generics = null;
				} else {
					generics = new ArgType[typeLength];
					for (int i = 0; i < typeLength; i++) {
						generics[i] = readArgType(in);
					}
				}
				return ArgType.generic(obj, generics);
			case GENERIC_TYPE:
				return ArgType.genericType(readString(in));
			case OBJECT:
				return ArgType.object(classes[in.readInt()].getName());
			case ARRAY:
				return ArgType.array(readArgType(in));
			case PRIMITIVE:
				int shortName = in.readByte();
				switch (shortName) {
					case 'Z':
						return ArgType.BOOLEAN;
					case 'C':
						return ArgType.CHAR;
					case 'B':
						return ArgType.BYTE;
					case 'S':
						return ArgType.SHORT;
					case 'I':
						return ArgType.INT;
					case 'F':
						return ArgType.FLOAT;
					case 'J':
						return ArgType.LONG;
					case 'D':
						return ArgType.DOUBLE;
					default:
						return ArgType.VOID;
				}
			default:
				throw new JadxRuntimeException("Unsupported Arg Type: " + ordinal);
		}
	}

	private static void writeString(DataOutputStream out, String name) throws IOException {
		byte[] bytes = name.getBytes(STRING_CHARSET);
		out.writeByte(bytes.length);
		out.write(bytes);
	}

	private static void writeLongString(DataOutputStream out, String name) throws IOException {
		byte[] bytes = name.getBytes(STRING_CHARSET);
		out.writeShort(bytes.length);
		out.write(bytes);
	}

	private static String readString(DataInputStream in) throws IOException {
		int len = in.readByte();
		return readString(in, len);
	}

	private static String readLongString(DataInputStream in) throws IOException {
		int len = in.readShort();
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

	public int getClassesCount() {
		return classes.length;
	}

	public void addToMap(Map<String, NClass> nameMap) {
		for (NClass cls : classes) {
			nameMap.put(cls.getName(), cls);
		}
	}
}
