package com.habitbot.service;

import com.habitbot.model.Habit;
import com.habitbot.repository.HabitRepository;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Обрабатывает сообщения одного пользователя и возвращает текст ответа.
 * В отличие от консольной версии, не читает ввод и не печатает сам:
 * Telegram присылает по одному сообщению, поэтому класс помнит, на каком
 * шаге диалога находится пользователь (например, ждём название привычки).
 */
public class HabitCommandHandler {

    private static final String COMMANDS =
            "/add, /list, /done, /skip, /today, /category, /stats, /delete, /cancel";

    /** Шаги диалога: чего бот ждёт от пользователя в следующем сообщении. */
    private enum State {
        IDLE,
        ADD_TITLE,
        ADD_COUNT,
        ADD_CATEGORY,
        DONE_INDEX,
        SKIP_INDEX,
        DELETE_INDEX,
        CATEGORY_FILTER
    }

    private final HabitRepository repository;
    private State state = State.IDLE;
    private String pendingTitle;
    private int pendingCount;

    public HabitCommandHandler(HabitRepository repository) {
        this.repository = repository;
    }

    /**
     * Принимает текст сообщения и возвращает ответ бота.
     * synchronized: Telegram может обрабатывать сообщения в нескольких потоках.
     */
    public synchronized String handle(String text) {
        String input = text.trim();

        // Команда в любой момент прерывает текущий диалог и выполняется заново
        if (input.startsWith("/")) {
            state = State.IDLE;
            return handleCommand(normalizeCommand(input));
        }

        switch (state) {
            case ADD_TITLE:
                return onAddTitle(input);
            case ADD_COUNT:
                return onAddCount(input);
            case ADD_CATEGORY:
                return onAddCategory(input);
            case DONE_INDEX:
                return onDone(input);
            case SKIP_INDEX:
                return onSkip(input);
            case DELETE_INDEX:
                return onDelete(input);
            case CATEGORY_FILTER:
                return onCategoryFilter(input);
            default:
                return "Не понимаю. Доступные команды: " + COMMANDS;
        }
    }

    /** "/Add@my_bot" -> "/add" (в группах Telegram дописывает имя бота). */
    private String normalizeCommand(String input) {
        String command = input.split("\\s+")[0].toLowerCase();
        int at = command.indexOf('@');
        if (at > 0) {
            command = command.substring(0, at);
        }
        return command;
    }

    private String handleCommand(String command) {
        switch (command) {
            case "/start":
                return "Привет! Я трекер привычек.\nДоступные команды: " + COMMANDS;
            case "/help":
                return "Доступные команды: " + COMMANDS;
            case "/cancel":
                return "Действие отменено.";
            case "/list":
                return list();
            case "/add":
                state = State.ADD_TITLE;
                return "Введите название привычки:";
            case "/done":
                return askIndex("Какую привычку отметить?", State.DONE_INDEX, false, "Список пуст.");
            case "/skip":
                return askIndex("Какую привычку пропустить сегодня?", State.SKIP_INDEX, true, "Список пуст.");
            case "/today":
                return today();
            case "/category":
                return askCategory();
            case "/stats":
                return stats();
            case "/delete":
                return askIndex("Какую привычку удалить?", State.DELETE_INDEX, false, "Список пуст. Нечего удалять.");
            default:
                return "Неизвестная команда. Доступно: " + COMMANDS;
        }
    }

    // ---------- команды без диалога ----------

    private String list() {
        if (repository.isEmpty()) {
            return "Список пуст. Добавьте привычку через /add";
        }
        StringBuilder sb = new StringBuilder("--- Ваши привычки ---");
        for (int i = 0; i < repository.size(); i++) {
            Habit h = repository.getHabit(i);
            sb.append("\n").append(i + 1).append(". ").append(h.getTitle())
                    .append(" [").append(h.getCurrentCount()).append("/").append(h.getTargetCount()).append("]")
                    .append(" (").append(h.getCategory()).append(")");
        }
        return sb.toString();
    }

    private String today() {
        if (repository.isEmpty()) {
            return "Список пуст. Добавьте привычку через /add";
        }
        StringBuilder sb = new StringBuilder("--- План на сегодня ---");
        int done = 0;
        for (int i = 0; i < repository.size(); i++) {
            Habit h = repository.getHabit(i);
            sb.append("\n").append(i + 1).append(". ").append(h.getTitle())
                    .append(" [").append(h.getCurrentCount()).append("/").append(h.getTargetCount()).append("] ")
                    .append(h.getStatus());
            if (h.isCompleted()) {
                done++;
            }
        }
        sb.append("\nВыполнено: ").append(done).append(" из ").append(repository.size());
        return sb.toString();
    }

    private String stats() {
        if (repository.isEmpty()) {
            return "Список пуст. Нет данных для статистики.";
        }
        int total = repository.size();
        int completed = 0;
        for (int i = 0; i < total; i++) {
            if (repository.getHabit(i).isCompleted()) {
                completed++;
            }
        }
        int percent = (int) (((double) completed / total) * 100);
        return "--- Статистика за сегодня ---\n"
                + "Всего привычек: " + total + "\n"
                + "Выполнено полностью: " + completed + "\n"
                + "Процент выполнения: " + percent + "%";
    }

