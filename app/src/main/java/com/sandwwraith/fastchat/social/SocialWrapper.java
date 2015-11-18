package com.sandwwraith.fastchat.social;

import java.io.InputStream;
import java.net.URL;

/**
 * Created by sandwwraith(@gmail.com)
 * ITMO University, 2015.
 */
public interface SocialWrapper {
    String retrieveToken(URL url);

    String retrieveId(URL url);

    URL generateUserInfoRequest(String token, String id);

    URL generateTokenRequest();

    SocialUser parseUserData(InputStream in);
}
