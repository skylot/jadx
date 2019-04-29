package jadx.tests.api.compiler;

import java.net.URI;

import javax.tools.SimpleJavaFileObject;

public class CharSequenceJavaFileObject extends SimpleJavaFileObject {

	private CharSequence content;

	public CharSequenceJavaFileObject(String className, CharSequence content) {
		super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
		this.content = content;
	}

	@Override
	public CharSequence getCharContent(boolean ignoreEncodingErrors) {
		return content;
	}
}
