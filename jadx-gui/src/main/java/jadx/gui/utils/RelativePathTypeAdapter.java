package jadx.gui.utils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class RelativePathTypeAdapter extends TypeAdapter<Path> {
	private final Path basePath;

	public RelativePathTypeAdapter(Path basePath) {
		this.basePath = Objects.requireNonNull(basePath);
	}

	@Override
	public void write(JsonWriter out, Path value) throws IOException {
		if (value == null) {
			out.nullValue();
		} else {
			value = value.toAbsolutePath().normalize();
			String relativePath = basePath.relativize(value).toString();
			out.value(relativePath);
		}
	}

	@Override
	public Path read(JsonReader in) throws IOException {
		if (in.peek() == JsonToken.NULL) {
			in.nextNull();
			return null;
		}
		Path p = Paths.get(in.nextString());
		if (p.isAbsolute()) {
			return p;
		}
		return basePath.resolve(p);
	}

}
