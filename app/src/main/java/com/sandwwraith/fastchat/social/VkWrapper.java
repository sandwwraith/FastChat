package com.sandwwraith.fastchat.social;

import android.util.JsonReader;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * Created by sandwwraith(@gmail.com)
 * ITMO University, 2015.
 */
public class VkWrapper implements SocialWrapper {

    private final String LOG_TAG = "vk_wrapper";
    String AppID = "5154453";

    @Override
    public String retrieveToken(String url) {

        if (url.contains("#access_token=")) {
            android.net.Uri uri = android.net.Uri.parse(url);
            String authCode = uri.getFragment().substring(13); // access_token=
            int amp_pos = authCode.indexOf('&');
            authCode = authCode.substring(0, amp_pos);
            return authCode;
        }
        return null;
    }

    @Override
    public String retrieveId(String url) {
        if (url.contains("#access_token=")) {
            android.net.Uri uri = android.net.Uri.parse(url);
            String authCode = uri.getFragment().substring(13); // access_token=
            int amp_pos = authCode.lastIndexOf('&');
            authCode = authCode.substring(amp_pos);
            return authCode;
        }
        return null;
    }

    @Override
    public URL generateUserInfoRequest(String token, String id) {
        String part1 = "https://api.vk.com/method/users.get?v=5.8&users_ids=";
        String part2 = "&fields=photo_100&access_token=";
        URL res = null;
        try {
            res = new URL(part1 + id + part2 + token);
        } catch (Exception e) {
            Log.wtf(LOG_TAG, "URL WTF");
        }
        return res;
    }

    @Override
    public String generateTokenRequest() {
        String part1 = "https://oauth.vk.com/authorize?client_id=";
        String part2 = "&display=mobile&redirect_uri=https://oauth.vk.com/blank.html&response_type=token";
        return part1 + AppID + part2;
    }

    @Override
    public SocialUser parseUserData(InputStream in) {
        //Example response:
        // {"response":[{"uid":54577011,"first_name":"Леонид","last_name":"Старцев","photo_100":"http:\/\/cs622922.vk.me\/v622922011\/4e849\/jHtGvaKk45E.jpg"}]}
        JsonReader reader = new JsonReader(new InputStreamReader(in));
        try {
            reader.beginObject();
            String resp = reader.nextName();
            if (!resp.equals("response")) {
                //TODO: Normal error handling
                return null;
            }
            reader.beginArray();
            reader.beginObject();
            String first = "", last = "", id = "-1";
            while (reader.hasNext()) {
                String section = reader.nextName();
                switch (section) {
                    case "id":
                        id = reader.nextString();
                        break;
                    case "first_name":
                        first = reader.nextString();
                        break;
                    case "last_name":
                        last = reader.nextString();
                        break;
                    default:
                        reader.skipValue();
                }
            }
            reader.endObject();
            reader.endArray();
            reader.endObject();
            String link = "https://vk.com/id" + id;
            return new SocialUser(id, SocialManager.Types.TYPE_VK, first, last, link);

        } catch (IOException e) {
            Log.e(LOG_TAG, "Parser exception: " + e.getMessage());
        }
        return null;
    }
}
