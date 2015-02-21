package jadx.core.dex.visitors;

import jadx.core.deobf.Deobfuscator;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.exceptions.JadxException;

import java.io.File;

import org.apache.commons.io.FilenameUtils;

public class RenameVisitor extends AbstractVisitor {

	private Deobfuscator deobfuscator;

	@Override
	public void init(RootNode root) {
		String firstInputFileName = root.getDexNodes().get(0).getInputFile().getFile().getAbsolutePath();
		String inputPath = FilenameUtils.getFullPathNoEndSeparator(firstInputFileName);
		String inputName = FilenameUtils.getBaseName(firstInputFileName);

		File deobfMapFile = new File(inputPath, inputName + ".jobf");
		deobfuscator = new Deobfuscator(root.getArgs(), root.getDexNodes(), deobfMapFile);
		// TODO: check classes for case sensitive names (issue #24)
		// TODO: sometimes can be used source file name from 'SourceFileAttr'
		deobfuscator.execute();
	}

	@Override
	public boolean visit(ClassNode cls) throws JadxException {
		// TODO: rename fields and methods
		return false;
	}
}
