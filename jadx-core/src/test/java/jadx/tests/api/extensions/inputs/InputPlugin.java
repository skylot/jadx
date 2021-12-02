package jadx.tests.api.extensions.inputs;

import java.util.function.Consumer;

import jadx.tests.api.IntegrationTest;

public enum InputPlugin implements Consumer<IntegrationTest> {
	DEX {
		@Override
		public void accept(IntegrationTest test) {
			test.useDexInput();
		}
	},
	JAVA {
		@Override
		public void accept(IntegrationTest test) {
			test.useJavaInput();
		}
	};
}
