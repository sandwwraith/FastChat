package com.sandwwraith.fastchat;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.sandwwraith.fastchat.clientUtils.MessageDeserializer;
import com.sandwwraith.fastchat.clientUtils.MessageSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class MessengerService extends Service {

    public static final String ADDRESS = "10.10.10.18";
    public static final int PORT = 2539;
    public static final int TIMEOUT = 5000; //in ms
    private static final String LOG_TAG = "msg_service";
    /**
     * Amount in ms after that service will be closed, if there are
     * no users of the service
     */
    private final static long KILL_DELAY = 1500;
    //Connection to service
    //Refer to documentation, "Bound service"
    //or to lesson #5
    private final MessengerBinder binder = new MessengerBinder();
    private Socket sock = null;
    private boolean socketAvailable = false;
    private WeakReference<connectResultHandler> connectCallback = null;
    private WeakReference<messageHandler> messageCallback = null;
    private ReceiveTask receiveTask = null;
    private int users = 0;
    private KillTask killer = null;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        users++;
        if (killer != null) {
            killer.cancel();
            killer = null;
        }
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        users--;
        if (users == 0) {
            Log.i(LOG_TAG, "No connection, closing");
            killer = new KillTask();
            new Timer().schedule(killer, KILL_DELAY);
        }
        return super.onUnbind(intent);
    }

    /**
     * Открывает сокет в отдельном потоке
     *
     * @param callback Вызывается onConnectResult при завершении
     */
    public void connect(connectResultHandler callback) {
        this.connectCallback = new WeakReference<>(callback);
        new ConnectionTask().execute();
    }

    public boolean connected() {
        return socketAvailable;
    }

    /**
     * Посылает сообщение на сервер
     *
     * @param msg Байты сообщения
     * @throws IllegalStateException Если сокет не соединён
     */
    public void send(byte[] msg) throws IllegalStateException {
//        if (!connected()) throw new IllegalStateException("Not connected");
        new Thread(new DataSender(msg)).start();
    }

    /**
     * Запускает поток, который будет "слушать" входящие с сервера данные
     *
     * @param callback Функиция, которая будет вызвана при получении сообщения
     * @throws IllegalStateException Если сокет не соединён
     */
    public void setReceiver(messageHandler callback) throws IllegalStateException {
//        if (!connected()) throw new IllegalStateException("Not connected");
        if (receiveTask != null) receiveTask.cancel(false);
        this.messageCallback = new WeakReference<>(callback);
        receiveTask = new ReceiveTask();
        receiveTask.execute();
    }

    /**
     * Снимает заднный callback с прослушивания
     *
     * @param callback Если эта функция совпадает с текущим ресивером, она будет отменена
     *                 Иначе ничего не происходит
     */
    public void unbindReceiver(messageHandler callback) {
        if (messageCallback != null) {
            if (messageCallback.get() == callback) {
                messageCallback.clear();
                messageCallback = null;
                receiveTask.cancel(true);
            }
        }
    }

    public interface connectResultHandler {
        void onConnectResult(boolean success, int usersOnline);
    }

    public interface messageHandler {
        void processMessage(byte[] bytes);
    }

    public class MessengerBinder extends Binder {
        public MessengerService getService() {
            return MessengerService.this;
        }
    }

    /**
     * Открывает сокет
     */
    private class ConnectionTask extends AsyncTask<Void, Void, Boolean> {
        int users = -1;

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            socketAvailable = aBoolean;
            if (connectCallback != null) {
                connectResultHandler call = connectCallback.get();
                if (call != null) call.onConnectResult(aBoolean, users);
            }
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                if (sock != null)
                    sock.close(); //Previous state was lost due to recreating MainActivity
                sock = new Socket();
                sock.connect(new InetSocketAddress(ADDRESS, PORT), TIMEOUT);
            } catch (IOException e) {
                Log.e(MessengerService.LOG_TAG, "Can't open socket: " + e.getMessage());
                return false;
            }

            try {
                //Приём сообщения, сразу посылаемого сервером новым клиентам
                byte[] greet = new byte[5];
                if (sock.getInputStream().read(greet, 0, 5) < 5)
                    throw new IOException("Not enough length!");
                users = MessageDeserializer.deserializeGreetings(greet);
                Log.i(MessengerService.LOG_TAG, "Users online: " + Arrays.toString(greet));
                return true;
            } catch (IOException | MessageDeserializer.MessageDeserializerException e) {
                Log.e(MessengerService.LOG_TAG, "Cannot read greetings: " + e.getMessage());
                try {
                    if (sock != null) {
                        sock.close();
                        sock = null;
                    }
                } catch (Exception ee) {
                    Log.wtf(MessengerService.LOG_TAG, ee.getMessage());
                }
            }
            return false;
        }
    }

    /**
     * Запускает один поток для отправки сообщения на сервер, закрывается
     */
    private class DataSender implements Runnable {
        private final byte[] message;

        public DataSender(byte[] msg) {
            this.message = msg;
        }

        @Override
        public void run() {

            OutputStream out = null;
            try {
                Log.d(MessengerService.LOG_TAG, "Sending " + Arrays.toString(message));
                out = sock.getOutputStream();

                out.write(message);
                out.flush();
            } catch (IOException e) {
                Log.e(MessengerService.LOG_TAG, "Cannot send data: " + e.getMessage());
                try {
                    if (out != null) out.close();
                } catch (Exception w) {
                    Log.wtf(MessengerService.LOG_TAG, "Cannot close: " + w.getMessage());
                }
            }
        }
    }

    /**
     * Поток, который постоянно запущен и получает сообщения с сервера.
     * Вызывает callback в UI потоке.
     */
    private class ReceiveTask extends AsyncTask<Void, byte[], Void> {

        @Override
        protected Void doInBackground(Void... params) {
            InputStream in = null;
            try {
                in = sock.getInputStream();
                while (!isCancelled()) {
                    //Крутимся, пока не был отменён приём

                    if (in.available() > 0) {
                        Log.d(MessengerService.LOG_TAG, "Available: " + in.available());
                        byte[] raw = new byte[in.available()];
                        if (in.read(raw) == -1) throw new IOException("Unexpected end of stream");
                        String s = new String(raw);
                        Log.d(MessengerService.LOG_TAG, "Received: " + s);
                        publishProgress(raw);
                    }
                }
            } catch (IOException e) {
                Log.e(MessengerService.LOG_TAG, e.getMessage());
                try {
                    if (in != null) in.close();
                } catch (Exception w) {
                    Log.wtf(MessengerService.LOG_TAG, "Cannot close: " + w.getMessage());
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(byte[]... values) {
            if (MessengerService.this.messageCallback != null) {
                messageHandler call = messageCallback.get();
                if (call != null) call.processMessage(values[0]);
            }
        }
    }

    /**
     * Поток, который посылает сообщение на сервер о завершении работы
     * и закрывает все соединения
     */
    private class KillTask extends TimerTask {
        @Override
        public void run() {
            if (!connected()) return;
            if (MessengerService.this.receiveTask != null) receiveTask.cancel(true);
            receiveTask = null;
            DataSender s = new DataSender(MessageSerializer.serializeDisconnect());
            s.run();
            if (MessengerService.this.sock != null)
                try {
                    sock.close();
                } catch (IOException e) {
                    Log.wtf(LOG_TAG, "Cannot close socket: " + e.getMessage());
                }
            Log.i(LOG_TAG, "Disconnect success");
            MessengerService.this.stopSelf();
        }
    }

}
