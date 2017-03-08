package net.macdidi5.at.thingscommander;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GpioContainer {

    private PeripheralManagerService service;
    private final Map<PiGPIO, Gpio> pins;
    private final List<String> portList;

    public GpioContainer(PeripheralManagerService service) {
        this.service = service;
        pins = new HashMap<>();
        portList = service.getGpioList();
    }

    // GPIO 物件是否已經存在
    public boolean isExist(PiGPIO pin) {
        Gpio result = pins.get(pin);
        return !(result == null);
    }

    // 建立或取得保存中的 GPIO 物件
    public Gpio getPin(PiGPIO pin) throws IOException {
        if (portList.indexOf(pin.name()) == -1) {
            return null;
        }

        // 取得保存中的 GPIO 物件
        Gpio result = pins.get(pin);

        // 如果不存在
        if (result == null) {
            result = service.openGpio(pin.name());
            result.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            pins.put(pin, result);
        }

        return result;
    }

}
