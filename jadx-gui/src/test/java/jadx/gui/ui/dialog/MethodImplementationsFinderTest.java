package jadx.gui.ui.dialog;

import java.awt.event.KeyEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jadx.api.JadxDecompiler;
import jadx.api.JavaMethod;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.ProcessState;
import jadx.core.dex.nodes.RootNode;
import jadx.gui.plugins.context.TestMainWindowShim;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JMethod;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.action.ActionModel;
import jadx.gui.ui.action.FindImplementationsAction;
import jadx.gui.utils.NLS;
import jadx.tests.api.IntegrationTest;
import jadx.tests.api.compiler.TestCompiler;

import static org.assertj.core.api.Assertions.assertThat;

class MethodImplementationsFinderTest extends IntegrationTest {

	@TempDir
	Path localTestDir;

	@SuppressWarnings("unused")
	public static class TestHierarchy {
		public interface Worker {
			void work();

			void work(int count);

			default void describe() {
			}
		}

		public interface RichWorker extends Worker {
			@Override
			default void describe() {
			}
		}

		public abstract static class AbstractBase implements Worker {
			@Override
			public abstract void work();

			@Override
			public void work(int count) {
			}
		}

		public abstract static class AbstractSub extends AbstractBase {
			@Override
			public abstract void work();
		}

		public static class ConcreteMid extends AbstractSub {
			@Override
			public void work() {
			}
		}

		public static class ConcreteLeaf extends ConcreteMid {
			@Override
			public void work() {
			}

			@Override
			public void work(int count) {
			}
		}

		public static class SiblingImpl implements RichWorker {
			@Override
			public void work() {
			}

			@Override
			public void work(int count) {
			}

			@Override
			public void describe() {
			}
		}

		public static class PlainRichWorker implements RichWorker {
			@Override
			public void work() {
			}

			@Override
			public void work(int count) {
			}
		}

		public static class UnrelatedWorker {
			public void work() {
			}

			public static void helper() {
			}

			private void secret() {
			}
		}

		public static Worker makeAnonWorker() {
			return new Worker() {
				@Override
				public void work() {
				}

				@Override
				public void work(int count) {
				}
			};
		}

		public interface Box<T> {
			T get();
		}

		public static class StringBox implements Box<String> {
			@Override
			public String get() {
				return "ok";
			}
		}

		public interface ValueConsumer<T> {
			void accept(T value);
		}

		public static class StringConsumer implements ValueConsumer<String> {
			@Override
			public void accept(String value) {
			}
		}

		public static class SubStringConsumer extends StringConsumer {
			@Override
			public void accept(String value) {
			}
		}

		public interface Producer {
			Object produce();
		}

		public static class StringProducer implements Producer {
			@Override
			public String produce() {
				return "produced";
			}
		}
	}

	@Test
	void testImplementationDiscovery() {
		disableCompilation();
		ClassNode hierarchyCls = getClassNode(TestHierarchy.class);
		assertImplementationsForHierarchy(jadxDecompiler, hierarchyCls);
	}

	@Test
	void testImplementationDiscoveryJavaInput() {
		useJavaInput();
		disableCompilation();
		ClassNode hierarchyCls = getClassNode(TestHierarchy.class);
		assertImplementationsForHierarchy(jadxDecompiler, hierarchyCls);
	}

	@Test
	void testGenericBridgeImplementation() throws Exception {
		List<File> inputFiles = compileHierarchyClasses();
		try (JadxDecompiler decompiler = loadFiles(inputFiles)) {
			RootNode root = decompiler.getRoot();
			for (ClassNode cls : root.getClasses(true)) {
				assertThat(cls.getState()).isEqualTo(ProcessState.NOT_LOADED);
			}
			ClassNode hierarchyCls = root.resolveClass(TestHierarchy.class.getName());
			assertThat(hierarchyCls).isNotNull();

			// Before decompilation (ProcessState.NOT_LOADED)
			assertGenericBridgeImplementations(decompiler, hierarchyCls, false);

			// After decompilation
			decompiler.getClasses().forEach(jadx.api.JavaClass::decompile);
			assertGenericBridgeImplementations(decompiler, hierarchyCls, true);

			// After ClassNode.unload()
			decompiler.getClasses().forEach(c -> c.getClassNode().unload());
			assertGenericBridgeImplementations(decompiler, hierarchyCls, true);
		}
	}

