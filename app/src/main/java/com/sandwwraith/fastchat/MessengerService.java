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
import java.net.Socket;

public class MessengerService extends Service {

    public static final String LOG_TAG = "Main_Tag";
    public static final String ADDRESS = "192.168.2.18";

    //Connection to service
    //Refer to documentation, "Bound service"
    //or to lesson #5
    private final MessengerBinder binder = new MessengerBinder();
    private Socket sock = null;
    private Thread launcher = null;
    private MessengerService.MessageReceiver callback = null;
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
     */
    public void connect() {
        launcher = new Thread(new SimpleConnection());
        launcher.start();
    }

    /**
     * Делает join() для потока, открывающего соединение
     *
     * @throws IllegalStateException Если запрос на соединение не был послан
     */
    private void waitConnection() throws IllegalStateException {
        //Waiting for socket to connect
        if (launcher != null) {
            try {
                launcher.join();
            } catch (InterruptedException e) {
                Log.wtf(MessengerService.LOG_TAG, e.getMessage());
            }
        } else {
            throw new IllegalStateException("Sending to unlaunched socket");
        }
    }

    /**
     * Посылает сообщение на сервер
     * @param msg Текст сообщения
     */
    public void send(String msg) {
        new Thread(new DataSender(msg)).start();
    }

    /**
     * Запускает поток, который будет "слушать" входящий с сервера поток
     * @param callback Функиция, которая будет вызвана при получении сообщения
     */
    public void startReceiving(MessengerService.MessageReceiver callback) {
        this.callback = callback;
        receiveTask = new ReceiveTask();
        receiveTask.execute();
    }

    /**
     * Обрывает приём данных и закрывает сокет
     * TODO: Дождаться всех запущенных тредов. Или сделать как forceclose() ?
     * @throws IOException Если что-то пошло не так.
     */
    public void close() throws IOException {
        waitConnection();
        send("\u0003"); //Ctrl+C
        if (receiveTask != null)
            receiveTask.cancel(false);
        if (sock != null)
            sock.close();

    }

    public interface MessageReceiver {
        void processMessage(String msg);
    }

    public class MessengerBinder extends Binder {
        public MessengerService getService() {
            return MessengerService.this;
        }
    }

    //TODO: Rework to use AsyncTask such we can use callbacks -
    //чтобы, например, делать активной кнопку при успехе соединения и т.п.
    private class SimpleConnection implements Runnable {
        @Override
        public void run() {
            BufferedReader in = null;

            try {
                if (sock != null) sock.close();
                //TODO: Add timeout
                sock = new Socket(MessengerService.ADDRESS, 2539);
            } catch (IOException e) {
                Log.e(MessengerService.LOG_TAG, "Can't open socket: " + e.getMessage());
                return;
            }

            try {
                //Приём сообщения, сразу посылаемого сервером новым клиентам
                in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                Log.i(MessengerService.LOG_TAG, "Greetings: " + in.readLine());

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
        }
    }

    private class DataSender implements Runnable {
        private final String message;

        public DataSender(String msg) {
            this.message = msg + "\n";
        }

        @Override
        public void run() {
            waitConnection();

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
            waitConnection();
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
