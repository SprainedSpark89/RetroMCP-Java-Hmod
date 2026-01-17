package org.mcphackers.mcp.tasks;

import static org.mcphackers.mcp.MCPPaths.*;

import java.io.BufferedWriter;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.json.JSONObject;
import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.tasks.mode.TaskMode;
import org.mcphackers.mcp.tasks.mode.TaskParameter;
import org.mcphackers.mcp.tools.FileUtil;
import org.mcphackers.mcp.tools.Util;
import org.mcphackers.mcp.tools.versions.DownloadData;
import org.mcphackers.mcp.tools.versions.VersionParser;
import org.mcphackers.mcp.tools.versions.VersionParser.VersionData;
import org.mcphackers.mcp.tools.versions.json.Version;

public class TaskSetupHmod extends TaskStaged {

	private long libsSize = 0;

	public TaskSetupHmod(MCP instance) {
		super(Side.HMOD, instance);
	}

	@Override
	protected Stage[] setStages() {
		return new Stage[]{
				stage(getLocalizedStage("setupH"), 0, () -> {
					new TaskCleanup(mcp).cleanup(); // normal server setup
					FileUtil.createDirectories(MCPPaths.get(mcp, JARS));
					FileUtil.createDirectories(MCPPaths.get(mcp, LIB));
					FileUtil.createDirectories(MCPPaths.get(mcp, NATIVES));

					setProgress(getLocalizedStage("setupH"), 1);
					VersionParser versionParser = mcp.getVersionParser();
					String chosenVersion = mcp.getOptions().getStringParameter(TaskParameter.SETUP_HMOD);
					VersionData chosenVersionData;

					// Keep asking until chosenVersion equals one of the versionData
					while (true) {
						chosenVersionData = versionParser.getVersion(chosenVersion);
						if (chosenVersionData != null) {
							break;
						}
						chosenVersion = mcp.selectCompatibleVersion(TaskMode.SETUPH.getFullName(), "Select a Server Version:");
					}
					
					

					InputStream versionStream;
					try {
						versionStream = new URL(chosenVersionData.url).openStream();
					} catch (MalformedURLException ex) {
						versionStream = Files.newInputStream(MCPPaths.get(mcp, chosenVersionData.url));
					}
					JSONObject versionJsonObj = new JSONObject(new String(Util.readAllBytes(versionStream), StandardCharsets.UTF_8));
					Version versionJson = Version.from(versionJsonObj);
					FileUtil.createDirectories(MCPPaths.get(mcp, CONF));

					if (chosenVersionData.resources != null) {
						setProgress(getLocalizedStage("download", chosenVersionData.resources), 2);
						try {
							URL url = new URL(chosenVersionData.resources);
							FileUtil.extract(url.openStream(), MCPPaths.get(mcp, CONF));
						} catch (MalformedURLException e) {
							Path p = Paths.get(chosenVersionData.resources);
							if (Files.exists(p)) {
								FileUtil.extract(p, MCPPaths.get(mcp, CONF));
							}
						}
					}

					DownloadData dlData = new DownloadData(mcp, versionJson);
					dlData.performDownload((dl, totalSize) -> {
						libsSize += dl.downloadSize();
						int percent = (int) ((double) libsSize / totalSize * 97D);
						setProgress(getLocalizedStage("download", dl.downloadURL()), 3 + percent);
					});
					Path natives = MCPPaths.get(mcp, NATIVES);
					for (Path nativeArchive : DownloadData.getNatives(MCPPaths.get(mcp, LIB), versionJson)) {
						FileUtil.extract(nativeArchive, natives);
					}
					try (BufferedWriter writer = Files.newBufferedWriter(MCPPaths.get(mcp, VERSION))) {
						versionJsonObj.write(writer, 1, 0);
					}
					mcp.setCurrentVersion(versionJson);
					
					
					// Hmod

					setProgress(getLocalizedStage("setupH"), 1);
					versionParser = mcp.getVersionParser();
					chosenVersion = mcp.getOptions().getStringParameter(TaskParameter.SETUP_HMOD);

					// Keep asking until chosenVersion equals one of the versionData
					while (true) {
						chosenVersionData = versionParser.getHmodVersion(chosenVersion);
						if (chosenVersionData != null) {
							break;
						}
						chosenVersion = mcp.selectCompatibleVersion(TaskMode.SETUPH.getFullName(), "Select a Server Version:");
					}
					
					

					
					try {
						versionStream = new URL(chosenVersionData.url).openStream();
					} catch (MalformedURLException ex) {
						versionStream = Files.newInputStream(MCPPaths.get(mcp, chosenVersionData.url));
					}
					versionJsonObj = new JSONObject(new String(Util.readAllBytes(versionStream), StandardCharsets.UTF_8));
					versionJson = Version.from(versionJsonObj);
					FileUtil.createDirectories(MCPPaths.get(mcp, CONF));

					if (chosenVersionData.resources != null) {
						setProgress(getLocalizedStage("download", chosenVersionData.resources), 2);
						try {
							URL url = new URL(chosenVersionData.resources);
							FileUtil.extract(url.openStream(), MCPPaths.get(mcp, CONF));
						} catch (MalformedURLException e) {
							Path p = Paths.get(chosenVersionData.resources);
							if (Files.exists(p)) {
								FileUtil.extract(p, MCPPaths.get(mcp, CONF));
							}
						}
					}

					dlData = new DownloadData(mcp, versionJson);
					dlData.performDownload((dl, totalSize) -> {
						libsSize += dl.downloadSize();
						int percent = (int) ((double) libsSize / totalSize * 97D);
						setProgress(getLocalizedStage("download", dl.downloadURL()), 3 + percent);
					});
					
					
					for (Path nativeArchive : DownloadData.getNatives(MCPPaths.get(mcp, LIB), versionJson)) {
						FileUtil.extract(nativeArchive, natives);
					}
					try (BufferedWriter writer = Files.newBufferedWriter(MCPPaths.get(mcp, VERSION))) {
						versionJsonObj.write(writer, 1, 0);
					}
					mcp.setCurrentHMODVersion(versionJson);
				})
		};
	}
}
