package com.sandwwraith.fastchat.clientUtils;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Date;

/**
 * Created by sandwwraith(@gmail.com)
 * ITMO University, 2015.
 */
public class MessageDeserializer {
    private static void validateSeq(byte[] raw, byte type, int requiredMinLength) throws MessageDeserializerException {
        if (raw.length < requiredMinLength)
            throw new MessageDeserializerException("Incorrect sequence length"
                    , Integer.toString(requiredMinLength), Integer.toString(raw.length));
        if (raw[0] != 42) throw new MessageDeserializerException("Incorrect start byte");
        if (raw[1] != type)
            throw new MessageDeserializerException("Invalid method for this sequence"
                    , Byte.toString(type), Byte.toString(raw[1]));
    }

    /**
     * Deserialize message from server about found opponent. Returns array of two elements and string.
     * First element of array - theme, second element - gender. String contains name
     *
     * @param raw Raw bytes got from server
     * @return Pair(int[], string) as described above
     * @throws MessageDeserializerException
     */
    public static Pair<int[], String> deserializePairFound(byte[] raw) throws MessageDeserializerException {
        validateSeq(raw, MessageType.QUEUE, 4);
        try {
            int theme = raw[2];
            int gend = (raw[3] & 3); //000X00YY, where Y - gender, X - language
            int len = raw[4];
            String s = new String(raw, 5, len, Charset.forName("UTF-8"));


            int[] res = new int[2];
            res[0] = theme;
            res[1] = gend;

            return new Pair<>(res, s);
        } catch (IndexOutOfBoundsException e) {
            throw new MessageDeserializerException("Unexpected end of sequence");
        }
    }

    public static Pair<Date, String> deserializeMessage(byte[] raw) throws MessageDeserializerException {
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
    public static Pair<String, String> deserializeVoting(byte[] raw) throws MessageDeserializerException {
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

    /**
     * Deserialize greetings message from server with number of users online
     *
     * @param raw Raw bytes from socket
     * @return Number of users on server by the connection moment
     * @throws MessageDeserializerException
     */
    public static int deserializeGreetings(byte[] raw) throws MessageDeserializerException {
        if (raw.length < 1) throw new MessageDeserializerException("Zero-length sequence");
        if (raw[0] != 42)
            throw new MessageDeserializerException("Invalid response code", Integer.toString(42), Byte.toString(raw[0]));
        if (raw.length < 5)
            throw new MessageDeserializerException("Insufficient length", Integer.toString(5), Integer.toString(raw.length));

        ByteBuffer buf = ByteBuffer.allocate(4);
        buf.put(raw, 1, 4);
        buf.flip();
        return buf.getInt();
    }

    public static class MessageDeserializerException extends Exception {

        MessageDeserializerException(String s) {
            super(s);
        }

        MessageDeserializerException(String s, String required, String found) {
            super(s + " Required: " + required + " Found: " + found);
        }
    }
}
