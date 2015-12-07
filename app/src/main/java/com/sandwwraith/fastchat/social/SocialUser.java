package com.sandwwraith.fastchat.social;

/**
 * Created by sandwwraith(@gmail.com)
 * ITMO University, 2015.
 */
public class SocialUser {

    private final String firstName;
    private final String lastName;
    private final String link;
    private final long id;
    private final SocialManager.Types type;
    private final byte gender;
    private final String image_link;

    public SocialUser(String id_str, SocialManager.Types type, String firstName, String lastName, String link, String image_link, int sex) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.link = link;
        this.id = Long.parseLong(id_str);
        this.type = type;
        this.gender = (byte) sex;
        this.image_link = image_link;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getLink() {
        return link;
    }

    public byte getGender() {
        return gender;
    }

    public long getId() {
        return id;
    }

    public String toString() {
        return firstName + " " + lastName;
    }

    public SocialManager.Types getType() {
        return type;
    }

    public String getImageUrl() {
        return image_link;
    }
}
