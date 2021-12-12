package jadx.tests.integration.inner;

import org.junit.jupiter.api.Test;

import jadx.api.CommentsLevel;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestAnonymousClass18 extends IntegrationTest {

	@SuppressWarnings({ "Convert2Lambda", "Anonymous2MethodRef", "unused" })
	public static class TestCls {

		public interface Job {
			void executeJob();
		}

		public void start() {
			runJob(new Job() {
				@Override
				public void executeJob() {
					runJob(new Job() {
						@Override
						public void executeJob() {
							doSomething();
						}
					});
				}

				private void doSomething() {
				}
			});
		}

		public static void runJob(Job job) {
		}
	}

	@Test
	public void test() {
		getArgs().setCommentsLevel(CommentsLevel.WARN);
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("AnonymousClass1.this")
				.doesNotContain("class AnonymousClass1")
				// .doesNotContain("TestAnonymousClass18$TestCls.runJob(") // TODO: ???
				.containsOne(indent() + "doSomething();");
	}

	@Test
	public void testNoInline() {
		getArgs().setInlineAnonymousClasses(false);
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("class AnonymousClass1 implements Job {")
				.containsOne("class C00001 implements Job {")
				.containsOne("AnonymousClass1.this.doSomething();");
	}
}
