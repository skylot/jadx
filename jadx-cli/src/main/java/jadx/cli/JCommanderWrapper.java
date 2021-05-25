package jadx.cli;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameterized;

import jadx.api.JadxDecompiler;

public class JCommanderWrapper<T> {
	private final JCommander jc;

	public JCommanderWrapper(T obj) {
		this.jc = JCommander.newBuilder().addObject(obj).build();
	}

	public boolean parse(String[] args) {
		try {
			jc.parse(args);
			return true;
		} catch (ParameterException e) {
			System.err.println("Arguments parse error: " + e.getMessage());
			printUsage();
			return false;
		}
	}

	public void overrideProvided(T obj) {
		List<ParameterDescription> fieldsParams = jc.getParameters();
		List<ParameterDescription> parameters = new ArrayList<>(1 + fieldsParams.size());
		parameters.add(jc.getMainParameterValue());
		parameters.addAll(fieldsParams);
		for (ParameterDescription parameter : parameters) {
			if (parameter.isAssigned()) {
				// copy assigned field value to obj
				Parameterized parameterized = parameter.getParameterized();
				Object val = parameterized.get(parameter.getObject());
				parameterized.set(obj, val);
			}
		}
	}

	public void printUsage() {
		// print usage in not sorted fields order (by default its sorted by description)
		PrintStream out = System.out;
		out.println();
		out.println("jadx - dex to java decompiler, version: " + JadxDecompiler.getVersion());
		out.println();
		out.println("usage: jadx [options] " + jc.getMainParameterDescription());
		out.println("options:");

		List<ParameterDescription> params = jc.getParameters();
		Map<String, ParameterDescription> paramsMap = new LinkedHashMap<>(params.size());
		int maxNamesLen = 0;
		for (ParameterDescription p : params) {
			paramsMap.put(p.getParameterized().getName(), p);
			int len = p.getNames().length();
			if (len > maxNamesLen) {
				maxNamesLen = len;
			}
		}

		JadxCLIArgs args = (JadxCLIArgs) jc.getObjects().get(0);
		for (Field f : getFields(args.getClass())) {
			String name = f.getName();
			ParameterDescription p = paramsMap.get(name);
			if (p == null) {
				continue;
			}
			StringBuilder opt = new StringBuilder();
			opt.append("  ").append(p.getNames());
			String description = p.getDescription();
			addSpaces(opt, maxNamesLen - opt.length() + 3);
			if (description.contains("\n")) {
				String[] lines = description.split("\n");
				opt.append("- ").append(lines[0]);
				for (int i = 1; i < lines.length; i++) {
					opt.append('\n');
					addSpaces(opt, maxNamesLen + 5);
					opt.append(lines[i]);
				}
			} else {
				opt.append("- ").append(description);
			}
			String defaultValue = getDefaultValue(args, f, opt);
			if (defaultValue != null) {
				opt.append(", default: ").append(defaultValue);
			}
			out.println(opt);
		}
		out.println("Examples:");
		out.println("  jadx -d out classes.dex");
		out.println("  jadx --rename-flags \"none\" classes.dex");
		out.println("  jadx --rename-flags \"valid, printable\" classes.dex");
		out.println("  jadx --log-level ERROR app.apk");
	}

	/**
	 * Get all declared fields of the specified class and all super classes
	 */
	private List<Field> getFields(Class<?> clazz) {
		List<Field> fieldList = new ArrayList<>();
		while (clazz != null) {
			fieldList.addAll(Arrays.asList(clazz.getDeclaredFields()));
			clazz = clazz.getSuperclass();
		}
		return fieldList;
	}

	@Nullable
	private String getDefaultValue(JadxCLIArgs args, Field f, StringBuilder opt) {
		try {
			Class<?> fieldType = f.getType();
			if (fieldType == int.class) {
				return Integer.toString(f.getInt(args));
			}
			if (fieldType == String.class) {
				return (String) f.get(args);
			}
			if (Enum.class.isAssignableFrom(fieldType)) {
				Enum<?> val = (Enum<?>) f.get(args);
				if (val != null) {
					return val.name();
				}
			}
		} catch (Exception e) {
			// ignore
		}
		return null;
	}

	private static void addSpaces(StringBuilder str, int count) {
		for (int i = 0; i < count; i++) {
			str.append(' ');
		}
	}
}
