package com.artemchep.basics_multithreading.cipher;

public class CipherUtil {

    public static long WORK_MILLIS = 500L;
    private volatile static long time;

    public static String encrypt(String plainText) {
        // Simulates the real struggle of encryption.
        try {
            time = WORK_MILLIS * (int) (Math.random() * 6);
            Thread.sleep(time);

        } catch (InterruptedException ignored) {
        }

        return String.valueOf(plainText.hashCode()); // yes, this is not a real encryption method
    }

    public static long getTime(){
        return time;
    }
}
