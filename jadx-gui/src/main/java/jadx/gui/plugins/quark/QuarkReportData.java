package jadx.gui.plugins.quark;

import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import jadx.core.utils.Utils;

@SuppressWarnings("MemberName")
public class QuarkReportData {

	public static class Crime {
		public String crime;
		public String confidence;
		public List<String> permissions;

		List<Method> native_api;
		List<JsonElement> combination;
		List<Map<String, InvokePlace>> register;

		public int parseConfidence() {
			return Integer.parseInt(confidence.replace("%", ""));
		}

		@Override
		public String toString() {
			final StringBuffer sb = new StringBuffer("Crime{");
			sb.append("crime='").append(crime).append('\'');
			sb.append(", confidence='").append(confidence).append('\'');
			sb.append(", permissions=").append(permissions);
			sb.append(", native_api=").append(native_api);
			sb.append(", combination=").append(combination);
			sb.append(", register=").append(register);
			sb.append('}');
			return sb.toString();
		}
	}

	public static class Method {
		@SerializedName("class")
		String cls;
		String method;
		String descriptor;

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(Utils.cleanObjectName(cls)).append(".").append(method);
			if (descriptor != null) {
				sb.append(descriptor);
			}
			return sb.toString();
		}
	}

	public static class InvokePlace {
		List<String> first;
		List<String> second;
	}

	String apk_filename;
	String threat_level;
	int total_score;
	List<Crime> crimes;

	public void validate() {
		if (crimes == null) {
			throw new RuntimeException("Invalid data: \"crimes\" list missing");
		}
		for (Crime crime : crimes) {
			if (crime.confidence == null) {
				throw new RuntimeException("Confidence value missing: " + crime);
			}
			try {
				crime.parseConfidence();
			} catch (Exception e) {
				throw new RuntimeException("Invalid crime entry: " + crime);
			}
		}

	}

}
