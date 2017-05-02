package net.macdidi5.at.hellorelay;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    // 控制繼電器的Handler物件
    private Handler relayHandler = new Handler();
    // 連接繼電器的GPIO物件
    private Gpio relayGpio;
    // 連接繼電器的GPIO pin
    private final String PIN_NAME = "BCM23";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "onCreate");

        // 建立設備管理員服物物件
        PeripheralManagerService service = new PeripheralManagerService();

        try {
            // 建立GPIO物件
            relayGpio = service.openGpio(PIN_NAME);
            // 設定GPIO為輸出模式，預設為低電壓
            relayGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);

            // 啟動控制繼電器的Handler物件
            relayHandler.post(relayRunnable);
        }
        catch (IOException e) {
            Log.e(TAG, e.toString());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.i(TAG, "onDestroy");

        // 移除控制繼電器的Handler物件
        relayHandler.removeCallbacks(relayRunnable);

        try {
            // 關閉GPIO物件
            relayGpio.close();
        }
        catch (IOException e) {
            Log.e(TAG, e.toString());
        }
        finally {
            relayGpio = null;
        }
    }

    // 控制繼電器的Handler物件
    private Runnable relayRunnable = new Runnable() {
        @Override
        public void run() {
            if (relayGpio == null) {
                return;
            }

            try {
                // 設定GPIO狀態
                relayGpio.setValue(!relayGpio.getValue());
                Log.i(TAG, "Relay: " + (relayGpio.getValue() ? "ON" : "OFF"));
                // 1秒以後重複執行
                relayHandler.postDelayed(relayRunnable, 1000);
            }
            catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }
    };

}
