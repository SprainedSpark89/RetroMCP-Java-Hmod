/*package org.mcphackers.mcp.tools.versions.json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.tools.versions.VersionParser.VersionData;

public class HmodVersion {
	public static HashMap<String, String> hmodToServer = new HashMap<String, String>();
	public static HashMap<String, String> serverToClient = new HashMap<String, String>();
	
	static {
		for(int i = 27; i < 64; i++) {
			hmodToServer.put("hmod"+i, "0.1.4");
		}
		serverToClient.put("0.1.4", "a1.0.17-a1.0.17_04");
		
		for(int i = 64; i < 123; i++) {
			hmodToServer.put("hmod"+i, "0.2.0_1-0.2.1");
		}
		
		serverToClient.put("0.2.0_1-0.2.1", "a1.1.0-a1.1.2_01");
		
		hmodToServer.put("hmod123", "0.2.2_01-0.2.3");
		hmodToServer.put("hmod124", "0.2.2_01-0.2.3");
		
		serverToClient.put("0.2.2_01-0.2.3", "a1.2.0-a1.2.1_01");
		
		hmodToServer.put("hmod125", "0.2.4");
		serverToClient.put("0.2.4", "a1.2.2");
		
		hmodToServer.put("hmod126", "0.2.5-0.2.5_02");
		hmodToServer.put("hmod127", "0.2.5-0.2.5_02");
		hmodToServer.put("hmod128", "0.2.5-0.2.5_02");
		
		serverToClient.put("0.2.5-0.2.5_02", "a1.2.3-a1.2.3_04");
		
		hmodToServer.put("hmod129", "0.2.6_02");
		hmodToServer.put("hmod130", "0.2.6_02");
		serverToClient.put("0.2.6_02", "a1.2.4_01-a1.2.6");
		
		hmodToServer.put("hmod131", "0.2.7-0.2.8");
		serverToClient.put("0.2.7-0.2.8", "a1.2.4_01-a1.2.6");
		
		hmodToServer.put("hmod132", "b1.1_02");
		
		hmodToServer.put("hmod133", "b1.1_02");
	}
	
	public static String[] getCompatibilityList(String hmodVersion) {
		// Accept "hmod 27" or "hmod27" (space optional)
		if(hmodVersion == null || hmodVersion.trim().isEmpty()) return new String[0];
		String idPart = hmodVersion.trim();
		if(idPart.contains(" ")) {
			String[] sp = idPart.split(" ");
			if(sp.length > 1) idPart = "hmod" + sp[1];
		}
		// If user passed just the number or already 'hmodNN', normalize
		if(!idPart.startsWith("hmod")) {
			if(idPart.matches("\\d+")) {
				idPart = "hmod" + idPart;
			}
		}
		String serverVers = hmodToServer.get(idPart);
		if(serverVers == null) return new String[0];
		String clientVers = serverToClient.get(serverVers);
		if(clientVers == null) clientVers = serverVers; // fallback
		
		List<VersionData> versions = MCP.getVersionParser().getSortedVersions();
		List<String> clientVersions = new ArrayList<String>();
		
		// clientVers can be a single token or a range like "a1.2.0-a1.2.1_01" or multiple comma-separated ranges
		String[] ranges = clientVers.split(",");
		for(VersionData version : versions) {
			String vid = version.id;
			for(String range : ranges) {
				range = range.trim();
				if(range.isEmpty()) continue;
				if(range.equals(vid)) { // exact match
					clientVersions.add(vid);
					break;
				}
				if(range.contains("-")) {
					String[] bounds = range.split("-", 2);
					String min = bounds[0].trim();
					String max = bounds[1].trim();
					if(isVersionInRangeInclusive(vid, min, max)) {
						clientVersions.add(vid);
						break;
					}
				} else {
					// single token, but not exact match: allow matching if token is prefix of vid (e.g., "a1.2" should match a1.2.x)
					if(vid.startsWith(range)) {
						clientVersions.add(vid);
						break;
					}
				}
			}
		}
		return clientVersions.toArray(new String[0]);
	}
	
	private static boolean isVersionInRangeInclusive(String target, String min, String max) {
		ParsedVersion t = ParsedVersion.parse(target);
		ParsedVersion a = ParsedVersion.parse(min);
		ParsedVersion b = ParsedVersion.parse(max);
		if(t == null || a == null || b == null) return false;
		return compareParsed(t, a) >= 0 && compareParsed(t, b) <= 0;
	}
	
	private static int compareParsed(ParsedVersion x, ParsedVersion y) {
		// compare prefix letter first (if present)
		if(x.prefix != y.prefix) return Character.compare(x.prefix, y.prefix);
		if(x.major != y.major) return Integer.compare(x.major, y.major);
		if(x.minor != y.minor) return Integer.compare(x.minor, y.minor);
		if(x.patch != y.patch) return Integer.compare(x.patch, y.patch);
		return Integer.compare(x.patchRev, y.patchRev);
	}
	
	private static class ParsedVersion {
		char prefix; // e.g., 'a' or 'b' or 0 if none
		int major;
		int minor;
		int patch;
		int patchRev; // number after underscore
		
		static ParsedVersion parse(String s) {
			if(s == null) return null;
			s = s.trim();
			if(s.isEmpty()) return null;
			ParsedVersion pv = new ParsedVersion();
			int idx = 0;
			char first = s.charAt(0);
			if(Character.isLetter(first)) {
				pv.prefix = first;
				idx = 1;
			} else {
				pv.prefix = 0;
			}
			String rest = s.substring(idx);
			// split off underscore patch rev
			String main = rest;
			pv.patchRev = 0;
			if(rest.contains("_")) {
				String[] parts = rest.split("_", 2);
				main = parts[0];
				String pr = parts[1].replaceAll("[^0-9]", "");
				if(!pr.isEmpty()) {
					try { pv.patchRev = Integer.parseInt(pr); } catch(NumberFormatException e) { pv.patchRev = 0; }
				}
			}
			String[] nums = main.split("\\.");
			pv.major = nums.length > 0 ? parseIntSafe(nums[0]) : 0;
			pv.minor = nums.length > 1 ? parseIntSafe(nums[1]) : 0;
			pv.patch = nums.length > 2 ? parseIntSafe(nums[2]) : 0;
			return pv;
		}
		
		private static int parseIntSafe(String in) {
			if(in == null) return 0;
			String cleaned = in.replaceAll("[^0-9]", "");
			if(cleaned.isEmpty()) return 0;
			try { return Integer.parseInt(cleaned); } catch(NumberFormatException e) { return 0; }
		}
	}
}*/