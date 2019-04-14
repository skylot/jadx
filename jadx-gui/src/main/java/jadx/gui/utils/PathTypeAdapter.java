package jadx.gui.utils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class PathTypeAdapter {

	private static final TypeAdapter<Path> SINGLETON = new TypeAdapter<Path>() {
		@Override
		public void write(JsonWriter out, Path value) throws IOException {
			if (value == null) {
				out.nullValue();
			} else {
				out.value(value.toAbsolutePath().toString());
			}
		}

		@Override
		public Path read(JsonReader in) throws IOException {
			if (in.peek() == JsonToken.NULL) {
				in.nextNull();
				return null;
			}
			return Paths.get(in.nextString());
		}
	};

	public static TypeAdapter<Path> singleton() {
		return SINGLETON;
	}

	private PathTypeAdapter() {
	}
}
