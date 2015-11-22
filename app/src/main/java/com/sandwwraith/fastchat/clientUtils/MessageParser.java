package com.sandwwraith.fastchat.clientUtils;

import android.support.annotation.Nullable;

import java.util.Date;

/**
 * Created by sandwwraith(@gmail.com)
 * ITMO University, 2015.
 */
public class MessageParser {
    public interface MessageResult {
        void onPairFound(Pair<int[], String> companion);

        void onTextMessageReceived(Pair<Date, String> message);

        void onTimeout();

        void onVotingResults(Pair<String, String> voted);

        void onLeave();

        void onServerError();

        void onMalformedSequence(@Nullable String errorDescription);
    }

    private MessageResult callBack;

    public MessageParser(MessageResult callBack) {
        this.callBack = callBack;
    }

    public void parse(byte[] raw) {
        MessageDeserializer d = new MessageDeserializer();
        try {
            if (raw.length < 2)
                throw new MessageDeserializer.MessageDeserializerException("Not enough seq length to detect type");

            switch (raw[1]) {
                case MessageType.QUEUE:
                    callBack.onPairFound(d.deserializePairFound(raw));
                    break;
                case MessageType.MESSAGE:
                    callBack.onTextMessageReceived(d.deserializeMessage(raw));
                    break;
                case MessageType.VOTING:
                    callBack.onVotingResults(d.deserializeVoting(raw));
                    break;
                case MessageType.TIMEOUT:
                    callBack.onTimeout();
                    break;
                case MessageType.LEAVE:
                    callBack.onLeave();
                    break;
                case MessageType.FATAL_ERROR:
                    callBack.onServerError();
                    break;
                default:
                    callBack.onMalformedSequence(null);
                    break;

            }
        } catch (MessageDeserializer.MessageDeserializerException e) {
            callBack.onMalformedSequence(e.getMessage());
        }
    }
}