    // ---------- вопрос и ответ: выбор номера привычки ----------

    private String askIndex(String question, State next, boolean showStatus, String emptyMessage) {
        if (repository.isEmpty()) {
            return emptyMessage;
        }
        StringBuilder sb = new StringBuilder(question);
        for (int i = 0; i < repository.size(); i++) {
            Habit h = repository.getHabit(i);
            sb.append("\n").append(i + 1).append(". ").append(h.getTitle());
            if (showStatus) {
                sb.append(" ").append(h.getStatus());
            }
        }
        sb.append("\nВведите номер (или /cancel для отмены):");
        state = next;
        return sb.toString();
    }

    /** Возвращает номер привычки (с нуля) или -1, если ввод некорректный. */
    private int parseIndex(String input) {
        try {
            int number = Integer.parseInt(input);
            if (number >= 1 && number <= repository.size()) {
                return number - 1;
            }
        } catch (NumberFormatException e) {
            // не число — вернём -1
        }
        return -1;
    }

    private String onDone(String input) {
        int index = parseIndex(input);
        if (index < 0) {
            return "Ошибка: неверный номер. Введите число из списка или /cancel.";
        }
        state = State.IDLE;
        Habit selected = repository.getHabit(index);
        selected.increaseCount();
        String reply = "Отметка добавлена: " + selected.getTitle()
                + " (" + selected.getCurrentCount() + "/" + selected.getTargetCount() + ")";
        if (selected.isCompleted()) {
            reply += "\nЦель на день достигнута.";
        }
        return reply;
    }

    private String onSkip(String input) {
        int index = parseIndex(input);
        if (index < 0) {
            return "Ошибка: неверный номер. Введите число из списка или /cancel.";
        }
        state = State.IDLE;
        Habit toSkip = repository.getHabit(index);
        if (toSkip.isCompleted()) {
            return "Привычка '" + toSkip.getTitle() + "' уже выполнена, пропускать нечего.";
        }
        toSkip.skip();
        return "Привычка '" + toSkip.getTitle() + "' отмечена как пропущенная ❌";
    }

    private String onDelete(String input) {
        int index = parseIndex(input);
        if (index < 0) {
            return "Ошибка: неверный номер. Введите число из списка или /cancel.";
        }
        state = State.IDLE;
        String title = repository.getHabit(index).getTitle();
        repository.removeHabit(index);
        return "Привычка '" + title + "' удалена.";
    }

    // ---------- добавление привычки: три шага ----------

    private String onAddTitle(String input) {
        if (input.isEmpty()) {
            return "Название не может быть пустым. Введите название привычки:";
        }
        pendingTitle = input;
        state = State.ADD_COUNT;
        return "Сколько раз в день выполнять?";
    }

    private String onAddCount(String input) {
        try {
            int count = Integer.parseInt(input);
            if (count >= 1) {
                pendingCount = count;
                state = State.ADD_CATEGORY;
                return "Введите категорию (или отправьте - чтобы оставить без категории):";
            }
        } catch (NumberFormatException e) {
            // обработаем ниже
        }
        return "Введите целое число от 1. Сколько раз в день выполнять?";
    }

    private String onAddCategory(String input) {
        // В Telegram нельзя отправить пустое сообщение, поэтому "-" означает "без категории"
        String category = input.equals("-") ? "" : input;
        repository.addHabit(new Habit(pendingTitle, pendingCount, category));
        state = State.IDLE;
        return "Привычка '" + pendingTitle + "' сохранена.";
    }

    // ---------- фильтр по категории ----------

    private String askCategory() {
        if (repository.isEmpty()) {
            return "Список пуст. Добавьте привычку через /add";
        }
        Set<String> categories = new LinkedHashSet<>();
        for (int i = 0; i < repository.size(); i++) {
            categories.add(repository.getHabit(i).getCategory());
        }
        state = State.CATEGORY_FILTER;
        return "Доступные категории: " + String.join(", ", categories)
                + "\nВведите категорию (или /cancel для отмены):";
    }

    private String onCategoryFilter(String input) {
        state = State.IDLE;
        StringBuilder sb = new StringBuilder("--- Категория: " + input + " ---");
        boolean found = false;
        for (int i = 0; i < repository.size(); i++) {
            Habit h = repository.getHabit(i);
            if (h.getCategory().equalsIgnoreCase(input)) {
                sb.append("\n").append(i + 1).append(". ").append(h.getTitle())
                        .append(" [").append(h.getCurrentCount()).append("/").append(h.getTargetCount()).append("] ")
                        .append(h.getStatus());
                found = true;
            }
        }
        if (!found) {
            sb.append("\nВ этой категории привычек нет.");
        }
        return sb.toString();
    }
}