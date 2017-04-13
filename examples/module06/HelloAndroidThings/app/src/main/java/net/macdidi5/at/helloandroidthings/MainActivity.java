package net.macdidi5.at.helloandroidthings;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import java.util.Properties;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        Properties ps = System.getProperties();
        Log.d(TAG, "os.name: " + ps.getProperty("os.name"));
        Log.d(TAG, "os.arch: " + ps.getProperty("os.arch"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }
}
