package jadx.core.xmlgen;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.mockito.internal.util.io.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ResourceFile;
import jadx.api.ResourceType;
import jadx.core.codegen.CodeWriter;
import jadx.core.utils.files.ZipSecurity;

import static jadx.core.utils.files.FileUtils.prepareFile;

public class ResourcesSaver implements Runnable {
	private static final Logger LOG = LoggerFactory.getLogger(ResourcesSaver.class);

	private final ResourceFile resourceFile;
	private File outDir;

	public ResourcesSaver(File outDir, ResourceFile resourceFile) {
		this.resourceFile = resourceFile;
		this.outDir = outDir;
	}

	@Override
	public void run() {
		ResContainer rc = resourceFile.loadContent();
		if (rc != null) {
			saveResources(rc);
		}
	}

	private void saveResources(ResContainer rc) {
		if (rc == null) {
			return;
		}
		List<ResContainer> subFiles = rc.getSubFiles();
		if (subFiles.isEmpty()) {
			save(rc, outDir);
		} else {
			saveToFile(rc, new File(outDir, "res/values/public.xml"));
			for (ResContainer subFile : subFiles) {
				saveResources(subFile);
			}
		}
	}

	private void save(ResContainer rc, File outDir) {
		File outFile = new File(outDir, rc.getFileName());
		BufferedImage image = rc.getImage();
		if (image != null) {
			String ext = FilenameUtils.getExtension(outFile.getName());
			try {
				outFile = prepareFile(outFile);

				if (!ZipSecurity.isInSubDirectory(outDir, outFile)) {
					LOG.error("Path traversal attack detected, invalid resource name: {}",
							outFile.getPath());
					return;
				}

				ImageIO.write(image, ext, outFile);
			} catch (IOException e) {
				LOG.error("Failed to save image: {}", rc.getName(), e);
			}
			return;
		}

		if (!ZipSecurity.isInSubDirectory(outDir, outFile)) {
			LOG.error("Path traversal attack detected, invalid resource name: {}",
					rc.getFileName());
			return;
		}
		saveToFile(rc, outFile);
	}

	private void saveToFile(ResContainer rc, File outFile) {
		CodeWriter cw = rc.getContent();
		if (cw != null) {
			cw.save(outFile);
			return;
		}
		InputStream binary = rc.getBinary();
		if(binary != null) {
			try {
				outFile.getParentFile().mkdirs();
				FileOutputStream binaryFileStream = new FileOutputStream(outFile);
				IOUtils.copy(binary, binaryFileStream);
				binaryFileStream.close();
				binary.close();
			}
			catch(IOException e) {
				LOG.warn("Resource '{}' not saved, got exception {}", rc.getName(), e);
			}
			return;
		}
		LOG.warn("Resource '{}' not saved, unknown type", rc.getName());
	}
}
