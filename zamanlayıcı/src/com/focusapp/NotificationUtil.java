package com.focusapp;

import java.awt.*;
import java.awt.image.BufferedImage;

public class NotificationUtil {

    private static TrayIcon trayIcon;
    private static boolean initialized = false;

    private static void initTray() {
        if (initialized) return;
        initialized = true;

        if (!SystemTray.isSupported()) {
            System.out.println("Sistem tepsisi desteklenmiyor.");
            return;
        }

        try {
            SystemTray tray = SystemTray.getSystemTray();

            BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            g.setColor(java.awt.Color.WHITE);
            g.fillOval(2, 2, 12, 12);
            g.dispose();

            trayIcon = new TrayIcon(image, "Focus");
            trayIcon.setImageAutoSize(true);
            tray.add(trayIcon);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showNotification(String title, String message) {
        initTray();

        if (trayIcon != null) {
            trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
        } else {
            System.out.println(title + ": " + message);
        }
    }

    public static void playBeep() {
        try {
            Toolkit.getDefaultToolkit().beep();
            Thread.sleep(120);
            Toolkit.getDefaultToolkit().beep();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}