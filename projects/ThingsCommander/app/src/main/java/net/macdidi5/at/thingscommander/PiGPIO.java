package net.macdidi5.at.thingscommander;

public enum PiGPIO {

    BCM4, BCM12, BCM16,
    BCM17, BCM20, BCM21,BCM22,
    BCM23, BCM24, BCM25, BCM27;

//    BCM4, BCM5, BCM6, BCM12, BCM16,
//    BCM17, BCM19, BCM20, BCM21,BCM22,
//    BCM23, BCM24, BCM25, BCM26, BCM27;


    public static PiGPIO getPiGPIO(String pinName) {
        final PiGPIO[] ps = values();

        for (PiGPIO p : ps) {
            if (p.name().equals(pinName)) {
                return p;
            }
        }

        return null;
    }

}
