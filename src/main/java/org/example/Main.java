package org.example;

import org.example.db.DatabaseInitializer;
import org.example.service.Bot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {
    public static void main(String[] args) {
        DatabaseInitializer.initialize();
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new Bot());
            System.out.println("bot started");
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}