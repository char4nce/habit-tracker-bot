package com.habitbot.bot;

import com.habitbot.repository.PostgresHabitRepository;
import com.habitbot.service.BotReply;
import com.habitbot.service.HabitCommandHandler;
import org.hibernate.SessionFactory;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.util.DefaultLongPollingUpdateConsumer;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.message.MaybeInaccessibleMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TelegramBot extends DefaultLongPollingUpdateConsumer implements Bot {

    // Подписи четырёх кнопок под полем ввода.
    // Плюс в "＋ Добавить" - широкий текстовый символ (не эмодзи), чтобы он был виден на тёмном фоне.
    private static final String BUTTON_TODAY = "📅 Сегодня";
    private static final String BUTTON_LIST = "📋 Все привычки";
    private static final String BUTTON_ADD = "＋ Добавить";
    private static final String BUTTON_CATEGORY = "📁 Категории";

    // Нажатие кнопки приходит боту как обычный текст с подписью кнопки.
    // Эта таблица говорит, какой команде соответствует каждая подпись.
    private static final Map<String, String> BUTTON_COMMANDS = Map.of(
            BUTTON_TODAY, "/today",
            BUTTON_LIST, "/list",
            BUTTON_ADD, "/add",
            BUTTON_CATEGORY, "/category");

    private final String token;
    private final TelegramClient client;
    private final HabitCommandHandler handler;
    private final ReplyKeyboardMarkup bottomMenu = buildBottomMenu();

    public TelegramBot(SessionFactory sessionFactory) {
        this.token = System.getenv("BOT_TOKEN");
        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                    "Не задан токен Telegram-бота: установите переменную BOT_TOKEN");
        }
        this.client = new OkHttpTelegramClient(token);
        PostgresHabitRepository repository = new PostgresHabitRepository(sessionFactory);
        this.handler = new HabitCommandHandler(repository);
    }

    @Override
    public void run() {
        try {
            new TelegramBotsLongPollingApplication().registerBot(token, this);
            setCommandMenu();
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

    /** Список команд, который открывается кнопкой «Меню» рядом с полем ввода. */
    private void setCommandMenu() {
        List<BotCommand> commands = List.of(
                command("start", "Начать работу"),
                command("today", "План на сегодня"),
                command("list", "Все привычки"),
                command("add", "Добавить привычку"),
                command("done", "Отметить выполнение"),
                command("skip", "Пропустить на сегодня"),
                command("category", "Привычки по категориям"),
                command("stats", "Статистика за день"),
                command("delete", "Удалить привычку"),
                command("cancel", "Отменить действие"));
        try {
            client.execute(SetMyCommands.builder().commands(commands).build());
        } catch (TelegramApiException e) {
            // Меню команд - украшение: если не задалось, бот всё равно работает
            System.out.println("Не удалось задать меню команд: " + e.getMessage());
        }
    }

    /** Одна команда меню: имя (без слэша) и короткое описание. */
    private static BotCommand command(String name, String description) {
        return BotCommand.builder().command(name).description(description).build();
    }

    /** Четыре кнопки под полем ввода: две строки по две кнопки. */
    private static ReplyKeyboardMarkup buildBottomMenu() {
        return ReplyKeyboardMarkup.builder()
                .keyboard(List.of(
                        new KeyboardRow(BUTTON_TODAY, BUTTON_LIST),
                        new KeyboardRow(BUTTON_ADD, BUTTON_CATEGORY)))
                .resizeKeyboard(true)  
                .isPersistent(true)    
                .build();
    }

    @Override
    public void consume(Update update) {
        if (update.hasCallbackQuery()) {
            onButtonPressed(update.getCallbackQuery());
            return;
        }
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }
        long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();

        // Если нажата кнопка под полем ввода, подменяем её подпись на команду
        String command = BUTTON_COMMANDS.get(text);
        if (command != null) {
            text = command;
        }

        BotReply reply = handler.handle(chatId, text);

        // У сообщения может быть только один набор кнопок: если есть кнопки под сообщением,
        // отправляем их, иначе прикладываем кнопки под полем ввода, чтобы они были всегда
        ReplyKeyboard keyboard = reply.buttons().isEmpty() ? bottomMenu : inlineKeyboard(reply);

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(reply.text())
                .replyMarkup(keyboard)
                .build();
        try {
            client.execute(message);
        } catch (TelegramApiException e) {
            System.out.println("Не удалось отправить сообщение: " + e.getMessage());
        }
    }

    /** Нажата кнопка под сообщением: меняем это же сообщение на новое содержимое. */
    private void onButtonPressed(CallbackQuery query) {
        // Подтверждаем Telegram, что нажатие кнопки получено
        try {
            client.execute(AnswerCallbackQuery.builder().callbackQueryId(query.getId()).build());
        } catch (TelegramApiException e) {
            System.out.println("Не удалось ответить на нажатие кнопки: " + e.getMessage());
        }

        MaybeInaccessibleMessage pressed = query.getMessage();
        if (pressed == null || query.getData() == null) {
            return;
        }
        long chatId = pressed.getChatId();

        BotReply reply = handler.handleButton(chatId, query.getData());
        if (reply == null) {
            return;   // нажатие нужно проигнорировать (например, повторное)
        }

        EditMessageText edit = EditMessageText.builder()
                .chatId(chatId)
                .messageId(pressed.getMessageId())
                .text(reply.text())
                // Если кнопок нет, поле не указываем: Telegram уберёт старые кнопки
                .replyMarkup(reply.buttons().isEmpty() ? null : inlineKeyboard(reply))
                .build();
        try {
            client.execute(edit);
        } catch (TelegramApiException e) {
            // Например, "message is not modified", если новый текст не отличается от старого
            System.out.println("Не удалось изменить сообщение: " + e.getMessage());
        }
    }

    /** Превращает кнопки из ответа обработчика в кнопки Telegram под сообщением. */
    private static InlineKeyboardMarkup inlineKeyboard(BotReply reply) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (List<BotReply.Button> line : reply.buttons()) {
            InlineKeyboardRow row = new InlineKeyboardRow();
            for (BotReply.Button button : line) {
                row.add(InlineKeyboardButton.builder()
                        .text(button.label())
                        .callbackData(button.data())
                        .build());
            }
            rows.add(row);
        }
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }
}