package jadx.gui.utils;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ICodeCache;
import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;
import jadx.api.JavaNode;
import jadx.api.metadata.ICodeMetadata;
import jadx.api.metadata.ICodeNodeRef;
import jadx.core.dex.nodes.ICodeNode;
import jadx.gui.JadxWrapper;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;

public final class JadxNodeWrapper {

	private static final Logger LOG = LoggerFactory.getLogger(JadxNodeWrapper.class);

	private final JadxWrapper wrapper;
	private final JadxDecompiler decompiler;
	private final JNodeCache nodeCache;
	private final ICodeCache codeCache;

	public JadxNodeWrapper(final JadxWrapper wrapper, final JNodeCache nodeCache) {
		this(wrapper, wrapper.getDecompiler(), nodeCache);
	}

	public JadxNodeWrapper(final JadxWrapper wrapper, final JadxDecompiler decompiler, final JNodeCache nodeCache) {
		this.wrapper = wrapper;
		this.decompiler = decompiler;
		this.nodeCache = nodeCache;
		this.codeCache = wrapper.getArgs().getCodeCache();
	}

	public final JadxWrapper getNodeWrapper() {
		return this.wrapper;
	}

	public final JadxDecompiler getDecompiler() {
		return this.decompiler;
	}

	public final JNodeCache getNodeCache() {
		return this.nodeCache;
	}

	public final JNode convertToJNode(JavaNode node) {
		return nodeCache.makeFrom(node);
	}

	public final JClass convertToJNode(JavaClass cls) {
		return nodeCache.makeFrom(cls);
	}

	public final JNode convertToJNode(ICodeNode codeNode) {
		JavaNode node = Objects.requireNonNull(decompiler.getJavaNodeByRef(codeNode));
		return Objects.requireNonNull(nodeCache.makeFrom(node));
	}

	public final String getClassCode(JavaClass javaClass) {
		try {
			// quick check for if code already in cache
			final String code = codeCache.getCode(javaClass.getRawName());
			if (code != null) {
				return code;
			}
			// start decompilation
			return javaClass.getCode();
		} catch (Exception e) {
			LOG.warn("Failed to get class code: {}", javaClass, e);
			return "";
		}
	}

	public final @Nullable JNode getEnclosingNode(JavaClass javaCls, int pos) {
		try {
			final ICodeMetadata metadata = javaCls.getCodeInfo().getCodeMetadata();
			final ICodeNodeRef nodeRef = metadata.getNodeAt(pos);
			final JavaNode encNode = wrapper.getJavaNodeByRef(nodeRef);
			if (encNode != null) {
				return convertToJNode(encNode);
			}
		} catch (Exception e) {
			LOG.debug("Failed to resolve enclosing node", e);
		}
		return null;
	}

	public final JClass makeFromRefNode(JavaClass cls) {
		return nodeCache.makeFrom(cls);
	}

	public final JNode makeFromRefNode(JavaNode node) {
		return nodeCache.makeFrom(node);
	}

	public final JNode makeFromRefNode(ICodeNodeRef codeNode) {
		return nodeCache.makeFrom(codeNode);
	}
}
