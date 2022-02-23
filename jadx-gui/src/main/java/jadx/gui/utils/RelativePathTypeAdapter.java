package jadx.gui.utils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class RelativePathTypeAdapter extends TypeAdapter<Path> {

	private static final Logger LOG = LoggerFactory.getLogger(RelativePathTypeAdapter.class);

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
			Path resultPath;
			try {
				resultPath = basePath.relativize(value);
			} catch (IllegalArgumentException e) {
				LOG.warn("Unable to build a relative path to {} - using absolute path", value);
				resultPath = value;
			}
			out.value(resultPath.toString());
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
