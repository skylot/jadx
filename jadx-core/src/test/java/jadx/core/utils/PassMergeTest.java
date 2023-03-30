package jadx.core.utils;

import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.api.impl.passes.DecompilePassWrapper;
import jadx.api.plugins.pass.JadxPass;
import jadx.api.plugins.pass.JadxPassInfo;
import jadx.api.plugins.pass.impl.OrderedJadxPassInfo;
import jadx.api.plugins.pass.impl.SimpleJadxPassInfo;
import jadx.api.plugins.pass.types.JadxDecompilePass;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.dex.visitors.IDexTreeVisitor;
import jadx.core.utils.exceptions.JadxRuntimeException;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class PassMergeTest {

	@Test
	public void testSimple() {
		List<String> base = asList("a", "b", "c");
		check(base, mockPass("x"), asList("a", "b", "c", "x"));
		check(base, mockPass(mockInfo("x").after(JadxPassInfo.START)), asList("x", "a", "b", "c"));
		check(base, mockPass(mockInfo("x").before(JadxPassInfo.END)), asList("a", "b", "c", "x"));
	}

	@Test
	public void testSingle() {
		List<String> base = asList("a", "b", "c");
		check(base, mockPass(mockInfo("x").after("a")), asList("a", "x", "b", "c"));
		check(base, mockPass(mockInfo("x").before("c")), asList("a", "b", "x", "c"));
		check(base, mockPass(mockInfo("x").before("a")), asList("x", "a", "b", "c"));
		check(base, mockPass(mockInfo("x").after("c")), asList("a", "b", "c", "x"));
	}

	@Test
	public void testMulti() {
		List<String> base = asList("a", "b", "c");
		JadxPass x = mockPass(mockInfo("x").after("a"));
		JadxPass y = mockPass(mockInfo("y").after("a"));
		JadxPass z = mockPass(mockInfo("z").before("b"));
		check(base, asList(x, y, z), asList("a", "y", "x", "z", "b", "c"));
	}

	@Test
	public void testMultiWithDeps() {
		List<String> base = asList("a", "b", "c");
		JadxPass x = mockPass(mockInfo("x").after("a"));
		JadxPass y = mockPass(mockInfo("y").after("x"));
		JadxPass z = mockPass(mockInfo("z").before("b").after("y"));
		check(base, asList(x, y, z), asList("a", "x", "y", "z", "b", "c"));
	}

	@Test
	public void testMultiWithDeps2() {
		List<String> base = asList("a", "b", "c");
		JadxPass x = mockPass(mockInfo("x").before("y"));
		JadxPass y = mockPass(mockInfo("y").before("b"));
		JadxPass z = mockPass(mockInfo("z").after("y"));
		check(base, asList(x, y, z), asList("a", "x", "y", "z", "b", "c"));
	}

	@Test
	public void testMultiWithDeps3() {
		List<String> base = asList("a", "b", "c");
		JadxPass x = mockPass(mockInfo("x"));
		JadxPass y = mockPass(mockInfo("y").after("x").before("b"));
		check(base, asList(x, y), asList("a", "x", "y", "b", "c"));
	}

	@Test
	public void testLoop() {
		List<String> base = asList("a", "b", "c");
		JadxPass x = mockPass(mockInfo("x").before("y"));
		JadxPass y = mockPass(mockInfo("y").before("x"));
		Throwable thrown = catchThrowable(() -> check(base, asList(x, y), emptyList()));
		assertThat(thrown).isInstanceOf(JadxRuntimeException.class);
	}

	private void check(List<String> visitorNames, JadxPass pass, List<String> result) {
		check(visitorNames, singletonList(pass), result);
	}

	private void check(List<String> visitorNames, List<JadxPass> passes, List<String> result) {
		List<IDexTreeVisitor> visitors = ListUtils.map(visitorNames, PassMergeTest::mockVisitor);
		new PassMerge(visitors).merge(passes, p -> new DecompilePassWrapper((JadxDecompilePass) p));
		List<String> resultVisitors = ListUtils.map(visitors, IDexTreeVisitor::getName);
		assertThat(resultVisitors).isEqualTo(result);
	}

	private static IDexTreeVisitor mockVisitor(String name) {
		return new AbstractVisitor() {
			@Override
			public String getName() {
				return name;
			}
		};
	}

	private JadxPass mockPass(String name) {
		return mockPass(new SimpleJadxPassInfo(name));
	}

	private OrderedJadxPassInfo mockInfo(String name) {
		return new OrderedJadxPassInfo(name, name);
	}

	private JadxPass mockPass(JadxPassInfo info) {
		return new JadxDecompilePass() {
			@Override
			public void init(RootNode root) {
			}

			@Override
			public boolean visit(ClassNode cls) {
				return false;
			}

			@Override
			public void visit(MethodNode mth) {
			}

			@Override
			public JadxPassInfo getInfo() {
				return info;
			}

			@Override
			public String toString() {
				return info.getName();
			}
		};
	}
}
