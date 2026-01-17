package org.mcphackers.mcp.tools.versions.json;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class VersionMetadata {
    public String id;
    public String type;
    public String time;
    public String releaseTime;
    public String url;

    // New fields
    public String range;
    public List<String> serverVersions;
    public List<String> gameVersions;

    public static VersionMetadata from(JSONObject obj) {
        if (obj == null) {
            return null;
        }

        VersionMetadata meta = new VersionMetadata();

        // Required / existing fields
        meta.id = obj.optString("id", null);
        meta.time = obj.optString("time", null);
        meta.releaseTime = obj.optString("releaseTime", null);
        meta.type = obj.optString("type", null);
        meta.url = obj.optString("url", null);

        // New optional fields
        meta.range = obj.optString("range", null);
        meta.serverVersions = readStringArray(obj, "serverVersions");
        meta.gameVersions = readStringArray(obj, "gameVersions");

        return meta;
    }

    protected static List<String> readStringArray(JSONObject obj, String key) {
        if (!obj.has(key)) {
            return null;
        }

        JSONArray arr = obj.optJSONArray(key);
        if (arr == null) {
            return null;
        }

        List<String> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            list.add(arr.optString(i));
        }
        return list;
    }
}
