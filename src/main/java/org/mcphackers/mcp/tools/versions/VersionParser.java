package org.mcphackers.mcp.tools.versions;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mcphackers.mcp.tools.JSONUtil;
import org.mcphackers.mcp.tools.versions.json.VersionMetadata;

public class VersionParser {

	public static final String DEFAULT_JSON = "https://raw.githubusercontent.com/SprainedSpark89/MCPHackers.github.io/main/versionsV3/versions.json";
	public static String mappingsJson = DEFAULT_JSON;

	private final Map<String, VersionData> versions = new HashMap<>();
	private final Map<String, VersionData> hmodVersions = new HashMap<>();
	public Exception failureCause;

	public VersionParser() {
		this.addVersionsFromURL(mappingsJson);
		this.addHmodFromURL("https://raw.githubusercontent.com/SprainedSpark89/MCPHackers.github.io/main/versionsHmod/versions.json");
	}

	public void addVersionsFromURL(String url) {
		JSONArray json;
		try {
			json = getJson(url);
		} catch (Exception e) {
			failureCause = e;
			return; // Couldn't init json
		}
		for (Object j : json) {
			if (!(j instanceof JSONObject)) {
				continue;
			}
			try {
				VersionData data = VersionData.from((JSONObject) j);
				versions.put(data.id, data);
			} catch (Exception e) {
				// Catching exception will skip to the next version
				e.printStackTrace();
			}
		}
	}
	
	public void addHmodFromURL(String url) {
		JSONArray json;
		try {
			json = getJson(url);
		} catch (Exception e) {
			failureCause = e;
			return; // Couldn't init json
		}
		for (Object j : json) {
			if (!(j instanceof JSONObject)) {
				continue;
			}
			try {
				VersionData data = VersionData.from((JSONObject) j);
				hmodVersions.put(data.id, data);
			} catch (Exception e) {
				// Catching exception will skip to the next version
				e.printStackTrace();
			}
		}
	}

	private static JSONArray getJson(String url) throws Exception {
		InputStream in;
		Path versions = Paths.get("versions.json");
		if (Files.exists(versions)) {
			in = Files.newInputStream(versions);
		} else {
			URLConnection connect = new URL(url).openConnection();
			connect.setConnectTimeout(30000);
			in = connect.getInputStream();
		}
		return JSONUtil.parseJSONArray(in);
	}

	/**
	 * Returns version data from version id/name
	 *
	 * @param id
	 * @return VersionData
	 */
	public VersionData getVersion(String id) {
		return versions.getOrDefault(id, null);
	}
	
	public VersionData getHmodVersion(String id) {
		return hmodVersions.getOrDefault(id, null);
	}

	/**
	 * @return All sorted VersionData
	 */
	public Collection<VersionData> getUnsortedVersions() {
		return versions.values();
	}

	/**
	 * @return All sorted VersionData
	 */
	public List<VersionData> getSortedVersions() {
		List<VersionData> sortedVersions = new ArrayList<>(versions.values());
		sortedVersions.sort(new VersionSorter());
		return sortedVersions;
	}
	
	public List<VersionData> getSortedHmodVersions() {
		List<VersionData> sortedVersions = new ArrayList<>(hmodVersions.values());
		sortedVersions.sort(new VersionSorter());
		return sortedVersions;
	}

	public static class VersionData extends VersionMetadata {
		public String resources;

		public static VersionData from(JSONObject obj) {
			if (obj == null) {
				return null;
			}
			return new VersionData() {
				{
					id = obj.optString("id", null);
			        time = obj.optString("time", null);
			        releaseTime = obj.optString("releaseTime", null);
			        type = obj.optString("type", null);
			        url = obj.optString("url", null);

			        // New optional fields
			        range = obj.optString("range", null);
			        serverVersions = readStringArray(obj, "serverVersions");
			        gameVersions = readStringArray(obj, "gameVersions");

				}
			};
		}

		@Override
		public String toString() {
			String typ;
			String ver;
			if (id.startsWith("rd") && "old_alpha".equals(type)) {
				typ = "Pre-Classic";
				ver = id;
			} else if (id.startsWith("c") && "old_alpha".equals(type)) {
				typ = "Classic";
				ver = id.substring(1);
			} else if (id.startsWith("inf-")) {
				typ = "Infdev";
				ver = id.substring(4);
			} else if (id.startsWith("in-")) {
				typ = "Indev";
				ver = id.substring(3);
			} else if (id.startsWith("a") && "old_alpha".equals(type)) {
				typ = "Alpha";
				ver = id.substring(1);
			} else if (id.startsWith("b")) {
				typ = "Beta";
				ver = id.substring(1);
			} else {
				typ = type.substring(0, 1).toUpperCase() + type.substring(1);
				ver = id;
			}
			return typ + " " + ver;
		}
	}

	/**
	 * Sorts versions by date
	 */
	public static class VersionSorter implements Comparator<VersionData> {

		@Override
		public int compare(VersionData t1, VersionData t2) {
			try {
				Instant i1 = Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(t1.releaseTime));
				Instant i2 = Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(t2.releaseTime));
				return i2.compareTo(i1);
			} catch (Exception e) {
				return -1;
			}
		}
	}

}