	@Test
	void testImplementationDiscoveryBeforeDecompilationAndAfterUnload() throws Exception {
		List<File> inputFiles = compileHierarchyClasses();
		try (JadxDecompiler decompiler = loadFiles(inputFiles)) {
			RootNode root = decompiler.getRoot();
			for (ClassNode cls : root.getClasses(true)) {
				assertThat(cls.getState()).isEqualTo(ProcessState.NOT_LOADED);
			}
			ClassNode hierarchyCls = root.resolveClass(TestHierarchy.class.getName());
			assertThat(hierarchyCls).isNotNull();

			assertImplementationsForHierarchy(decompiler, hierarchyCls);

			// Decompiling & unloading classes via ClassNode.unload() must preserve MethodOverrideAttr
			// (PinnedAttribute)
			decompiler.getClasses().forEach(c -> {
				c.decompile();
				c.getClassNode().unload();
			});
			assertImplementationsForHierarchy(decompiler, hierarchyCls);
		}
	}

	@Test
	void testActionModelAndEnabledState() {
		NLS.setLocale(NLS.defaultLocale());
		assertThat(ActionModel.FIND_IMPLEMENTATIONS.getDefaultShortcut().toKeyStroke())
				.isEqualTo(KeyStroke.getKeyStroke(KeyEvent.VK_I, 0));

		disableCompilation();
		ClassNode hierarchyCls = getClassNode(TestHierarchy.class);
		ClassNode workerCls = findInnerClass(hierarchyCls, "Worker");
		ClassNode leafCls = findInnerClass(hierarchyCls, "ConcreteLeaf");

		JavaMethod workerWork = toJavaMethod(findMethod(workerCls, "work()V"));
		JavaMethod leafWork = toJavaMethod(findMethod(leafCls, "work()V"));

		JMethod jWorkerWork = new JMethod(workerWork, new JClass(workerWork.getDeclaringClass(), null, null));
		JMethod jLeafWork = new JMethod(leafWork, new JClass(leafWork.getDeclaringClass(), null, null));
		JClass jWorkerCls = new JClass(workerWork.getDeclaringClass(), null, null);

		FindImplementationsAction action = new FindImplementationsAction(null);
		assertThat(action.isActionEnabled(null)).isFalse();
		assertThat(action.isActionEnabled(jWorkerCls)).isFalse();
		assertThat(action.isActionEnabled(jWorkerWork)).isTrue();
		assertThat(action.isActionEnabled(jLeafWork)).isFalse();

		MainWindow mainWindow = TestMainWindowShim.build();
		JPopupMenu workerMenu = jWorkerWork.onTreePopupMenu(mainWindow);
		JMenuItem workerImplItem = (JMenuItem) workerMenu.getComponent(workerMenu.getComponentCount() - 1);
		assertThat(workerImplItem.getText()).isEqualTo(NLS.str("popup.find_implementations"));
		assertThat(workerImplItem.isEnabled()).isTrue();

		JPopupMenu leafMenu = jLeafWork.onTreePopupMenu(mainWindow);
		JMenuItem leafImplItem = (JMenuItem) leafMenu.getComponent(leafMenu.getComponentCount() - 1);
		assertThat(leafImplItem.getText()).isEqualTo(NLS.str("popup.find_implementations"));
		assertThat(leafImplItem.isEnabled()).isFalse();
	}

