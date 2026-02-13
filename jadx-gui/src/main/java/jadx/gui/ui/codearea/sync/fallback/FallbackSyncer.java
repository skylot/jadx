package jadx.gui.ui.codearea.sync.fallback;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.BadLocationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.gui.ui.codearea.AbstractCodeArea;
import jadx.gui.ui.codearea.CodeArea;
import jadx.gui.ui.codearea.CodePanel;
import jadx.gui.ui.codearea.SmaliArea;
import jadx.gui.ui.codearea.sync.CodeSyncHighlighter;

/**
 * Regex/String based sync strategy of toPanel when clicking in fromPanel
 * Summary of syncing strategy:
 * 1) Look for an identifying class member token under the caret position.
 * 2) If found look for the enclosing method or class declaration.
 * 3) If the line is a declaration line, find the equivalent line in the other code panel.
 * 4) Otherwise find the nth occurence of the token in the enclosing method/class in the other code
 * panel.
 * The following are not yet supported:
 * - generic classes/methods
 * - anonymous classes
 * - lambda functions
 * - constructors
 */
public class FallbackSyncer {
	private static final Logger LOG = LoggerFactory.getLogger(FallbackSyncer.class);

	public static boolean sync(CodePanel fromPanel, CodePanel toPanel) throws BadLocationException, Exception {
		LOG.debug("FALLBACK SYNC START");
		try {
			AbstractCodeArea from = fromPanel.getCodeArea();
			AbstractCodeArea to = toPanel.getCodeArea();

			int caretPos = from.getCaretPosition();
			int lineIndex = from.getLineOfOffset(caretPos);
			String[] fromLines = from.getText().split("\\R");
			if (lineIndex >= fromLines.length) {
				return false;
			}

			String caretLine = fromLines[lineIndex];
			LOG.debug("Caret line [{}]: {}", caretPos, caretLine);

			// Extract token under caret (string literal or identifier)
			AbstractCodeAreaToken areaToken = FallbackSyncer.getToken(from, caretPos);
			String token = areaToken.getStr();
			LOG.debug("Token at caret: '{}'", token);
			if (token == null || token.isEmpty()) {
				return false;
			}

			if (!allowSync(areaToken)) {
				LOG.debug("Fallback matching only applicable for variable, classname, field or method tokens");
				return false;
			}

			return syncToIdentifyingNthOccurence(areaToken, to);
		} finally {
			LOG.debug("FALLBACK SYNC END");
		}
	}

	// This function just serves as a way to create the correct Token type
	// FallbackSyncer should be refactored to use CodePanelSyncer
	private static AbstractCodeAreaToken getToken(AbstractCodeArea from, int caretPos) throws BadLocationException, FallbackSyncException {
		if (from instanceof SmaliArea) {
			return new SmaliAreaToken((SmaliArea) from, caretPos);
		}
		if (from instanceof CodeArea) {
			return new JavaCodeAreaToken((CodeArea) from, caretPos);
		}
		throw new FallbackSyncException("Unknown AbstractCodeArea type for " + from);
	}

	/**
	 * Looks for the nth occurence of the token in the enclosing class/method scope in the `to` area.
	 * If found, sync to it in the `to` area.
	 */
	private static boolean syncToIdentifyingNthOccurence(AbstractCodeAreaToken sourceToken, AbstractCodeArea to)
			throws BadLocationException, FallbackSyncException {
		AbstractCodeAreaLine tokenLine = sourceToken.getLine();

		// Locate the method/class declaration line for context
		IDeclaration fromDeclaration = tokenLine.getEnclosingScopeDeclaration();
		if (fromDeclaration == null) {
			LOG.warn("Unable to find declaration line above {}", tokenLine);
			return false;
		}
		AbstractCodeAreaLine fromDeclaringLine = fromDeclaration.getLine();

		AbstractCodeArea from = fromDeclaringLine.getArea();
		String declarationLineStr = fromDeclaringLine.getStr();
		LOG.debug("Found declaration line: {}", declarationLineStr);
		String nameToFind = fromDeclaration.getIdentifyingName();
		if (nameToFind == null || nameToFind.isEmpty()) {
			return false;
		}

		// Determine whether we're matching a class or method
		boolean isClass = fromDeclaringLine.isClassDeclaration();
		String regex = isClass
				? generateClassRegex(nameToFind)
				: generateMethodRegex(nameToFind);

		// Find the declaration in target text
		Matcher matcher = Pattern.compile(regex).matcher(to.getText());
		LOG.debug("Searching for {} in targetText, isClass {}", nameToFind, isClass);
		AbstractCodeAreaLine targetDeclLine = findTargetDeclaringLine(to, matcher, fromDeclaration);
		if (targetDeclLine == null) {
			LOG.debug("Cannot find target declaration line");
			return false;
		}
		int targetDeclarationLineIndex = targetDeclLine.getLineIndex();
		LOG.debug("Target declaration line {}", targetDeclLine.getStr());
		if (tokenLine.isScopeDeclarationLine()) {
			CodeSyncHighlighter.defaultHighlighter().highlightAndScrollToLine(to, targetDeclarationLineIndex);
			LOG.info("{} - Highlighted target declaration line", LOG.getName(), targetDeclLine.getStr());
			return true;
		}

		// Extract the method/class body from target
		String methodBody = extractMethodBody(to, matcher.start());

		// Find nth occurrence of token in source method
		// Extract method body from source (to count occurrences)
		Matcher fromMatcher = Pattern.compile(regex).matcher(from.getText());
		if (!fromMatcher.find()) {
			LOG.debug("No method/class match found in source for regex: {}", regex);
			return false;
		}
		String sourceMethodBody = extractMethodBody(from, fromMatcher.start());

		// Count which occurrence of token the caret corresponds to in the source method body
		String tokenStr = sourceToken.getStr();
		int caretPos = sourceToken.getAtPos();
		int caretOffsetInMethod = caretPos - fromMatcher.start();
		int nthOccurrence = 0;
		Pattern tokenPattern = Pattern.compile("\"" + Pattern.quote(tokenStr) + "\"|\\b" + Pattern.quote(tokenStr) + "\\b");
		Matcher tokenMatcher = tokenPattern.matcher(sourceMethodBody);

		while (tokenMatcher.find()) {
			if (tokenMatcher.start() > caretOffsetInMethod) {
				break;
			}
			nthOccurrence++;
		}

		LOG.debug("Caret is at occurrence number: {}", nthOccurrence);

		// Now find nth occurrence of token in target method body
		tokenMatcher = tokenPattern.matcher(methodBody);
		int occurrenceCount = 0;
		while (tokenMatcher.find()) {
			occurrenceCount++;
			if (occurrenceCount == nthOccurrence) {
				// Find absolute offset of this line in targetText
				int tokenPosInMethod = tokenMatcher.start();
				int absoluteOffset = matcher.start() + tokenPosInMethod;

				// Find line start and end offset in target
				int tokenLineIndex = to.getLineOfOffset(absoluteOffset);
				CodeSyncHighlighter.defaultHighlighter().highlightAndScrollToLine(to, tokenLineIndex);
				LOG.info("{} - Highlighted token '{}' at nth occurrence: {}", LOG.getName(), tokenStr, nthOccurrence);
				return true;
			}
		}

		LOG.debug("No matching token or instruction found in method: {}", nameToFind);
		return false;
	}

