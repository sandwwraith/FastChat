package com.sandwwraith.fastchat.clientUtils;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Date;

/**
 * Created by sandwwraith(@gmail.com)
 * ITMO University, 2015.
 */
public class MessageDeserializer {
    public static class MessageDeserializerException extends Exception {
        MessageDeserializerException() {
            super();
        }

        MessageDeserializerException(String s) {
            super(s);
        }

        MessageDeserializerException(String s, String required, String found) {
            super(s + " Required: " + required + " Found: " + found);
        }
    }

    private void validateSeq(byte[] raw, byte type, int requiredMinLength) throws MessageDeserializerException {
        if (raw.length < requiredMinLength)
            throw new MessageDeserializerException("Incorrect sequence length"
                    , Integer.toString(requiredMinLength), Integer.toString(raw.length));
        if (raw[0] != 42) throw new MessageDeserializerException("Incorrect start byte");
        if (raw[1] != type)
            throw new MessageDeserializerException("Invalid method for this sequence"
                    , Byte.toString(type), Byte.toString(raw[1]));
    }

    public Pair<Integer, String> deserializePairFound(byte[] raw) throws MessageDeserializerException {
        validateSeq(raw, MessageType.QUEUE, 4);
        try {
            int gend = (raw[2] & 3); //000X00YY, where Y - gender, X - language
            int len = raw[3];
            String s = new String(raw, 4, len, Charset.forName("UTF-8"));
            return new Pair<>(gend, s);
        } catch (IndexOutOfBoundsException e) {
            throw new MessageDeserializerException("Unexpected end of sequence");
        }
    }

    public Pair<Date, String> deserializeMessage(byte[] raw) throws MessageDeserializerException {
        validateSeq(raw, MessageType.MESSAGE, 14);

        try {
            ByteBuffer buf = ByteBuffer.allocate(8 + 4);
            buf.put(raw, 2, 12);
            buf.flip();
            Date date = new Date(buf.getLong());
            int len = buf.getInt();
            String msg = new String(raw, 14, len, Charset.forName("UTF-8"));

            return new Pair<>(date, msg);
        } catch (IndexOutOfBoundsException e) {
            throw new MessageDeserializerException("Unexpected end of sequence");
        }
    }

    /**
     * If voting unsuccessful, returns (null,null)
     * Returns (name, URL) if success.
     */
    public Pair<String, String> deserializeVoting(byte[] raw) throws MessageDeserializerException {
        validateSeq(raw, MessageType.VOTING, 3);
        try {
            if (raw[2] == 0)
                return new Pair<>();
            ByteBuffer buf = ByteBuffer.allocate(2);
            buf.put(raw, 3, 2);
            buf.flip();
            int len = buf.getShort();

            String name = new String(raw, 5, len, Charset.forName("UTF-8"));

            buf.clear();
            buf.put(raw, 5 + len, 2);
            buf.flip();
            int len_url = buf.getShort();

            String url = new String(raw, 7 + len, len_url, Charset.forName("UTF-8"));
            return new Pair<>(name, url);
        } catch (IndexOutOfBoundsException e) {
            throw new MessageDeserializerException("Unexpected end of sequence");
        }
    }
}
