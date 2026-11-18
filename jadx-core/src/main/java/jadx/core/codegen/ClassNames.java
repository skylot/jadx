package jadx.core.codegen;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jadx.api.impl.SimpleCodeWriter;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.visitors.rename.RenameVisitor;
import jadx.core.utils.exceptions.JadxRuntimeException;

/**
 * Names that can start a type reference in one generated compilation unit.
 * Collect with the renderer, after transformations, rather than using semantic dependencies
 * (which exclude external classes and several kinds of generated references).
 */
public final class ClassNames {

	private final Set<String> reserved = new HashSet<>();
	private final Map<ClassNode, Set<String>> fieldReserved = new HashMap<>();
	private final Set<ClassNode> fieldOwners = new HashSet<>();

	public static void collect(ClassNode cls) {
		ClassNames names = new ClassNames();
		for (int attempt = 0; attempt < 2; attempt++) {
			ClassGen gen = new ClassGen(cls, cls.root().getArgs());
			gen.setCollectNames(names);
			try {
				SimpleCodeWriter code = new SimpleCodeWriter(cls.root().getArgs());
				if (cls.isAnonymous()) {
					gen.addClassBody(code);
				} else {
					gen.addClassCode(code);
				}
				break;
			} catch (Exception e) {
				if (attempt == 0 && cls.contains(AFlag.RESTART_CODEGEN)) {
					cls.remove(AFlag.RESTART_CODEGEN);
				} else {
					throw new JadxRuntimeException("Failed to collect codegen names", e);
				}
			}
		}
		if (cls.root().getArgs().isRenameValid()) {
			names.renameFields(cls);
		}
		cls.root().getCacheStorage().putClassNames(cls, names);
	}

	private void renameFields(ClassNode cls) {
		RenameVisitor.checkFieldNames(cls, fieldReserved.getOrDefault(cls, Collections.emptySet()));
		for (ClassNode inner : cls.getInnerClasses()) {
			renameFields(inner);
		}
	}

	public void add(ClassNode scope, ClassInfo cls) {
		// Reserve both qualified and short forms. Import selection must not feed back into naming.
		String fullName = cls.getAliasFullName();
		int dot = fullName.indexOf('.');
		String root = dot == -1 ? fullName : fullName.substring(0, dot);
		reserved.add(root);
		if (dot != -1) {
			addQualifier(scope, root);
		}
		for (ClassInfo current = cls; current != null; current = current.getParentClass()) {
			reserved.add(current.getAliasShortName());
		}
	}

	public void addQualifier(ClassNode scope, String name) {
		int dot = name.indexOf('.');
		String qualifier = dot == -1 ? name : name.substring(0, dot);
		fieldReserved.computeIfAbsent(scope, k -> new HashSet<>()).add(qualifier);
		scope.visitParentClasses(parent -> fieldReserved.computeIfAbsent(parent, k -> new HashSet<>()).add(qualifier));
	}

	public void addField(FieldNode field) {
		if (field != null) {
			fieldOwners.add(field.getParentClass().getTopParentClass());
		}
	}

	public Set<String> getReserved() {
		return Collections.unmodifiableSet(reserved);
	}

	public Set<ClassNode> getFieldOwners() {
		return Collections.unmodifiableSet(fieldOwners);
	}
}
