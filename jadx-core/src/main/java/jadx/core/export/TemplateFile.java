package jadx.core.export;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jadx.core.utils.exceptions.JadxRuntimeException;

import static jadx.core.utils.files.FileUtils.close;

/**
 * Simple template engine
 * Syntax for replace variable with value: '{{variable}}'
 */
public class TemplateFile {

	private enum State {
		NONE, START, VARIABLE, END
	}

	private static class ParserState {
		private State state = State.NONE;
		private StringBuilder curVariable;
		private boolean skip;
	}

	private final String templateName;
	private final InputStream template;
	private final Map<String, String> values = new HashMap<>();

	public static TemplateFile fromResources(String path) throws FileNotFoundException {
		InputStream res = TemplateFile.class.getResourceAsStream(path);
		if (res == null) {
			throw new FileNotFoundException("Resource not found: " + path);
		}
		return new TemplateFile(path, res);
	}

	private TemplateFile(String name, InputStream in) {
		this.templateName = name;
		this.template = in;
	}

	public void add(String name, @NotNull Object value) {
		values.put(name, value.toString());
	}

	public String build() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			process(out);
		} finally {
			close(out);
		}
		return out.toString();
	}

	public void save(File outFile) throws IOException {
		OutputStream out = new FileOutputStream(outFile);
		try {
			process(out);
		} finally {
			close(out);
		}
	}

	private void process(OutputStream out) throws IOException {
		if (template.available() == 0) {
			throw new IOException("Template already processed");
		}
		try (InputStream in = new BufferedInputStream(template)) {
			ParserState state = new ParserState();
			while (true) {
				int ch = in.read();
				if (ch == -1) {
					break;
				}
				String str = process(state, (char) ch);
				if (str != null) {
					out.write(str.getBytes());
				} else if (!state.skip) {
					out.write(ch);
				}
			}
		}
	}

	@Nullable
	private String process(ParserState parser, char ch) {
		State state = parser.state;
		switch (ch) {
			case '{':
				switch (state) {
					case START:
						parser.state = State.VARIABLE;
						parser.curVariable = new StringBuilder();
						break;

					default:
						parser.state = State.START;
						break;
				}
				parser.skip = true;
				return null;

			case '}':
				switch (state) {
					case VARIABLE:
						parser.state = State.END;
						parser.skip = true;
						return null;

					case END:
						parser.state = State.NONE;
						String varName = parser.curVariable.toString();
						parser.curVariable = new StringBuilder();
						return processVar(varName);
				}
				break;

			default:
				switch (state) {
					case VARIABLE:
						parser.curVariable.append(ch);
						parser.skip = true;
						return null;

					case START:
						parser.state = State.NONE;
						return "{" + ch;

					case END:
						throw new JadxRuntimeException("Expected variable end: '" + parser.curVariable
								+ "' (missing second '}')");
				}
				break;
		}
		parser.skip = false;
		return null;
	}

	private String processVar(String varName) {
		String str = values.get(varName);
		if (str == null) {
			throw new JadxRuntimeException("Unknown variable: '" + varName
					+ "' in template: " + templateName);
		}
		return str;
	}
}
