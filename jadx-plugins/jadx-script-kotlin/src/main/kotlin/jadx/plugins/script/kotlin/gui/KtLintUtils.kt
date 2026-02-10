package jadx.plugins.script.kotlin.gui

import com.pinterest.ktlint.rule.engine.api.Code
import com.pinterest.ktlint.rule.engine.api.EditorConfigOverride
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CodeStyleValue
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.ruleset.standard.StandardRuleSetProvider
import org.ec4j.core.model.PropertyType

data class JadxLintError(
	val line: Int,
	val col: Int,
	val ruleId: String,
	val detail: String,
)

object KtLintUtils {
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
		return ktLint.format(
			code,
			rerunAfterAutocorrect = true,
			defaultAutocorrect = true,
		) { AutocorrectDecision.ALLOW_AUTOCORRECT }
	}

	fun lint(content: String): List<JadxLintError> {
		val errors = mutableListOf<JadxLintError>()
		val code = Code.fromSnippet(content, script = true)
		ktLint.lint(code) { lintError ->
			errors.add(
				JadxLintError(
					line = lintError.line,
					col = lintError.col,
					ruleId = lintError.ruleId.value,
					detail = lintError.detail,
				),
			)
		}
		return errors
	}
}
