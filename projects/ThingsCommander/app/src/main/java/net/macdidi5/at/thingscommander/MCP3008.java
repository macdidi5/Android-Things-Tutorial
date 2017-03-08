package net.macdidi5.at.thingscommander;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;

public class MCP3008 {

    /**
     * MCP3008的八個輸入埠
     */
    public enum Channel {

        CH_00(0), CH_01(1), CH_02(2), CH_03(3),
        CH_04(4), CH_05(5), CH_06(6), CH_07(7);

        private int channel;

        private Channel(int channel) {
            this.channel = channel;
        }

        public int getChannel() {
            return channel;
        }

    }

    // Serial data out
    private Gpio serialDataOutput;

    // Serial data in、Serial clock、Chip select
    private Gpio serialDataInput;
    private Gpio serialClock;
    private Gpio chipSelect;

    public MCP3008(PeripheralManagerService service,
                   String sc, String sdo, String sdi, String cs)
            throws IOException {
        configGPIO(service, sc, sdo, sdi, cs);
    }

    private void configGPIO(PeripheralManagerService service,
                            String sc, String sdo, String sdi, String cs)
            throws IOException {
        serialDataOutput = service.openGpio(sdo);
        serialDataOutput.setDirection(Gpio.DIRECTION_IN);

        serialDataInput = service.openGpio(sdi);
        serialDataInput.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        serialClock = service.openGpio(sc);
        serialClock.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        chipSelect = service.openGpio(cs);
        chipSelect.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
    }

    public void close() throws IOException {
        serialDataOutput.close();
        serialDataInput.close();
        serialClock.close();
        chipSelect.close();
    }

    /**
     * 讀取指定輸入埠的資料
     *
     * @param channel 輸入埠
     * @return 讀取的資料
     */
    public int read(int channel) throws IOException {
        chipSelect.setValue(true);
        serialClock.setValue(false);
        chipSelect.setValue(false);

        int adccommand = channel;

        // 0x18 => 00011000
        adccommand |= 0x18;

        adccommand <<= 3;

        // 傳送讀取的輸入埠給MCP3008
        for (int i = 0; i < 5; i++) {
            // 0x80 => 0&10000000
            if ((adccommand & 0x80) != 0x0) {
                serialDataInput.setValue(true);
            }
            else {
                serialDataInput.setValue(false);
            }

            adccommand <<= 1;

            tickPin(serialClock);
        }

        int adcOut = 0;

        // 讀取指定輸入埠的資料
        for (int i = 0; i < 12; i++) {
            tickPin(serialClock);
            adcOut <<= 1;

            if (serialDataOutput.getValue()) {
                adcOut |= 0x1;
            }
        }

        chipSelect.setValue(true);

        // 移除第一個位元
        adcOut >>= 1;

        return adcOut;
    }

    /**
     * 讀取指定輸入埠的資料
     *
     * @param channel 輸入埠
     * @return 讀取的資料
     */
    public int read(Channel channel) throws IOException {
        return read(channel.getChannel());
    }

    /**
     * 讀取指定輸入埠的資料
     *
     * @param channel 輸入埠
     * @param input 輸入電壓
     * @return 讀取的資料（電壓）
     */
    public float readVoltage(int channel, float input) throws IOException {
        return read(channel) * input / 1023;
    }

    /**
     * 讀取指定輸入埠的資料
     *
     * @param channel 輸入埠
     * @param input 輸入電壓
     * @return 讀取的資料（電壓）
     */
    public float readVoltage(Channel channel, float input) throws IOException {
        return readVoltage(channel.getChannel(), input);
    }

    private void tickPin(Gpio pin) throws IOException {
        pin.setValue(true);
        pin.setValue(false);
    }

}
