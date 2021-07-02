package jadx.gui.plugins.quark;

import java.util.List;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

import jadx.core.utils.Utils;

@SuppressWarnings("MemberName")
public class QuarkReportData {
	public static class Crime {
		public String crime;
		public String confidence;
		public List<String> permissions;

		List<Method> native_api;
		List<Method> combination;
		List<Map<String, InvokePlace>> register;
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
}
