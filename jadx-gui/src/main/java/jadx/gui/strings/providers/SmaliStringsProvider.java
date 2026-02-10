package jadx.gui.strings.providers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JavaClass;
import jadx.api.utils.CodeUtils;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.utils.Utils;
import jadx.gui.jobs.Cancelable;
import jadx.gui.strings.SingleStringResult;
import jadx.gui.strings.StringResult;
import jadx.gui.strings.caching.IStringsInfoCache;
import jadx.gui.treemodel.CodeNode;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.utils.JadxNodeWrapper;

public class SmaliStringsProvider implements IStringsProvider {
	private static final Logger LOG = LoggerFactory.getLogger(SmaliStringsProvider.class);

	private final JadxNodeWrapper wrapper;
	private final Set<String> classStringCache;
	private final JavaClassStringReader stringReader;
	private final IStringsInfoCache stringsCache;
	private final LinkedList<SingleStringResult> pendingResultsForClass;

	private JavaClass currentClass;
	private Iterator<SingleStringResult> resultIterator;

	public SmaliStringsProvider(JadxNodeWrapper wrapper, JavaClassStringReader stringReader, final IStringsInfoCache stringsCache) {
		this.classStringCache = new HashSet<>();
		this.stringReader = stringReader;
		this.wrapper = wrapper;
		this.stringsCache = stringsCache;
		this.pendingResultsForClass = new LinkedList<>();
	}

	@Override
	public @Nullable StringResult next(final Supplier<Optional<JavaClass>> clsSupplier, final Cancelable cancelable) {
		if (this.resultIterator != null) {
			if (this.resultIterator.hasNext()) {
				final SingleStringResult next = this.resultIterator.next();
				pendingResultsForClass.add(next);
				return next;
			}
			this.resultIterator = null;
		}

		SingleStringResult nextResult = null;
		while (!cancelable.isCanceled()) {
			if (stringReader == null || !stringReader.hasNext()) {
				cacheCurrentClassResults();

				final Optional<JavaClass> clsOptional = clsSupplier.get();
				if (clsOptional.isEmpty()) {
					break;
				}

				currentClass = clsOptional.get();

				classStringCache.clear();
				final String smali = currentClass.getSmali();
				if (smali == null) {
					continue;
				}
				this.stringReader.registerSmali(smali);
				if (!this.stringReader.hasNext()) {
					continue;
				}
			}

			final String next = stringReader.next();
			if (classStringCache.contains(next)) {
				// We've already searched this class for the string, so continue
				continue;
			}
			if (next.isBlank()) {
				// Ignore whitespace strings
				continue;
			}
			classStringCache.add(next);
			this.resultIterator = getResultsFromMatch(next);
			// The iterator generated below will always have at least one element, so it's safe to just call
			// next.
			nextResult = this.resultIterator.next();
			break;
		}

		if (nextResult != null) {
			pendingResultsForClass.add(nextResult);
		} else {
			cacheCurrentClassResults();
		}
		return nextResult;
	}

	/**
	 * This function will create a result, converting between the Smali string that was identified to
	 * the JNode which is used for code navigation.
	 *
	 * @param matchedSmaliString The Smali const-string.
	 * @return The string search result representing the specified Smali string.
	 */
	public Iterator<SingleStringResult> getResultsFromMatch(final String matchedSmaliString) {
		final List<SingleStringResult> list = new LinkedList<>();

		int searchPositionPointer = 0;

		final String clsCode = wrapper.getClassCode(currentClass);
		while (true) {
			final String searchString = "\"" + matchedSmaliString + "\"";
			final int newPos = clsCode.indexOf(searchString, searchPositionPointer);
			if (newPos == -1) {
				break;
			}
			final int lineStart = 1 + CodeUtils.getNewLinePosBefore(clsCode, newPos);
			final int lineEnd = CodeUtils.getNewLinePosAfter(clsCode, newPos);
			final int end = lineEnd == -1 ? clsCode.length() : lineEnd;
			final String line = clsCode.substring(lineStart, end);
			searchPositionPointer = end;
			final JClass rootCls = wrapper.convertToJNode(currentClass);
			final JNode enclosingNode = Utils.getOrElse(wrapper.getEnclosingNode(currentClass, end), rootCls);
			list.add(new SingleStringResult(matchedSmaliString, this.currentClass,
					new CodeNode(rootCls, enclosingNode, line.trim(), newPos)));
		}

		if (list.isEmpty()) {
			list.add(new SingleStringResult(matchedSmaliString, this.currentClass, wrapper.makeFromRefNode(currentClass)));
		}

		return list.iterator();
	}

	private void cacheCurrentClassResults() {
		if (currentClass != null) {
			final ClassNode clsNode = currentClass.getClassNode();
			stringsCache.addResults(clsNode, new ArrayList<>(pendingResultsForClass));
			pendingResultsForClass.clear();
		}
	}
}
