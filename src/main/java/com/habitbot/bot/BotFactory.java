package com.habitbot.bot;
 
import com.habitbot.repository.InMemoryHabitRepository;
 
/**
 * Решает, какого бота запускать. Main про конкретных ботов ничего не знает.
 * Если задана переменная окружения BOT_TOKEN, создаётся Telegram-бот,
 * иначе консольный.
 */
public class BotFactory {
 
    public static Bot create() {
        String token = System.getenv("BOT_TOKEN");
 
        if (token != null && !token.isBlank()) {
            return new TelegramBot(token);
        }
        return new ConsoleBot(new InMemoryHabitRepository());
    }
}
 