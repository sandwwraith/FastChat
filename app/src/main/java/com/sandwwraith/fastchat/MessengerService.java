package com.sandwwraith.fastchat;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;

public class MessengerService extends Service {

    public static final String LOG_TAG = "msg_service";
    public static final String ADDRESS = "10.10.10.18";
    public static final int TIMEOUT = 5000; //in ms

    //Connection to service
    //Refer to documentation, "Bound service"
    //or to lesson #5
    private final MessengerBinder binder = new MessengerBinder();
    private Socket sock = null;
    private boolean socketAvailable = false;
    private connectResultHandler connectCallback = null;
    private messageHandler messageCallback = null;
    private ReceiveTask receiveTask = null;

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        try {
            close();
        } catch (Exception e) {
            Log.wtf(LOG_TAG, "Cannot stop service: " + e.getMessage());
        }

    }

    /**
     * Открывает сокет в отдельном потоке
     *
     * @param callback Вызывается onConnectResult при завершении
     */
    public void connect(connectResultHandler callback) {
        this.connectCallback = callback;
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
        if (!connected()) throw new IllegalStateException("Not connected");
        new Thread(new DataSender(msg)).start();
    }

    /**
     * Запускает поток, который будет "слушать" входящий с сервера поток
     *
     * @param callback Функиция, которая будет вызвана при получении сообщения
     * @throws IllegalStateException Если сокет не соединён
     */
    public void setReceiver(messageHandler callback) throws IllegalStateException {
        if (!connected()) throw new IllegalStateException("Not connected");
        if (receiveTask != null) receiveTask.cancel(false);
        this.messageCallback = callback;
        receiveTask = new ReceiveTask();
        receiveTask.execute();
    }

    /**
     * Обрывает приём данных и закрывает сокет
     * TODO: Normal closing
     *
     * @throws IOException Если что-то пошло не так.
     */
    public void close() throws IOException {
        if (!connected()) return;
        send(new byte[]{3}); //Ctrl+C
        if (receiveTask != null)
            receiveTask.cancel(false);
        if (sock != null)
            sock.close();
    }

    public interface connectResultHandler {
        void onConnectResult(boolean success);
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
        @Override
        protected void onPostExecute(Boolean aBoolean) {
            socketAvailable = aBoolean;
            connectCallback.onConnectResult(aBoolean);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            BufferedReader in = null;

            try {
                if (sock != null) sock.close(); //TODO: Think how can it happen and what to do
                sock = new Socket();
                sock.connect(new InetSocketAddress(ADDRESS, 2539), TIMEOUT);
            } catch (IOException e) {
                Log.e(MessengerService.LOG_TAG, "Can't open socket: " + e.getMessage());
                return false;
            }

            try {
                //Приём сообщения, сразу посылаемого сервером новым клиентам
                in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                Log.i(MessengerService.LOG_TAG, "Greetings: " + in.readLine());
                return true;
            } catch (IOException e) {
                Log.e(MessengerService.LOG_TAG, "Cannot read greetings: " + e.getMessage());
                try {
                    if (sock != null) {
                        sock.close();
                        sock = null;
                    }
                    if (in != null) in.close();
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
            if (MessengerService.this.messageCallback != null)
                MessengerService.this.messageCallback.processMessage(values[0]);
        }
    }

}
