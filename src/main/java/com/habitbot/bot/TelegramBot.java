package com.habitbot.bot;
 
import com.habitbot.repository.InMemoryHabitRepository;
import com.habitbot.service.HabitCommandHandler;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.util.DefaultLongPollingUpdateConsumer;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
 
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
 
public class TelegramBot extends DefaultLongPollingUpdateConsumer implements Bot {
 
    private final String token;
    private final TelegramClient client;
    private final Map<Long, HabitCommandHandler> handlers = new ConcurrentHashMap<>();
 
    public TelegramBot(String token) {
        this.token = token;
        this.client = new OkHttpTelegramClient(token);
    }
 
    @Override
    public void run() {
        try {
            new TelegramBotsLongPollingApplication().registerBot(token, this);
            System.out.println("Telegram-бот запущен. Напишите ему в Telegram. Остановить: Ctrl+C.");
        } catch (TelegramApiException e) {
            System.out.println("Не удалось запустить бота: " + e.getMessage());
            Throwable cause = e.getCause();
            while (cause != null) {
                System.out.println("Причина: " + cause);
                cause = cause.getCause();
            }
        }
    }
 
    @Override
    public void consume(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }
        long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();
 
        // Для нового чата создаём свой обработчик и свой список привычек (один раз),
        // для известного чата берём уже созданный
        HabitCommandHandler handler = handlers.computeIfAbsent(
                chatId, id -> new HabitCommandHandler(new InMemoryHabitRepository()));
        String reply = handler.handle(text);
 
        try {
            client.execute(SendMessage.builder().chatId(chatId).text(reply).build());
        } catch (TelegramApiException e) {
            System.out.println("Не удалось отправить сообщение: " + e.getMessage());
        }
    }
}