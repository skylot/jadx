package jadx.mcp.samples;

@SuppressWarnings("unused")
public class SampleService {
	private static final String TAG = "SampleService";
	public static final String PADDING = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";

	private int counter;
	private final Helper helper = new Helper();

	public int compute(int a) {
		return a * 2 + counter;
	}

	public int compute(int a, int b) {
		return a + b;
	}

	public String compute(String s) {
		return s + TAG;
	}

	public void run() {
		String message = "hello-mcp-" + counter;
		counter = compute(5) + helper.help(message);
		Listener listener = new Listener() {
			@Override
			public void onEvent(String event) {
				counter += event.length();
			}
		};
		listener.onEvent(message);
	}

	public int getCounter() {
		return counter;
	}

	public interface Listener {
		void onEvent(String event);
	}

	public static class Helper {
		public int help(String text) {
			int length = text.length();
			return length > 3 ? length : -1;
		}
	}
}
