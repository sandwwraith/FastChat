package com.sandwwraith.fastchat.clientUtils;

import com.sandwwraith.fastchat.social.SocialManager;
import com.sandwwraith.fastchat.social.SocialUser;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Date;

/**
 * Created by sandwwraith(@gmail.com)
 * ITMO University, 2015.
 */
public class MessageSerializer {

    public static byte[] queueUser(SocialUser user) {
        byte[] raw = user.getFirstName().getBytes(Charset.forName("UTF-8"));
        ByteBuffer buf = ByteBuffer.allocate(4 + raw.length);
        buf.put((byte) 42);
        buf.put(MessageType.QUEUE);
        byte magic = (byte) ((user.getType() == SocialManager.Types.TYPE_VK) ? 0 : 1);
        magic <<= 4;
        magic |= user.getGender();
        buf.put(magic);
        buf.put((byte) raw.length);
        buf.put(raw);
        return buf.array();
    }

    public static byte[] serializeMessage(String msg) {
        byte[] raw = msg.getBytes(Charset.forName("UTF-8"));
        long stamp = new Date().getTime();
        ByteBuffer buf = ByteBuffer.allocate(2 + 8 + 4 + raw.length);
        buf.put((byte) 42);
        buf.put(MessageType.MESSAGE);
        buf.putLong(stamp);
        buf.putInt(raw.length);
        buf.put(raw);
        return buf.array();
    }

    public static byte[] serializeVoting(boolean accepted, SocialUser user) {
        if (!accepted) return voteNotAccepted();
        byte[] raw1 = user.toString().getBytes(Charset.forName("UTF-8"));
        byte[] raw2 = user.getLink().getBytes(Charset.forName("UTF-8"));
        ByteBuffer buf = ByteBuffer.allocate(2 + 1 + 2 + raw1.length + 2 + raw2.length);
        buf.put((byte) 42);
        buf.put(MessageType.VOTING);
        buf.put((byte) 1);

        buf.putShort((short) raw1.length);
        buf.put(raw1);

        buf.putShort((short) raw2.length);
        buf.put(raw2);

        return buf.array();
    }

    private static byte[] voteNotAccepted() {
        byte[] b = new byte[3];
        b[0] = (byte) 42;
        b[1] = MessageType.VOTING;
        b[2] = (byte) 0;
        return b;
    }

    public static byte[] serializeTimeout() {
        byte[] b = new byte[2];
        b[0] = (byte) 42;
        b[1] = MessageType.TIMEOUT;
        return b;
    }

    public static byte[] serializeDisconnect() {
        byte[] b = new byte[2];
        b[0] = (byte) 42;
        b[1] = MessageType.DISCONNECT;
        return b;
    }

    public static byte[] serializeLeave() {
        byte[] b = new byte[2];
        b[0] = (byte) 42;
        b[1] = MessageType.LEAVE;
        return b;
    }
}
