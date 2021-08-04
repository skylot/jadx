package jadx.gui.utils;

import java.awt.Rectangle;
import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class RectangleTypeAdapter {

	private static final TypeAdapter<Rectangle> SINGLETON = new TypeAdapter<Rectangle>() {
		@Override
		public void write(JsonWriter out, Rectangle value) throws IOException {
			if (value == null) {
				out.nullValue();
			} else {
				out.beginObject();
				out.name("x").value(value.getX());
				out.name("y").value(value.getY());
				out.name("width").value(value.getWidth());
				out.name("height").value(value.getHeight());
				out.endObject();
			}
		}

		@Override
		public Rectangle read(JsonReader in) throws IOException {
			if (in.peek() == JsonToken.NULL) {
				in.nextNull();
				return null;
			}
			in.beginObject();
			Rectangle rectangle = new Rectangle();
			while (in.hasNext()) {
				String name = in.nextName();
				switch (name) {
					case "x":
						rectangle.x = in.nextInt();
						break;
					case "y":
						rectangle.y = in.nextInt();
						break;
					case "width":
						rectangle.width = in.nextInt();
						break;
					case "height":
						rectangle.height = in.nextInt();
						break;

					default:
						throw new IllegalArgumentException("Unknown field in Rectangle: " + name);
				}
			}
			in.endObject();
			return rectangle;
		}
	};

	public static TypeAdapter<Rectangle> singleton() {
		return SINGLETON;
	}

	private RectangleTypeAdapter() {
	}
}
