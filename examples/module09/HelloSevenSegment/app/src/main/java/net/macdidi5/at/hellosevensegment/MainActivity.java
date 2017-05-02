package net.macdidi5.at.hellosevensegment;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    // 控制數字顯示的Handler物件
    private Handler numberHandler = new Handler();
    // 連接七段顯示器的GPIO pin
    private final String[] PIN_NAMES =
            {"BCM24", "BCM23", "BCM21", "BCM20", "BCM16", "BCM25", "BCM12"};
    // 連接七段顯示器的GPIO物件
    private Gpio[] gpios = new Gpio[PIN_NAMES.length];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "onCreate");

        // 建立設備管理員服物物件
        PeripheralManagerService service = new PeripheralManagerService();

        try {
            // 建立GPIO物件
            for (int i = 0; i < PIN_NAMES.length; i++) {
                gpios[i] = service.openGpio(PIN_NAMES[i]);
                // 設定GPIO為輸出模式，預設為低電壓
                gpios[i].setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            }

            // 啟動控制數字顯示的Handler物件
            numberHandler.post(numberRunnable);
        }
        catch (IOException e) {
            Log.e(TAG, e.toString());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.i(TAG, "onDestroy");

        // 移除控制數字顯示的Handler物件
        numberHandler.removeCallbacks(numberRunnable);

        try {
            // 關閉GPIO物件
            for (Gpio gpio : gpios) {
                gpio.close();
            }
        }
        catch (IOException e) {
            Log.e(TAG, e.toString());
        }
        finally {
            gpios = null;
        }
    }

    // 顯示參數指定的數字
    private void displayNumber(int number) throws IOException {
        if (number < 0 || number > 9) {
            return;
        }

        for (int i = 0; i < gpios.length; i++) {
            // 根據共陰極對照表設定針腳的狀態輸出電壓
            boolean status = DisplayMap.COMMON_CATHODE[number][i] == 1;
            gpios[i].setValue(status);
        }
    }

    // 控制數字顯示的Runnable物件
    private Runnable numberRunnable = new Runnable() {
        int number = 0;

        @Override
        public void run() {
            try {
                displayNumber(number++);

                if (number > 9) {
                    number = 0;
                }

                numberHandler.postDelayed(numberRunnable, 500);
            }
            catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }
    };


}
