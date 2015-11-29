package com.sandwwraith.fastchat.clientUtils;

import android.support.annotation.Nullable;

import com.sandwwraith.fastchat.MessengerService;

import java.util.Date;

/**
 * Created by sandwwraith(@gmail.com)
 * ITMO University, 2015.
 */
public class MessageParser implements MessengerService.messageHandler {

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
        try {
            if (raw.length < 2)
                throw new MessageDeserializer.MessageDeserializerException("Not enough seq length to detect type");

            switch (raw[1]) {
                case MessageType.QUEUE:
                    callBack.onPairFound(MessageDeserializer.deserializePairFound(raw));
                    break;
                case MessageType.MESSAGE:
                    callBack.onTextMessageReceived(MessageDeserializer.deserializeMessage(raw));
                    break;
                case MessageType.VOTING:
                    callBack.onVotingResults(MessageDeserializer.deserializeVoting(raw));
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

    @Override
    public void processMessage(byte[] bytes) {
        parse(bytes);
    }
}
