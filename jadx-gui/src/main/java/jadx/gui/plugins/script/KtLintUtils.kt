package jadx.gui.plugins.script

import com.pinterest.ktlint.rule.engine.api.Code
import com.pinterest.ktlint.rule.engine.api.EditorConfigOverride
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CodeStyleValue
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.ruleset.standard.StandardRuleSetProvider
import org.ec4j.core.model.PropertyType
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object KtLintUtils {

	val LOG: Logger = LoggerFactory.getLogger(KtLintUtils::class.java)

	private val ktLint by lazy {
		KtLintRuleEngine(
			ruleProviders = StandardRuleSetProvider().getRuleProviders(),
			editorConfigOverride = EditorConfigOverride.from(
				CODE_STYLE_PROPERTY to CodeStyleValue.intellij_idea,
				INDENT_STYLE_PROPERTY to PropertyType.IndentStyleValue.tab,
			),
		)
	}

	fun format(content: String): String {
		val code = Code.fromSnippet(content, script = true)
		return ktLint.format(code) { lintError, corrected ->
			if (!corrected) {
				LOG.warn("Format error: {}", lintError)
			}
		}
	}

	fun lint(content: String): List<JadxLintError> {
		val code = Code.fromSnippet(content, script = true)
		val errors = mutableListOf<JadxLintError>()
		ktLint.lint(code) { lintError ->
			errors.add(JadxLintError(lintError.line, lintError.col, lintError.ruleId.value, lintError.detail))
		}
		return errors
	}
}

data class JadxLintError(
	val line: Int,
	val col: Int,
	val ruleId: String,
	val detail: String,
)
