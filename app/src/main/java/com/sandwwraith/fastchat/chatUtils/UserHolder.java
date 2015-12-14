package com.sandwwraith.fastchat.chatUtils;

import android.util.JsonReader;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStreamReader;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Azarn on 14.12.2015.
 */
public class UserHolder {
    public String username;
    public String link;

    public UserHolder(String username, String link) {
        this.username = username;
        this.link = link;
    }

    public UserHolder(String json) {
        try {
            JSONObject jo = new JSONObject(json);
            username = jo.getString("username");
            link = jo.getString("link");
        } catch (JSONException je) {

        }
    }

    public String ConvertToJson() {
        try {
            JSONObject jo = new JSONObject();
            jo.put("username", username);
            jo.put("link", link);
            return jo.toString();
        } catch (JSONException je) {
            return null;
        }
    }
}
