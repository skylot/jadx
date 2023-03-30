package jadx.api.impl.passes;

import jadx.api.plugins.pass.JadxPass;
import jadx.core.dex.visitors.IDexTreeVisitor;

public interface IPassWrapperVisitor extends IDexTreeVisitor {

	JadxPass getPass();
}
