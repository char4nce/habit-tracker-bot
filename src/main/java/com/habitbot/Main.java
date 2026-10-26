package com.habitbot;

import com.habitbot.bot.Bot;
import com.habitbot.bot.ConsoleBot;
import com.habitbot.bot.TelegramBot;
import com.habitbot.repository.HabitRepository;
import com.habitbot.repository.InMemoryHabitRepository;

public class Main {
    public static void main(String[] args) {
        String token = System.getenv("BOT_TOKEN");

        Bot bot;

        if (token != null && !token.isBlank()) {
            bot = new TelegramBot(token);
        } else {
            HabitRepository repository = new InMemoryHabitRepository();
            bot = new ConsoleBot(repository);
        }
        bot.run();
    }
}