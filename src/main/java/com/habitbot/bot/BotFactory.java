package com.habitbot.bot;
 
import com.habitbot.repository.InMemoryHabitRepository;
 
/**
 * Решает, какого бота запускать. Main про конкретных ботов ничего не знает.
 * Тип бота задаётся переменной окружения BOT_TYPE:
 *   telegram - Telegram-бот (токен он проверяет сам),
 *   console  - консольный бот (по умолчанию, если переменная не задана).
 */
public class BotFactory {
 
    public static Bot create() {
        String type = System.getenv("BOT_TYPE");
 
        if (type == null || type.isBlank() || type.equalsIgnoreCase("console")) {
            return new ConsoleBot(new InMemoryHabitRepository());
        }
        if (type.equalsIgnoreCase("telegram")) {
            return new TelegramBot();
        }
        throw new IllegalArgumentException(
                "Неизвестный BOT_TYPE: " + type + ". Допустимо: console, telegram");
    }
}
 