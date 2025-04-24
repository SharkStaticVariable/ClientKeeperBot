package io.project.clientkeeperbot.service;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.User;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class CaptchaService {
    private static final int WIDTH = 200;
    private static final int HEIGHT = 80;
    private static final int CAPTCHA_LENGTH = 6;
    private static final long CAPTCHA_EXPIRE_MINUTES = 5;

    private final Map<Long, CaptchaData> captchaStorage = new HashMap<>();
    private final Map<Long, User> pendingUsers = new HashMap<>();

    public String generateCaptchaImageBase64(Long chatId) throws IOException {
        String captchaText = RandomStringUtils.randomAlphanumeric(CAPTCHA_LENGTH);
        BufferedImage image = createCaptchaImage(captchaText);

        captchaStorage.put(chatId, new CaptchaData(captchaText,
                System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(CAPTCHA_EXPIRE_MINUTES)));

        return convertImageToBase64(image);
    }

    public boolean verifyCaptcha(Long chatId, String userInput) {
        CaptchaData captchaData = captchaStorage.get(chatId);
        if (captchaData == null || captchaData.expirationTime < System.currentTimeMillis()) {
            captchaStorage.remove(chatId);
            pendingUsers.remove(chatId);
            return false;
        }

        boolean isCorrect = captchaData.code.equalsIgnoreCase(userInput);
        if (isCorrect) {
            captchaStorage.remove(chatId);
        }
        return isCorrect;
    }

    public boolean isCaptchaPending(Long chatId) {
        return captchaStorage.containsKey(chatId) &&
                captchaStorage.get(chatId).expirationTime > System.currentTimeMillis();
    }

    public void storeUserData(Long chatId, User telegramUser) {
        pendingUsers.put(chatId, telegramUser);
    }

    public User getPendingUser(Long chatId) {
        return pendingUsers.remove(chatId);
    }

    private BufferedImage createCaptchaImage(String text) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();

        // Фон
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, WIDTH, HEIGHT);

        // Шум
        graphics.setColor(Color.LIGHT_GRAY);
        for (int i = 0; i < 20; i++) {
            int x1 = (int) (Math.random() * WIDTH);
            int y1 = (int) (Math.random() * HEIGHT);
            int x2 = (int) (Math.random() * WIDTH);
            int y2 = (int) (Math.random() * HEIGHT);
            graphics.drawLine(x1, y1, x2, y2);
        }

        // Текст
        graphics.setFont(new Font("Arial", Font.BOLD, 36));
        for (int i = 0; i < text.length(); i++) {
            graphics.setColor(new Color(
                    (int) (Math.random() * 100) + 50,
                    (int) (Math.random() * 100) + 50,
                    (int) (Math.random() * 100) + 50));

            int x = 30 + i * 30;
            int y = 50 + (int) (Math.random() * 20 - 10);
            graphics.drawString(String.valueOf(text.charAt(i)), x, y);
        }

        graphics.dispose();
        return image;
    }

    private String convertImageToBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    private static class CaptchaData {
        String code;
        long expirationTime;

        CaptchaData(String code, long expirationTime) {
            this.code = code;
            this.expirationTime = expirationTime;
        }
    }
}