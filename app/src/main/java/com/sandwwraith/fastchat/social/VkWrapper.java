package com.sandwwraith.fastchat.social;

import java.io.InputStream;
import java.net.URL;

/**
 * Created by sandwwraith(@gmail.com)
 * ITMO University, 2015.
 */
public class VkWrapper implements SocialWrapper {

    private final String LOG_TAG = "vk_wrapper";

    @Override
    public String retrieveToken(String url) {
        return null;
    }

    @Override
    public String retrieveId(String url) {
        return null;
    }

    @Override
    public URL generateUserInfoRequest(String token, String id) {
        return null;
    }

    @Override
    public URL generateTokenRequest() {
        return null;
    }

    @Override
    public SocialUser parseUserData(InputStream in) {
        return null;
    }
}
