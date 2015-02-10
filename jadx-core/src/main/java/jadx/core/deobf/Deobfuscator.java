package jadx.core.deobf;

public class Deobfuscator {

	private static final StubDeobfuscator stubDeobfuscator;
	private static IDeobfuscator deobfuscatorInstance;

	static {
		stubDeobfuscator = new StubDeobfuscator();
		deobfuscatorInstance = stubDeobfuscator;
	}
	
	/**
	 * Gets instance of active deobfuscator
	 * 
	 * @return deobfuscator instance
	 */
	public static IDeobfuscator instance() {
		return deobfuscatorInstance;
	}

	/**
	 * Sets active deobfuscator
	 * 
	 * @param deobfuscator  object that makes deobfuscation or {@code null} 
	 * to set stub deobfuscator 
	 * 
	 */
	public static void setDeobfuscator(IDeobfuscator deobfuscator) {
		if (deobfuscator != null) {
			deobfuscatorInstance = deobfuscator;
		} else {
			deobfuscatorInstance = stubDeobfuscator;
		}
	}

}