	private void assertGenericBridgeImplementations(JadxDecompiler decompiler, ClassNode hierarchyCls, boolean decompiled) {
		ClassNode boxCls = findInnerClass(hierarchyCls, "Box");
		ClassNode stringBoxCls = findInnerClass(hierarchyCls, "StringBox");
		ClassNode consumerCls = findInnerClass(hierarchyCls, "ValueConsumer");
		ClassNode stringConsumerCls = findInnerClass(hierarchyCls, "StringConsumer");
		ClassNode subStringConsumerCls = findInnerClass(hierarchyCls, "SubStringConsumer");

		MethodNode boxGet = findMethod(boxCls, "get()Ljava/lang/Object;");
		MethodNode stringBoxGet = findMethod(stringBoxCls, "get()Ljava/lang/String;");

		MethodNode consumerAccept = findMethod(consumerCls, "accept(Ljava/lang/Object;)V");
		MethodNode stringConsumerAccept = findMethod(stringConsumerCls, "accept(Ljava/lang/String;)V");
		MethodNode stringConsumerBridge = findMethod(stringConsumerCls, "accept(Ljava/lang/Object;)V");
		MethodNode subStringConsumerAccept = findMethod(subStringConsumerCls, "accept(Ljava/lang/String;)V");

		List<MethodNode> boxImpls = MethodImplementationsFinder.collectImplementations(boxGet);
		assertThat(boxImpls).containsExactly(stringBoxGet);
		assertThat(stringBoxGet.contains(AFlag.DONT_GENERATE)).isFalse();
		assertThat(MethodImplementationsFinder.hasImplementations(boxGet)).isTrue();
		assertThat(MethodImplementationsFinder.collectImplementations((JavaMethod) decompiler.getJavaNodeByRef(boxGet)))
				.extracting(JavaMethod::getMethodNode)
				.containsExactly(stringBoxGet);

		List<MethodNode> consumerImpls = MethodImplementationsFinder.collectImplementations(consumerAccept);
		assertThat(consumerImpls).containsExactlyInAnyOrder(stringConsumerAccept, subStringConsumerAccept);
		assertThat(consumerImpls).allSatisfy(m -> assertThat(m.contains(AFlag.DONT_GENERATE)).isFalse());
		assertThat(MethodImplementationsFinder.hasImplementations(consumerAccept)).isTrue();
		assertThat(MethodImplementationsFinder.collectImplementations((JavaMethod) decompiler.getJavaNodeByRef(consumerAccept)))
				.extracting(JavaMethod::getMethodNode)
				.containsExactlyInAnyOrder(stringConsumerAccept, subStringConsumerAccept);

		// Querying via either the concrete method or its bridge NodeDeclareRef returns only the subclass
		// override
		assertThat(MethodImplementationsFinder.collectImplementations(stringConsumerAccept))
				.containsExactly(subStringConsumerAccept);
		assertThat(MethodImplementationsFinder.collectImplementations(stringConsumerBridge))
				.containsExactly(subStringConsumerAccept);

		// Leaf box & consumer implementations have no overrides
		assertThat(MethodImplementationsFinder.hasImplementations(stringBoxGet)).isFalse();
		assertThat(MethodImplementationsFinder.hasImplementations(subStringConsumerAccept)).isFalse();

		// Non-generic covariant return bridge must not duplicate the concrete implementation before or
		// after decompilation
		ClassNode producerCls = findInnerClass(hierarchyCls, "Producer");
		ClassNode stringProducerCls = findInnerClass(hierarchyCls, "StringProducer");
		MethodNode producerProduce = findMethod(producerCls, "produce()Ljava/lang/Object;");
		MethodNode stringProducerProduce = findMethod(stringProducerCls, "produce()Ljava/lang/String;");
		assertThat(MethodImplementationsFinder.collectImplementations(producerProduce))
				.containsExactly(stringProducerProduce);
		assertThat(MethodImplementationsFinder.hasImplementations(stringProducerProduce)).isFalse();

		if (decompiled) {
			// MethodGen attaches NodeDeclareRef to MethodReplaceAttr.getReplaceMth() for bridge-merged
			// methods, so resolveJumpMethod / resolveDefPosition must yield a non-zero definition offset.
			assertThat(MethodImplementationsFinder.resolveDefPosition(stringBoxGet)).isGreaterThan(0);
			assertThat(MethodImplementationsFinder.resolveDefPosition(stringConsumerAccept)).isGreaterThan(0);
			assertThat(MethodImplementationsFinder.resolveDefPosition(subStringConsumerAccept)).isGreaterThan(0);
			assertThat(MethodImplementationsFinder.resolveDefPosition(stringProducerProduce)).isGreaterThan(0);
			assertThat(MethodImplementationsFinder.resolveJumpMethod(stringBoxGet).getDefPosition()).isGreaterThan(0);
			assertThat(MethodImplementationsFinder.resolveJumpMethod(stringConsumerAccept).getDefPosition()).isGreaterThan(0);
			assertThat(MethodImplementationsFinder.resolveJumpMethod(stringProducerProduce).getDefPosition()).isGreaterThan(0);
		}
	}

