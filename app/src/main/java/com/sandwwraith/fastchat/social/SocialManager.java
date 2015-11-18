package com.sandwwraith.fastchat.social;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.sandwwraith.fastchat.AuthActivity;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by sandwwraith(@gmail.com)
 * ITMO University, 2015.
 */
public class SocialManager {

    private final String LOG_TAG = "social_manager";

    public enum Types {
        TYPE_VK, TYPE_DUMMY;

        public static final String Intent_Param = "INTENT_PARAM_TYPE";

        public String getTokenString() {
            if (this == TYPE_VK) return "type_vk_token";
            else throw new IllegalStateException("Incorrect type");
        }

        public String getIdString() {
            if (this == TYPE_VK) return "type_vk_id";
            else throw new IllegalStateException("Incorrect type");
        }

        public SocialWrapper getWrapper() {
            if (this == TYPE_VK) return new VkWrapper();
            else throw new IllegalStateException("Incorrect type");
        }
    }

    private static SocialUser[] users = new SocialUser[Types.values().length]; //Freaking


    public interface SocialManagerCallback {
        /**
         * Вызыватеся после неуспешной проверки токенов. Нужно уведомить пользователя о необходимости авторизации
         */
        void onValidationFail(Types type);

        /**
         * Вызывается после запроса данных пользователя
         *
         * @param success Успешен ли запрос
         * @param user    Данные о пользователе, если первое поле true; иначе null
         */
        void onUserInfoUpdated(boolean success, SocialUser user);
    }


    public static SocialUser getUser(Types type) {
        return users[type.ordinal()];
    }

    private static void saveUser(Types type, SocialUser user) {
        users[type.ordinal()] = user;
    }

    private Activity prefHost;
    private SocialManagerCallback callback;

    /**
     * Создаёт новый экземпляр, привязанный к текущей Activity. Привязка нужна для получения SharedPreferences,
     * в которых хранятся токены и id пользователей
     *
     * @param context  Activity, из SharedPreferences которох берутся токены
     * @param callback Вызыватеся при завершении методов по проверке токенов и авторизации
     */
    public SocialManager(Activity context, SocialManagerCallback callback) {
        this.prefHost = context;
        this.callback = callback;
    }

    /**
     * Проверяет токены пользователя, которые берёт из SharedPreferences
     *
     * @param type Тип соцсети
     */
    public void validateToken(Types type) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(prefHost.getApplicationContext());
        if (!sp.contains(type.getTokenString())) {
            //No saved token
            Log.d(LOG_TAG, "No saved token");
            callback.onValidationFail(type);
            return;
        }
        String token = sp.getString(type.getTokenString(), "");
        String id = sp.getString(type.getIdString(), "");
        new UserInfoTask().execute(type.name(), token, id);
    }

    /**
     * Запускает AuthActivity для авторизации пользователя.
     * Запустившая активность должна получить Intent в onActivityResult()
     * и передать его в continueAuth()
     *
     * @param type Тип соцсети
     */
    public void startAuth(Types type) {
        Intent intent = new Intent(prefHost, AuthActivity.class);
        intent.putExtra(Types.Intent_Param, type.name());
        prefHost.startActivityForResult(intent, 1);
    }

    public void continueAuth(Intent intent) {
        String type_s = intent.getStringExtra(Types.Intent_Param);
        Types type = Types.valueOf(type_s);
        String tok = intent.getStringExtra(type.getTokenString());
        String id = intent.getStringExtra(type.getIdString());

        Log.d(LOG_TAG, "Token: " + tok);
        Log.d(LOG_TAG, "ID: " + id);

        //Saving
        SharedPreferences.Editor sp = PreferenceManager.getDefaultSharedPreferences(prefHost.getApplicationContext())
                .edit();
        sp.putString(type.getTokenString(), tok);
        sp.putString(type.getIdString(), id);
        sp.apply();

        //Launch updateInfo
        new UserInfoTask().execute(type_s, tok, id);
    }

    /**
     * Класс получает из типа, токена и ид всю информацию о пользователе
     * Первый параметр - тип (как строка), второй - токен, третий - id
     */
    private class UserInfoTask extends AsyncTask<String, Void, SocialUser> {
        Types type;

        @Override
        protected void onPostExecute(SocialUser socialUser) {
            SocialManager.saveUser(type, socialUser);
            callback.onUserInfoUpdated(socialUser != null, socialUser);
        }

        @Override
        protected SocialUser doInBackground(String... params) {
            type = Types.valueOf(params[0]);
            String token = params[1];
            String id = params[2];
            SocialWrapper wrapper = type.getWrapper();
            URL url = wrapper.generateUserInfoRequest(token, id);
            Log.d(LOG_TAG, "Generated URL: " + url.toString());

            HttpsURLConnection conn = null;
            InputStream in = null;
            try {

                conn = (HttpsURLConnection) url.openConnection();
                in = conn.getInputStream();
                return wrapper.parseUserData(in); //TODO: Parsing exceptions
            } catch (IOException e) {
                Log.e(LOG_TAG, "getting info error: " + e.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
                try {
                    if (in != null) in.close();
                } catch (Exception w) {
                    Log.wtf(LOG_TAG, w.getMessage());
                }
            }
            return null;
        }
    }
}
