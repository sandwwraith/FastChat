package com.sandwwraith.fastchat.social;

import java.net.URL;

/**
 * Created by sandwwraith(@gmail.com)
 * ITMO University, 2015.
 */
public class SocialUser {

    public SocialUser(String id_str, SocialManager.Types type, String firstName, String lastName, URL link) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.link = link;
        this.id = Long.parseLong(id_str);
        this.type = type;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public URL getLink() {
        return link;
    }

    public long getId() {
        return id;
    }

    public SocialManager.Types getType() {
        return type;
    }

    private final String firstName;
    private final String lastName;
    private final URL link;
    private final long id;
    private final SocialManager.Types type;
}
