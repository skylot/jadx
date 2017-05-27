package jadx.tests.integration.inline;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.junit.Assert.assertThat;

public class TestIssue86 extends IntegrationTest {

	public static class TestCls {
		private static final String SERVER_ERR = "server-err";
		private static final String NOT_FOUND = "not-found";
		private static final String LIST_TAG = "list-tag";
		private static final String TEMP_TAG = "temp-tag";
		private static final String MIN_TAG = "min-tag";
		private static final String MAX_TAG = "max-tag";
		private static final String MILLIS_TAG = "millis-tag";
		private static final String WEATHER_TAG = "weather-tag";
		private static final String DESC_TAG = "desc-tag";

		private List<Day> test(String response) {
			List<Day> reportList = new ArrayList<>();
			try {
				System.out.println(response);
				if (response != null
						&& (response.startsWith(SERVER_ERR)
						|| response.startsWith(NOT_FOUND))) {
					return reportList;
				}
				JSONObject jsonObj = new JSONObject(response);
				JSONArray days = jsonObj.getJSONArray(LIST_TAG);
				for (int i = 0; i < days.length(); i++) {
					JSONObject c = days.getJSONObject(i);
					long millis = c.getLong(MILLIS_TAG);
					JSONObject temp = c.getJSONObject(TEMP_TAG);
					String max = temp.getString(MAX_TAG);
					String min = temp.getString(MIN_TAG);
					JSONArray weather = c.getJSONArray(WEATHER_TAG);
					String weatherDesc = weather.getJSONObject(0).getString(DESC_TAG);
					Day d = new Day();
					d.setMilis(millis);
					d.setMinTmp(min);
					d.setMaxTmp(max);
					d.setWeatherDesc(weatherDesc);
					reportList.add(d);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return reportList;
		}

		private static class Day {
			public void setMilis(long milis) {
			}

			public void setMinTmp(String min) {
			}

			public void setMaxTmp(String max) {
			}

			public void setWeatherDesc(String weatherDesc) {
			}
		}

		private static class JSONObject {
			public JSONObject(String response) {
			}

			public JSONArray getJSONArray(String tag) throws JSONException {
				return null;
			}

			public JSONObject getJSONObject(String tag) throws JSONException {
				return null;
			}

			public String getString(String tag) throws JSONException {
				return null;
			}

			public long getLong(String tag) throws JSONException {
				return 0;
			}
		}

		private class JSONArray {
			public JSONObject getJSONObject(int i) throws JSONException {
				return null;
			}

			public int length() {
				return 0;
			}
		}

		private class JSONException extends Exception {
			private static final long serialVersionUID = -4358405506584551910L;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("response.startsWith(NOT_FOUND)"));
	}
}
