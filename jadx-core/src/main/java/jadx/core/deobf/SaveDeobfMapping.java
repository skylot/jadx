package jadx.core.deobf;

import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JadxArgs;
import jadx.api.args.DeobfuscationMapFileMode;
import jadx.core.codegen.json.JsonMappingGen;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.utils.exceptions.JadxException;

public class SaveDeobfMapping extends AbstractVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(SaveDeobfMapping.class);

	@Override
	public void init(RootNode root) throws JadxException {
		JadxArgs args = root.getArgs();
		if (args.isDeobfuscationOn() || !args.isJsonOutput()) {
			saveMappings(root);
		}
		if (args.isJsonOutput()) {
			JsonMappingGen.dump(root);
		}
	}

	private void saveMappings(RootNode root) {
		DeobfuscationMapFileMode mode = root.getArgs().getDeobfuscationMapFileMode();
		if (!mode.shouldWrite()) {
			return;
		}
		DeobfPresets mapping = DeobfPresets.build(root);
		Path deobfMapFile = mapping.getDeobfMapFile();
		if (mode == DeobfuscationMapFileMode.READ_OR_SAVE && Files.exists(deobfMapFile)) {
			return;
		}
		try {
			mapping.clear();
			mapping.fill(root);
			mapping.save();
		} catch (Exception e) {
			LOG.error("Failed to save deobfuscation map file '{}'", deobfMapFile.toAbsolutePath(), e);
		}
	}
}