	private void assertImplementationsForHierarchy(JadxDecompiler decompiler, ClassNode hierarchyCls) {
		ClassNode workerCls = findInnerClass(hierarchyCls, "Worker");
		ClassNode richWorkerCls = findInnerClass(hierarchyCls, "RichWorker");
		ClassNode absBaseCls = findInnerClass(hierarchyCls, "AbstractBase");
		ClassNode absSubCls = findInnerClass(hierarchyCls, "AbstractSub");
		ClassNode midCls = findInnerClass(hierarchyCls, "ConcreteMid");
		ClassNode leafCls = findInnerClass(hierarchyCls, "ConcreteLeaf");
		ClassNode siblingCls = findInnerClass(hierarchyCls, "SiblingImpl");
		ClassNode plainRichCls = findInnerClass(hierarchyCls, "PlainRichWorker");
		ClassNode unrelatedCls = findInnerClass(hierarchyCls, "UnrelatedWorker");
		ClassNode anonCls = findAnonymousClass(hierarchyCls);

		MethodNode workerWork = findMethod(workerCls, "work()V");
		MethodNode workerWorkInt = findMethod(workerCls, "work(I)V");
		MethodNode workerDescribe = findMethod(workerCls, "describe()V");
		MethodNode richDescribe = findMethod(richWorkerCls, "describe()V");
		MethodNode absBaseWork = findMethod(absBaseCls, "work()V");
		MethodNode absBaseWorkInt = findMethod(absBaseCls, "work(I)V");
		MethodNode absSubWork = findMethod(absSubCls, "work()V");
		MethodNode midWork = findMethod(midCls, "work()V");
		MethodNode leafWork = findMethod(leafCls, "work()V");
		MethodNode leafWorkInt = findMethod(leafCls, "work(I)V");
		MethodNode siblingWork = findMethod(siblingCls, "work()V");
		MethodNode siblingWorkInt = findMethod(siblingCls, "work(I)V");
		MethodNode siblingDescribe = findMethod(siblingCls, "describe()V");
		MethodNode plainRichWork = findMethod(plainRichCls, "work()V");
		MethodNode plainRichWorkInt = findMethod(plainRichCls, "work(I)V");
		MethodNode unrelatedWork = findMethod(unrelatedCls, "work()V");
		MethodNode unrelatedHelper = findMethod(unrelatedCls, "helper()V");
		MethodNode unrelatedSecret = findMethod(unrelatedCls, "secret()V");
		MethodNode unrelatedCtor = findMethod(unrelatedCls, "<init>()V");
		MethodNode anonWork = findMethod(anonCls, "work()V");
		MethodNode anonWorkInt = findMethod(anonCls, "work(I)V");

		// Interface -> all concrete implementations (excludes abstract classes, unrelated classes,
		// overloaded signatures)
		assertThat(MethodImplementationsFinder.hasImplementations(workerWork)).isTrue();
		assertThat(MethodImplementationsFinder.collectImplementations(workerWork))
				.containsExactlyInAnyOrder(midWork, leafWork, siblingWork, plainRichWork, anonWork);
		assertThat(MethodImplementationsFinder.collectImplementations(workerWorkInt))
				.containsExactlyInAnyOrder(absBaseWorkInt, leafWorkInt, siblingWorkInt, plainRichWorkInt, anonWorkInt);

		// Interface default method -> subinterface default override + concrete override
		assertThat(MethodImplementationsFinder.collectImplementations(workerDescribe))
				.containsExactlyInAnyOrder(richDescribe, siblingDescribe);
		// Subinterface default method -> excludes parent default and self, includes only concrete override
		assertThat(MethodImplementationsFinder.collectImplementations(richDescribe))
				.containsExactlyInAnyOrder(siblingDescribe);

		// Abstract base & intermediate abstract redeclaration -> excludes self and abstract
		// sub-declarations
		assertThat(MethodImplementationsFinder.collectImplementations(absBaseWork))
				.containsExactlyInAnyOrder(midWork, leafWork);
		assertThat(MethodImplementationsFinder.collectImplementations(absBaseWorkInt))
				.containsExactlyInAnyOrder(leafWorkInt);
		assertThat(MethodImplementationsFinder.collectImplementations(absSubWork))
				.containsExactlyInAnyOrder(midWork, leafWork);

		// Multi-level concrete class -> includes only subclass overrides, excludes parent/interface/sibling
		assertThat(MethodImplementationsFinder.collectImplementations(midWork))
				.containsExactlyInAnyOrder(leafWork);

		// Leaf implementations & siblings -> no implementations below them
		assertThat(MethodImplementationsFinder.hasImplementations(leafWork)).isFalse();
		assertThat(MethodImplementationsFinder.collectImplementations(leafWork)).isEmpty();
		assertThat(MethodImplementationsFinder.hasImplementations(siblingWork)).isFalse();
		assertThat(MethodImplementationsFinder.collectImplementations(siblingWork)).isEmpty();

		// Unrelated, static, private, constructor, and null -> no implementations
		assertThat(MethodImplementationsFinder.hasImplementations(unrelatedWork)).isFalse();
		assertThat(MethodImplementationsFinder.hasImplementations(unrelatedHelper)).isFalse();
		assertThat(MethodImplementationsFinder.hasImplementations(unrelatedSecret)).isFalse();
		assertThat(MethodImplementationsFinder.hasImplementations(unrelatedCtor)).isFalse();
		assertThat(MethodImplementationsFinder.hasImplementations((MethodNode) null)).isFalse();
		assertThat(MethodImplementationsFinder.collectImplementations((MethodNode) null)).isEmpty();
		assertThat(MethodImplementationsFinder.hasImplementations((JavaMethod) null)).isFalse();
		assertThat(MethodImplementationsFinder.collectImplementations((JavaMethod) null)).isEmpty();

		// JavaMethod overloads work identically
		JavaMethod workerWorkJava = (JavaMethod) decompiler.getJavaNodeByRef(workerWork);
		assertThat(MethodImplementationsFinder.hasImplementations(workerWorkJava)).isTrue();
		assertThat(MethodImplementationsFinder.collectImplementations(workerWorkJava))
				.extracting(JavaMethod::getMethodNode)
				.containsExactlyInAnyOrder(midWork, leafWork, siblingWork, plainRichWork, anonWork);
	}

