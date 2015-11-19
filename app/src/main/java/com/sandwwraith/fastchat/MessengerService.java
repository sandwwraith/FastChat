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

public class MessengerService extends Service {

    public static final String LOG_TAG = "Main_Tag";
    public static final String ADDRESS = "192.168.2.18";
    public static final int TIMEOUT = 5000; //in ms

    //Connection to service
    //Refer to documentation, "Bound service"
    //or to lesson #5
    private final MessengerBinder binder = new MessengerBinder();
    private Socket sock = null;
    private boolean socketAvailable = false;
    private ServerInteract callback = null;
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
    public void connect(ServerInteract callback) {
        this.callback = callback;
        new ConnectionTask().execute();
    }

    public boolean connected() {
        return socketAvailable;
    }

    /**
     * Посылает сообщение на сервер
     *
     * @param msg Текст сообщения
     * @throws IllegalStateException Если сокет не соединён
     */
    public void send(String msg) throws IllegalStateException {
        if (!connected()) throw new IllegalStateException("Not connected");
        new Thread(new DataSender(msg)).start();
    }

    /**
     * Запускает поток, который будет "слушать" входящий с сервера поток
     *
     * @param callback Функиция, которая будет вызвана при получении сообщения
     * @throws IllegalStateException Если сокет не соединён
     */
    public void setReceiver(ServerInteract callback) throws IllegalStateException {
        if (!connected()) throw new IllegalStateException("Not connected");
        this.callback = callback;
        receiveTask = new ReceiveTask();
        receiveTask.execute();
    }

    /**
     * Обрывает приём данных и закрывает сокет
     * TODO: Дождаться всех запущенных тредов. Или сделать как forceclose() ?
     *
     * @throws IOException Если что-то пошло не так.
     */
    public void close() throws IOException {
        if (!connected()) return;
        send("\u0003"); //Ctrl+C
        if (receiveTask != null)
            receiveTask.cancel(false);
        if (sock != null)
            sock.close();
    }

    public interface ServerInteract {
        void processMessage(String msg);

        void onConnectResult(boolean success);
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
            callback.onConnectResult(aBoolean);
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
        private final String message;

        public DataSender(String msg) {
            this.message = msg + "\n";
        }

        @Override
        public void run() {

            OutputStream out = null;
            try {
                Log.d(MessengerService.LOG_TAG, "Sending " + message);
                out = sock.getOutputStream();

                out.write(message.getBytes());
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
    private class ReceiveTask extends AsyncTask<Void, String, Void> {

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
                        publishProgress(s);
                    }
                }
            } catch (IOException e) {
                Log.e(MessengerService.LOG_TAG, e.getMessage());
                try {
                    //TODO: Привести секцию в более красивое состояние
                    Log.wtf(MessengerService.LOG_TAG, "Why i am closing?");
                    if (in != null) in.close();
                } catch (Exception w) {
                    Log.wtf(MessengerService.LOG_TAG, "Cannot close: " + w.getMessage());
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            if (MessengerService.this.callback != null)
                MessengerService.this.callback.processMessage(values[0]);
        }
    }

}
