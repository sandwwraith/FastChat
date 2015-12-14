package com.sandwwraith.fastchat.chatUtils;

import com.sandwwraith.fastchat.clientUtils.Pair;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by sandwwraith(@gmail.com)
 * ITMO University, 2015.
 */
public class MessageHolder {
    public static final int M_SEND = 0x1;
    public static final int M_RECV = 0x2;

    private final int type; // Indicates if this message was "my" (sent to server) or from my opponent
    private final Date time;
    private final String msg;

    /**
     * Создаёт новый элемент сообщения из пары и типа.
     * Лучше использовать для создания из принятого сообщения
     *
     * @param msg  Пара принятого сообщения
     * @param type Тип, см. константы класса
     */
    public MessageHolder(Pair<Date, String> msg, int type) {
        this.type = type;
        this.time = new Date(); //Время получения на этом устройстве, а не отправки на том
        this.msg = msg.second;
    }

    /**
     * Создаёт сообщение из строки, выставляя дату как текущую, а тип сообщения - как посланное
     * Удобно для хранения только что посланных сообщений
     *
     * @param msg Текст сообщения
     */
    public MessageHolder(String msg) {//Use this to create a sent message
        this.time = new Date();
        this.type = M_SEND;
        this.msg = msg;
    }

    public int getType() {
        return type;
    }

    public String getFormattedDate() {
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        return df.format(time);
    }

    public String getMessage() {
        return msg;
    }
}