	private List<File> compileHierarchyClasses() throws Exception {
		File sourceFile = new File("src/test/java/"
				+ MethodImplementationsFinderTest.class.getName().replace('.', '/') + ".java");
		assertThat(sourceFile).exists();
		Path outTmp = Files.createTempDirectory(localTestDir, "impl-finder-classes");
		try (TestCompiler compiler = new TestCompiler(getCompilerOptions())) {
			List<File> files = compiler.compileFiles(List.of(sourceFile), outTmp);
			return files.stream()
					.filter(f -> f.getName().contains("TestHierarchy"))
					.collect(Collectors.toList());
		}
	}

	private static ClassNode findInnerClass(ClassNode parent, String shortName) {
		return parent.getInnerClasses().stream()
				.filter(c -> c.getClassInfo().getShortName().equals(shortName))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Inner class not found: " + shortName));
	}

	private static ClassNode findAnonymousClass(ClassNode parent) {
		return parent.getInnerClasses().stream()
				.filter(c -> c.getClassInfo().getRawName().endsWith("$1"))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Anonymous class not found in " + parent));
	}

	private static MethodNode findMethod(ClassNode cls, String shortId) {
		MethodNode mth = cls.searchMethodByShortId(shortId);
		assertThat(mth).as("Method %s in %s", shortId, cls).isNotNull();
		return mth;
	}
}