	private static AbstractCodeAreaLine findTargetDeclaringLine(
			AbstractCodeArea to, // target area
			Matcher matcher, // matcher to search for method/ctor name
			IDeclaration sourceDecl // source decl to match against
	) throws BadLocationException, FallbackSyncException {
		// Find the declaration in target text
		while (matcher.find()) {
			LOG.debug("Match found at offset: {}", matcher.start());
			int targetDeclarationLineIndex = to.getLineOfOffset(matcher.start());
			AbstractCodeAreaLine toDeclCandidate = getLine(to, targetDeclarationLineIndex);
			if (!toDeclCandidate.isScopeDeclarationLine()) {
				continue;
			}
			IDeclaration targetDecl = toDeclCandidate.getDeclaration();
			if (sourceDecl.equals(targetDecl)) {
				return toDeclCandidate;
			}
		}
		return null;
	}

	// Similar with the function above if refactored to use the CodePanelSyncer Abstraction we can
	// remove this.
	private static AbstractCodeAreaLine getLine(AbstractCodeArea area, int lineIndex) throws BadLocationException, FallbackSyncException {
		if (area instanceof SmaliArea) {
			return new SmaliAreaLine((SmaliArea) area, lineIndex);
		}
		if (area instanceof CodeArea) {
			return new JavaCodeAreaLine((CodeArea) area, lineIndex);
		}
		throw new FallbackSyncException("Unknown AbstractCodeArea type for " + area);
	}

	private static boolean allowSync(AbstractCodeAreaToken areaToken) throws BadLocationException {
		boolean isOnDeclarationLine = areaToken.getLine().isDeclarationLine();
		return isOnDeclarationLine
				|| areaToken.isClassField()
				|| areaToken.isFieldReference()
				|| areaToken.isMethodConstructorDeclarationOrCall();
	}

	private static String generateClassRegex(String name) {
		return "\\b(class|interface|enum)\\s+" + Pattern.quote(name) + "\\b" // java
				+ "|"
				+ "\\.class.*L.*" + Pattern.quote(name) + ";" // smali text
				+ "|"
				+ "Class:\\sL.*" + Pattern.quote(name) + ";"; // smali + dalvik
	}

	private static String generateMethodRegex(String name) {
		return "\\b" + Pattern.quote(name) + "\\s*\\(" // java like
				+ "|"
				+ "\\.method.*" + Pattern.quote(name) + "\\s*\\("; // smali
	}

	private static String extractMethodBody(AbstractCodeArea area, int startIndex) {
		String text = area.getText();
		if (area instanceof SmaliArea) {
			int end = text.indexOf(".end method", startIndex);
			return end != -1 ? text.substring(startIndex, end + ".end method".length()) : text.substring(startIndex);
		} else {
			int brace = 0;
			boolean inMethod = false;
			for (int i = startIndex; i < text.length(); i++) {
				char c = text.charAt(i);
				if (c == '{') {
					brace++;
					inMethod = true;
				} else if (c == '}') {
					brace--;
					if (brace == 0 && inMethod) {
						return text.substring(startIndex, i + 1);
					}
				}
			}
			return text.substring(startIndex);
		}
	}
}
