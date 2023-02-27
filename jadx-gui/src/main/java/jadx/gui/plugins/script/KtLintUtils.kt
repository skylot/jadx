package jadx.gui.plugins.script

import com.pinterest.ktlint.core.Code
import com.pinterest.ktlint.core.KtLintRuleEngine
import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.api.EditorConfigOverride
import com.pinterest.ktlint.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.ruleset.standard.StandardRuleSetProvider
import org.ec4j.core.model.PropertyType
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object KtLintUtils {

	val LOG: Logger = LoggerFactory.getLogger(KtLintUtils::class.java)

	val ktLint by lazy {
		KtLintRuleEngine(
			ruleProviders = StandardRuleSetProvider().getRuleProviders(),
			editorConfigOverride = EditorConfigOverride.from(
				INDENT_STYLE_PROPERTY to PropertyType.IndentStyleValue.tab,
			),
		)
	}

	fun format(content: String, fileName: String): String {
		val code = Code.CodeSnippet(content, script = true)
		return ktLint.format(code) { lintError, corrected ->
			if (!corrected) {
				LOG.warn("Format error: {}", lintError)
			}
		}
	}

	fun lint(content: String, fileName: String): List<LintError> {
		val code = Code.CodeSnippet(content, script = true)
		val errors = mutableListOf<LintError>()
		ktLint.lint(code, errors::add)
		return errors
	}
}
