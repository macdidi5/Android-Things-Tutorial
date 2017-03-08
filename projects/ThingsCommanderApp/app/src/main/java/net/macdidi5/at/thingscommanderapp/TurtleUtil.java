package net.macdidi5.at.thingscommanderapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TurtleUtil {

    private static SharedPreferences sp = null;

    public static final String KEY_COMMANDER = "PI_COMMANDER";

    public static final String CONTROLLER_COMMANDER = "CONTROLLER";
    public static final String LISTENER_COMMANDER = "LISTENER";

    public static boolean isFirstTime(Context context){
        return getSharedPreferences(context).getBoolean("FIRST_TIME", true);
    }

    public static void setFirstTime(Context context) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putBoolean("FIRST_TIME", false);
        editor.commit();
    }

    public static void savePref(Context context, String key, String value) {
        SharedPreferences.Editor editor =
                getSharedPreferences(context).edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static String getPref(Context context, String key, String def) {
        return getSharedPreferences(context).getString(key, def);
    }

    /**
     * Save user define command blocks
     *
     * @param context Android Context
     * @param items Command block objects
     */
    public static void saveCommanders(Context context,
                                      List<CommanderItem> items) {
        SharedPreferences.Editor editor =
                getSharedPreferences(context).edit();

        for (int i = 0; i < items.size(); i++) {
            CommanderItem item = items.get(i);

            String data = item.getGpioName() + "," +
                    item.getDesc() + "," +
                    item.getHighDesc() + "," +
                    item.getLowDesc() + "," +
                    item.getCommandType() + "," +
                    item.isHighNotify() + "," +
                    item.isLowNotify();

            editor.putString(KEY_COMMANDER + item.getCommandType() +
                    String.format("%02d", i), data);
        }

        editor.apply();
    }

    /**
     * Save user define command block
     *
     * @param context Android Context
     * @param position GridView item position
     */
    public static void deleteCommander(Context context, int position,
                                       String commandType) {
        SharedPreferences.Editor editor =
                getSharedPreferences(context).edit();
        editor.remove(KEY_COMMANDER  + commandType +
                String.format("%02d", position));
        editor.apply();
    }

    public static void logPref(Context context) {
        SharedPreferences sp = getSharedPreferences(context);

        Set<String> keys = sp.getAll().keySet();

        for (String key : keys) {
            Log.d("logPref=====", key + ": " + sp.getString(key, ""));
        }
    }


    public static List<CommanderItem> getControllers(Context context) {
        return readCommanders(context, CONTROLLER_COMMANDER);
    }

    public static List<CommanderItem> getListeners(Context context) {
        return readCommanders(context, LISTENER_COMMANDER);
    }

    /**
     * Read user define command blocks
     *
     * @param context Android Context
     * @return All command block objects
     */
    private static List<CommanderItem> readCommanders(Context context,
                                                      String commandType) {
        List<CommanderItem> result = new ArrayList<>();
        SharedPreferences sp = getSharedPreferences(context);

        int counter = 0;
        String keyPrefix = KEY_COMMANDER + commandType;

        while (true) {
            String key = keyPrefix + String.format("%02d", counter);
            String content = sp.getString(key, null);

            if (content == null) {
                break;
            }

            String[] ds = content.split(",");

            CommanderItem item = null;

            if (ds.length == 7) {
                item = new CommanderItem(ds[0], ds[1], ds[2], ds[3], ds[4],
                        Boolean.parseBoolean(ds[5]), Boolean.parseBoolean(ds[6]));
            }

            if (item != null) {
                result.add(item);
            }

            counter++;
        }

        return result;
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        if (sp == null) {
            return PreferenceManager.getDefaultSharedPreferences(context);
        }
        else {
            return sp;
        }
    }

    public static boolean checkNetwork(Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();

        if (info == null || !info.isConnected()) {
            return false;
        }

        return true;
    }

}
