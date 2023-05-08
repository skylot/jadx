package jadx.core.dex.visitors.kotlin

import jadx.core.dex.nodes.ClassNode
import jadx.core.dex.nodes.RootNode
import jadx.core.dex.visitors.AbstractVisitor
import jadx.core.dex.visitors.InitCodeVariables
import jadx.core.dex.visitors.JadxVisitor
import jadx.core.dex.visitors.ProcessInstructionsVisitor
import jadx.core.dex.visitors.debuginfo.DebugInfoApplyVisitor
import jadx.core.dex.visitors.rename.CodeRenameVisitor
import jadx.core.dex.visitors.rename.KotlinMetadataRename

@JadxVisitor(
	name = "KotlinMetadataVisitor",
	desc = "Use kotlin.Metadata annotation to rename method args & fields",
	runAfter = [
		InitCodeVariables::class,
		DebugInfoApplyVisitor::class,
		ProcessKotlinInternals::class,
		ProcessInstructionsVisitor::class,
	],
	runBefore = [
		CodeRenameVisitor::class,
	]
)
class KotlinMetadataVisitor : AbstractVisitor() {
	private var isParseMetadata: Boolean = false

	override fun init(root: RootNode) {
		isParseMetadata = root.args.isParseKotlinMetadata
	}

	override fun visit(cls: ClassNode): Boolean {
		if (isParseMetadata) {
			cls.innerClasses.forEach(::visit)

			KotlinMetadataRename.process(cls)
		}


		return false
	}
}
