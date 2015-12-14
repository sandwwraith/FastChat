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
import java.lang.ref.WeakReference;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by sandwwraith(@gmail.com)
 * ITMO University, 2015.
 */
public class SocialManager {

    private static final SocialUser[] users = new SocialUser[Types.values().length]; //Freaking
    private final String LOG_TAG = "social_manager";
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

    public static SocialUser getUser(Types type) {
        return users[type.ordinal()];
    }

    private static void saveUser(Types type, SocialUser user) {
        users[type.ordinal()] = user;
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
        new UserInfoTask(callback).execute(type.name(), token, id);
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
        new UserInfoTask(callback).execute(type_s, tok, id);
    }

    public enum Types {
        TYPE_VK {
            @Override
            public String getTokenString() {
                return "type_vk_token";
            }

            @Override
            public String getIdString() {
                return "type_vk_id";
            }

            @Override
            public SocialWrapper getWrapper() {
                return new VkWrapper();
            }
        };

        public static final String Intent_Param = "INTENT_PARAM_SOCIAL_TYPE";

        public abstract String getTokenString();

        public abstract String getIdString();

        public abstract SocialWrapper getWrapper();
    }

    public interface SocialManagerCallback {
        /**
         * Вызыватеся после неуспешной проверки токенов. Нужно уведомить пользователя о необходимости авторизации
         */
        void onValidationFail(Types type);

        /**
         * Вызывается после успешного запроса данных пользователя
         *
         * @param user Данные о пользователе
         */
        void onUserInfoUpdated(SocialUser user);

        /**
         * Вызывается после неуспешного запроса данных пользователя
         *
         * @param type      Тип соц.сети
         * @param lastError Информация об ошибке
         */
        void onUserInfoFailed(Types type, SocialWrapper.ErrorStorage lastError);
    }

    /**
     * Класс получает из типа, токена и ид всю информацию о пользователе
     * Первый параметр - тип (как строка), второй - токен, третий - id
     *
     * у этого класса очень странное поведение:
     * onPreExecute выполняется моментально, а onPostExecute зачем то ждет
     * выполнения запланированого KillerTask в MessengerService, который с ним вроде бы никак не связан.
     * WTF ???!1
     * Кажется, решилось введением слабых ссылок
     */
    private static class UserInfoTask extends AsyncTask<String, Void, SocialUser> {
        private static final String LOG_TAG = "user_info_loader";
        private final WeakReference<SocialManagerCallback> callback;
        private Types type;
        private SocialWrapper.ErrorStorage lastError = null;

        UserInfoTask(SocialManagerCallback call) {
            callback = new WeakReference<>(call);
        }

        @Override
        protected void onPreExecute() {
            Log.d(LOG_TAG, "onPreExecute: ");
        }

        @Override
        protected void onPostExecute(SocialUser socialUser) {
            SocialManager.saveUser(type, socialUser);
            //callback.onUserInfoUpdated(socialUser != null, socialUser);
            SocialManagerCallback call = callback.get();
            if (call != null) {
                if (socialUser != null) {
                    call.onUserInfoUpdated(socialUser);
                } else {
                    call.onUserInfoFailed(type, this.lastError);
                }
            }
        }

        @Override
        protected SocialUser doInBackground(String... params) {
            Log.d(LOG_TAG, "Started fetching");
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
                SocialUser user = wrapper.parseUserData(in);
                if (user == null) {
                    this.lastError = wrapper.getLastError();
                }
                Log.d(LOG_TAG, "Finished fetching");
                return user;
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
