package jadx.mcp.samples;

@SuppressWarnings("unused")
public class SampleUser {

	public int start() {
		SampleService service = new SampleService();
		service.run();
		SampleService.Helper helper = new SampleService.Helper();
		return helper.help("user") + service.getCounter();
	}
}
