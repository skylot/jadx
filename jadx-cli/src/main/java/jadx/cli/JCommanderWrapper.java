package jadx.cli;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameterized;

import jadx.api.JadxDecompiler;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.options.JadxPluginOptions;
import jadx.api.plugins.options.OptionDescription;
import jadx.core.plugins.JadxPluginManager;
import jadx.core.plugins.PluginContext;
import jadx.core.utils.Utils;
import jadx.plugins.tools.JadxExternalPluginsLoader;

public class JCommanderWrapper {
	private final JCommander jc;
	private final JadxCLIArgs argsObj;

	public JCommanderWrapper(JadxCLIArgs argsObj) {
		JCommander.Builder builder = JCommander.newBuilder().addObject(argsObj);
		builder.acceptUnknownOptions(true); // workaround for "default" command
		JadxCLICommands.append(builder);
		this.jc = builder.build();
		this.argsObj = argsObj;
	}

	public boolean parse(String[] args) {
		try {
			jc.parse(args);
			applyFiles(argsObj);
			return true;
		} catch (ParameterException e) {
			System.err.println("Arguments parse error: " + e.getMessage());
			printUsage();
			return false;
		}
	}

	public void overrideProvided(JadxCLIArgs obj) {
		applyFiles(obj);
		for (ParameterDescription parameter : jc.getParameters()) {
			if (parameter.isAssigned()) {
				overrideProperty(obj, parameter);
			}
		}
	}

	public boolean processCommands() {
		String parsedCommand = jc.getParsedCommand();
		if (parsedCommand == null) {
			return false;
		}
		return JadxCLICommands.process(this, jc, parsedCommand);
	}

	/**
	 * The main parameter parsing doesn't work if accepting unknown options
	 */
	private void applyFiles(JadxCLIArgs argsObj) {
		argsObj.setFiles(jc.getUnknownOptions());
	}

	/**
	 * Override assigned field value to obj
	 */
	private static void overrideProperty(JadxCLIArgs obj, ParameterDescription parameter) {
		Parameterized parameterized = parameter.getParameterized();
		Object providedValue = parameterized.get(parameter.getObject());
		Object newValue = mergeValues(parameterized.getType(), providedValue, () -> parameterized.get(obj));
		parameterized.set(obj, newValue);
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
		LogHelper.setLogLevel(LogHelper.LogLevelEnum.ERROR); // mute logger while printing help

		// print usage in not sorted fields order (by default sorted by description)
		PrintStream out = System.out;
		out.println();
		out.println("jadx - dex to java decompiler, version: " + JadxDecompiler.getVersion());
		out.println();
		out.println("usage: jadx [command] [options] " + jc.getMainParameterDescription());

		out.println("commands (use '<command> --help' for command options):");
		for (String command : jc.getCommands().keySet()) {
			out.println("  " + command + "\t  - " + jc.getUsageFormatter().getCommandDescription(command));
		}
		out.println();

		int maxNamesLen = printOptions(jc, out, true);
		out.println(appendPluginOptions(maxNamesLen));
		out.println();
		out.println("Environment variables:");
		out.println("  JADX_DISABLE_XML_SECURITY - set to 'true' to disable all security checks for XML files");
		out.println("  JADX_DISABLE_ZIP_SECURITY - set to 'true' to disable all security checks for zip files");
		out.println("  JADX_ZIP_MAX_ENTRIES_COUNT - maximum allowed number of entries in zip files (default: 100 000)");
		out.println("  JADX_CONFIG_DIR - custom config directory, using system by default");
		out.println("  JADX_CACHE_DIR - custom cache directory, using system by default");
		out.println("  JADX_TMP_DIR - custom temp directory, using system by default");
		out.println();
		out.println("Examples:");
		out.println("  jadx -d out classes.dex");
		out.println("  jadx --rename-flags \"none\" classes.dex");
		out.println("  jadx --rename-flags \"valid, printable\" classes.dex");
		out.println("  jadx --log-level ERROR app.apk");
		out.println("  jadx -Pdex-input.verify-checksum=no app.apk");
	}

	public void printUsage(JCommander subCommander) {
		PrintStream out = System.out;
		out.println("usage: " + subCommander.getProgramName() + " [options]");
		printOptions(subCommander, out, false);
	}

	private static int printOptions(JCommander jc, PrintStream out, boolean addDefaults) {
		out.println("options:");

		List<ParameterDescription> params = jc.getParameters();
		Map<String, ParameterDescription> paramsMap = new HashMap<>(params.size());
		int maxNamesLen = 0;
		for (ParameterDescription p : params) {
			paramsMap.put(p.getParameterized().getName(), p);
			int len = p.getNames().length();
			String valueDesc = getValueDesc(p);
			if (valueDesc != null) {
				len += 1 + valueDesc.length();
			}
			maxNamesLen = Math.max(maxNamesLen, len);
		}
		maxNamesLen += 3;

		Object args = jc.getObjects().get(0);
		for (Field f : getFields(args.getClass())) {
			String name = f.getName();
			ParameterDescription p = paramsMap.get(name);
			if (p == null || p.getParameter().hidden()) {
				continue;
			}
			StringBuilder opt = new StringBuilder();
			opt.append("  ").append(p.getNames());
			String valueDesc = getValueDesc(p);
			if (valueDesc != null) {
				opt.append(' ').append(valueDesc);
			}
			addSpaces(opt, maxNamesLen - opt.length());
			String description = p.getDescription();
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
			if (addDefaults) {
				String defaultValue = getDefaultValue(args, f);
				if (defaultValue != null && !description.contains("(default)")) {
					opt.append(", default: ").append(defaultValue);
				}
			}
			out.println(opt);
		}
		return maxNamesLen;
	}

	private static @Nullable String getValueDesc(ParameterDescription p) {
		Parameter parameterAnnotation = p.getParameterAnnotation();
		return parameterAnnotation == null ? null : parameterAnnotation.defaultValueDescription();
	}

	/**
	 * Get all declared fields of the specified class and all super classes
	 */
	private static List<Field> getFields(Class<?> clazz) {
		List<Field> fieldList = new ArrayList<>();
		while (clazz != null) {
			fieldList.addAll(Arrays.asList(clazz.getDeclaredFields()));
			clazz = clazz.getSuperclass();
		}
		return fieldList;
	}

	@Nullable
	private static String getDefaultValue(Object args, Field f) {
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
		try (JadxDecompiler decompiler = new JadxDecompiler(argsObj.toJadxArgs())) {
			JadxPluginManager pluginManager = decompiler.getPluginManager();
			pluginManager.load(new JadxExternalPluginsLoader());
			pluginManager.initAll();
			try {
				for (PluginContext context : pluginManager.getAllPluginContexts()) {
					JadxPluginOptions options = context.getOptions();
					if (options != null) {
						if (appendPlugin(context.getPluginInfo(), context.getOptions(), sb, maxNamesLen)) {
							k++;
						}
					}
				}
			} finally {
				pluginManager.unloadAll();
			}
		}
		if (sb.length() == 0) {
			return "";
		}
		return "\nPlugin options (-P<name>=<value>):" + sb;
	}

	private boolean appendPlugin(JadxPluginInfo pluginInfo, JadxPluginOptions options, StringBuilder out, int maxNamesLen) {
		List<OptionDescription> descs = options.getOptionsDescriptions();
		if (descs.isEmpty()) {
			return false;
		}
		out.append("\n  ");
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
