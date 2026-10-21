package com.habitbot;

import com.habitbot.bot.Bot;
import com.habitbot.bot.ConsoleBot;
import com.habitbot.bot.TelegramBot;
import com.habitbot.repository.HabitRepository;
import com.habitbot.repository.InMemoryHabitRepository;
import com.habitbot.service.HabitReminder;

public class Main {
    public static void main(String[] args) {
        // Если задана переменная окружения BOT_TOKEN, запускаем Telegram-бота,
        // иначе работает консольная версия (с напоминаниями)
        String token = System.getenv("BOT_TOKEN");

        Bot bot;
        if (token != null && !token.isBlank()) {
            bot = new TelegramBot(token);
        } else {
            HabitRepository repository = new InMemoryHabitRepository();

            HabitReminder reminder = new HabitReminder(repository);
            reminder.start();

            bot = new ConsoleBot(repository);
        }
        bot.run();
    }
}