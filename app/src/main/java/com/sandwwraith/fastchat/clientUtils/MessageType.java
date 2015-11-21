package com.sandwwraith.fastchat.clientUtils;

/**
 * Created by sandwwraith(@gmail.com)
 * ITMO University, 2015.
 */
public class MessageType {
    public static final byte QUEUE = 1;
    public static final byte MESSAGE = 2;
    public static final byte TIMEOUT = 3;
    public static final byte VOTING = 4;

    public static final byte DISCONNECT = 10;
    public static final byte LEAVE = 69;

    public static final byte MSG_ERROR = 42;
    public static final byte FATAL_ERROR = 127;
}
