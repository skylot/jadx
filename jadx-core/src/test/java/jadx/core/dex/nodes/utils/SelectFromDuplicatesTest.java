package jadx.core.dex.nodes.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.RootNode;
import jadx.tests.api.utils.TestUtils;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

class SelectFromDuplicatesTest {

	private RootNode root;

	@BeforeEach
	public void init() {
		JadxArgs args = new JadxArgs();
		args.addInputFile(TestUtils.getFileForSample("test-samples/hello.dex"));
		JadxDecompiler decompiler = new JadxDecompiler(args);
		decompiler.load();
		root = decompiler.getRoot();
	}

	@Test
	void testSelectBySource() {
		selectBySources(0, false, "classes.dex", "classes2.dex");
		selectBySources(2, false, "classes10.dex", "classes20.dex", "classes2.dex");
	}

	@RepeatedTest(10)
	void testSelectBySourceShuffled() {
		selectFirstByShuffleSources("classes.dex", "classes2.dex", "classes4.dex");
		selectFirstByShuffleSources("classes2.dex", "classes10.dex", "classes20.dex");
		selectFirstByShuffleSources("classes10.dex", "classes1.dex", "classes01.dex", "classes000.dex", "classes02.dex");
	}

	private void selectFirstByShuffleSources(String... sources) {
		selectBySources(0, true, sources);
	}

	private void selectBySources(int selectedPos, boolean shuffle, String... sources) {
		List<ClassNode> clsList = Arrays.stream(sources)
				.map(this::buildClassNodeBySource)
				.collect(Collectors.toList());
		ClassNode expected = clsList.get(selectedPos);
		if (shuffle) {
			Collections.shuffle(clsList, new Random(System.currentTimeMillis() + System.nanoTime()));
		}
		ClassNode selectedCls = SelectFromDuplicates.process(clsList);
		assertThat(selectedCls)
				.describedAs("Expect %s, but got %s from list: %s", expected, selectedCls, clsList)
				.isSameAs(expected);
	}

	private ClassNode buildClassNodeBySource(String clsSource) {
		ClassInfo clsInfo = ClassInfo.fromName(root, "ClassFromSource:" + clsSource);
		ClassNode cls = ClassNode.addSyntheticClass(root, clsInfo, 0);
		cls.setInputFileName(clsSource);
		return cls;
	}
}
