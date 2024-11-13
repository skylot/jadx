package jadx.core.utils;

import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class GsonUtils {

	public static Gson buildGson() {
		return defaultGsonBuilder().create();
	}

	public static GsonBuilder defaultGsonBuilder() {
		return new GsonBuilder()
				.disableJdkUnsafe()
				.setPrettyPrinting();
	}

	public static void fillObjectFromJsonString(GsonBuilder builder, Object obj, String jsonStr) {
		Class<?> type = obj.getClass();
		Gson gson = builder.registerTypeAdapter(type, (InstanceCreator<?>) t -> obj).create();
		gson.fromJson(jsonStr, type);
	}

	public static <T> InterfaceReplace<T> interfaceReplace(Class<T> replaceCls) {
		return new InterfaceReplace<>(replaceCls);
	}

	public static final class InterfaceReplace<T> implements JsonSerializer<T>, JsonDeserializer<T> {
		private final Class<T> replaceCls;

		private InterfaceReplace(Class<T> replaceCls) {
			this.replaceCls = replaceCls;
		}

		@Override
		public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			return context.deserialize(json, this.replaceCls);
		}

		@Override
		public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
			return context.serialize(src, this.replaceCls);
		}
	}
}
