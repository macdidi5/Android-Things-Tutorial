package net.macdidi5.at.thingscommanderapp;

public class Device {
    
    // 代號, 名稱, 值, 遞增, 精確度, 通道
    private String id;
    private String name;
    private float value;
    private boolean isIncrememt;
    private int accuracy;
    private int channel;

    public Device() {
        this("", "", 0.0F, false, 0, 0);
    }

    public Device(String id, String name, float value, 
                  boolean isIncrememt, int accuracy, int channel) {
        this.id = id;
        this.name = name;
        this.value = value;
        this.isIncrememt = isIncrememt;
        this.accuracy = accuracy;
        this.channel = channel;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public boolean isIsIncrememt() {
        return isIncrememt;
    }

    public void setIsIncrememt(boolean isIncrememt) {
        this.isIncrememt = isIncrememt;
    }

    public int getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(int accuracy) {
        this.accuracy = accuracy;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    @Override
    public String toString() {
        return "Device{" + "id=" + id + ", name=" + name + 
               ", value=" + value + ", isIncrememt=" + isIncrememt + 
               ", accuracy=" + accuracy + ", channel=" + channel + '}';
    }
    
}
