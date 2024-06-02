package jadx.core.deobf;

import jadx.api.JadxArgs;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.utils.exceptions.JadxException;

public class InitRenameProviders extends AbstractVisitor {

	@Override
	public void init(RootNode root) throws JadxException {
		JadxArgs args = root.getArgs();
		if (args.isDeobfuscationOn() || !args.getRenameFlags().isEmpty()) {
			args.getAliasProvider().init(root);
		}
		if (args.isDeobfuscationOn()) {
			args.getRenameCondition().init(root);
		}
	}
}
