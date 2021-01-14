package jadx.api.plugins.input.data;

public enum MethodHandleType {
	STATIC_PUT,
	STATIC_GET,
	INSTANCE_PUT,
	INSTANCE_GET,
	INVOKE_STATIC,
	INVOKE_INSTANCE,
	INVOKE_DIRECT,
	INVOKE_CONSTRUCTOR,
	INVOKE_INTERFACE;

	public boolean isField() {
		switch (this) {
			case STATIC_PUT:
			case STATIC_GET:
			case INSTANCE_PUT:
			case INSTANCE_GET:
				return true;

			default:
				return false;
		}
	}
}
