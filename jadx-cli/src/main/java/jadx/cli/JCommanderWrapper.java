package jadx.cli;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameterized;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.options.JadxPluginOptions;
import jadx.api.plugins.options.OptionDescription;
import jadx.core.plugins.JadxPluginManager;
import jadx.core.plugins.PluginContext;
import jadx.core.utils.Utils;

public class JCommanderWrapper<T> {
	private final JCommander jc;
	private final JadxCLIArgs argsObj;

	public JCommanderWrapper(JadxCLIArgs argsObj) {
		this.jc = JCommander.newBuilder().addObject(argsObj).build();
		this.argsObj = argsObj;
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

	public void overrideProvided(JadxCLIArgs obj) {
		List<ParameterDescription> fieldsParams = jc.getParameters();
		List<ParameterDescription> parameters = new ArrayList<>(1 + fieldsParams.size());
		parameters.add(jc.getMainParameterValue());
		parameters.addAll(fieldsParams);
		for (ParameterDescription parameter : parameters) {
			if (parameter.isAssigned()) {
				// copy assigned field value to obj
				Parameterized parameterized = parameter.getParameterized();
				Object providedValue = parameterized.get(parameter.getObject());
				Object newValue = mergeValues(parameterized.getType(), providedValue, () -> parameterized.get(obj));
				parameterized.set(obj, newValue);
			}
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Object mergeValues(Class<?> type, Object value, Supplier<Object> prevValueProvider) {
		if (type.isAssignableFrom(Map.class)) {
			// merge maps instead replacing whole map
			Map prevMap = (Map) prevValueProvider.get();
			return Utils.mergeMaps(prevMap, (Map) value); // value map will override keys in prevMap
		}
		// simple override
		return value;
	}

	public void printUsage() {
		// print usage in not sorted fields order (by default sorted by description)
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
		maxNamesLen += 3;

		JadxCLIArgs args = (JadxCLIArgs) jc.getObjects().get(0);
		for (Field f : getFields(args.getClass())) {
			String name = f.getName();
			ParameterDescription p = paramsMap.get(name);
			if (p == null || p.getParameter().hidden()) {
				continue;
			}
			StringBuilder opt = new StringBuilder();
			opt.append("  ").append(p.getNames());
			String description = p.getDescription();
			addSpaces(opt, maxNamesLen - opt.length());
			if (description.contains("\n")) {
				String[] lines = description.split("\n");
				opt.append("- ").append(lines[0]);
				for (int i = 1; i < lines.length; i++) {
					opt.append('\n');
					addSpaces(opt, maxNamesLen + 2);
					opt.append(lines[i]);
				}
			} else {
				opt.append("- ").append(description);
			}
			String defaultValue = getDefaultValue(args, f, opt);
			if (defaultValue != null && !description.contains("(default)")) {
				opt.append(", default: ").append(defaultValue);
			}
			out.println(opt);
		}
		out.println(appendPluginOptions(maxNamesLen));
		out.println();
		out.println("Examples:");
		out.println("  jadx -d out classes.dex");
		out.println("  jadx --rename-flags \"none\" classes.dex");
		out.println("  jadx --rename-flags \"valid, printable\" classes.dex");
		out.println("  jadx --log-level ERROR app.apk");
		out.println("  jadx -Pdex-input.verify-checksum=no app.apk");
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
					return val.name().toLowerCase(Locale.ROOT);
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

	private String appendPluginOptions(int maxNamesLen) {
		StringBuilder sb = new StringBuilder();
		int k = 1;
		// load and init all options plugins to print all options
		try (JadxDecompiler decompiler = new JadxDecompiler(new JadxArgs())) {
			JadxPluginManager pluginManager = decompiler.getPluginManager();
			pluginManager.initAll();
			for (PluginContext context : pluginManager.getAllPluginContexts()) {
				JadxPluginOptions options = context.getOptions();
				if (options != null) {
					if (appendPlugin(context.getPluginInfo(), context.getOptions(), sb, maxNamesLen, k)) {
						k++;
					}
				}
			}
		}
		if (sb.length() == 0) {
			return "";
		}
		return "\nPlugin options (-P<name>=<value>):" + sb;
	}

	private boolean appendPlugin(JadxPluginInfo pluginInfo, JadxPluginOptions options, StringBuilder out, int maxNamesLen, int k) {
		List<OptionDescription> descs = options.getOptionsDescriptions();
		if (descs.isEmpty()) {
			return false;
		}
		out.append("\n ").append(k).append(") ");
		out.append(pluginInfo.getPluginId()).append(": ").append(pluginInfo.getDescription());
		for (OptionDescription desc : descs) {
			StringBuilder opt = new StringBuilder();
			opt.append("    - ").append(desc.name());
			addSpaces(opt, maxNamesLen - opt.length());
			opt.append("- ").append(desc.description());
			if (!desc.values().isEmpty()) {
				opt.append(", values: ").append(desc.values());
			}
			if (desc.defaultValue() != null) {
				opt.append(", default: ").append(desc.defaultValue());
			}
			out.append("\n").append(opt);
		}
		return true;
	}
}
