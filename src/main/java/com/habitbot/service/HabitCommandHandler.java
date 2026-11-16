package com.habitbot.service;

import com.habitbot.model.Habit;
import com.habitbot.repository.HabitRepository;
import com.habitbot.service.BotReply.Button;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HabitCommandHandler {

    private static final String COMMANDS =
            "/add, /list, /done, /skip, /today, /category, /stats, /delete, /cancel";

    // Данные кнопок под сообщениями. Если после двоеточия идёт число, это номер привычки в базе
    private static final String DONE_PREFIX = "done:";   // "✅ название": отметить выполнение
    private static final String SKIP_MENU = "skip";      // "⏭ Пропустить": открыть выбор привычки
    private static final String SKIP_PREFIX = "skip:";   // "⏭ название": пропустить эту привычку
    private static final String STATS = "stats";         // "📊 Статистика"
    private static final String TODAY = "today";         // "← Назад", "← К плану на сегодня"
    private static final String LIST = "list";           // "← Назад" из выбора удаления: вернуться к списку
    private static final String ADD = "add";             // "＋ Добавить": начать добавление привычки
    private static final String DELETE_MENU = "del";     // "🗑 Удалить": открыть выбор привычки
    private static final String DELETE_PREFIX = "del:";  // "название": удалить эту привычку
    private static final String CATS = "cats";           // "← К категориям": экран со списком категорий
    private static final String CAT_PREFIX = "cat:";     // "Спорт · 1": открыть категорию 
    private static final String CAT_DONE_PREFIX = "cdone:"; // "✅ название" внутри категории 
    private static final String VIEW_ALL = "all";
    private static final String NO_CATEGORY = "Без категории";

    // Добавление привычки
    private static final String COUNT_PREFIX = "cnt:";          // "1".."5": сколько раз в день 
    private static final String ADD_CAT_PREFIX = "addcat:";     // выбор категории
    private static final String ADD_CAT_NONE = "none";          // "Без категории"
    private static final String ADD_CAT_NEW = "new";            // "✏️ Новая": ввести название категории текстом
    private static final String ADD_CANCEL = "addcancel";       // "✖ Отмена"
    private static final int MAX_TITLE_LENGTH = 100;            // длиннее название не помещается в базу и в кнопки
    private static final int MAX_CATEGORY_LENGTH = 50;
    private static final int MAX_COUNT = 99;

    private static final String[] MONTHS = {"января", "февраля", "марта", "апреля", "мая", "июня",
            "июля", "августа", "сентября", "октября", "ноября", "декабря"};

    private enum State {
        IDLE, ADD_TITLE, ADD_COUNT, ADD_CATEGORY, ADD_NEW_CATEGORY, DONE_INDEX, SKIP_INDEX, DELETE_INDEX
    }

    private class UserContext {
        State state = State.IDLE;
        String pendingTitle;
        int pendingCount;
        List<String> offeredCategories = new ArrayList<>();   // категории, показанные кнопками при добавлении
        List<Long> lastShownHabitIds = new ArrayList<>();
    }

    private final HabitRepository repository;
    private final Map<Long, UserContext> contexts = new ConcurrentHashMap<>();

    public HabitCommandHandler(HabitRepository repository) {
        this.repository = repository;
    }

    private UserContext getContext(Long chatId) {
        return contexts.computeIfAbsent(chatId, k -> new UserContext());
    }

    public BotReply handle(Long chatId, String text) {
        String input = text.trim();
        UserContext ctx = getContext(chatId);

        synchronized (ctx) {
            if (input.startsWith("/")) {
                ctx.state = State.IDLE;
                String command = normalizeCommand(input);
                // Экраны с кнопками под сообщением; остальные ответы обычным текстом
                if (command.equals("/today")) {
                    return today(chatId);
                }
                if (command.equals("/stats")) {
                    return statsScreen(chatId);
                }
                if (command.equals("/list")) {
                    return listScreen(chatId, null);
                }
                if (command.equals("/category")) {
                    return categoriesScreen(chatId);
                }
                if (command.equals("/add")) {
                    return startAdd(ctx);
                }
                return BotReply.of(handleCommand(chatId, ctx, command));
            }

            return handleDialog(chatId, ctx, input);
        }
    }

    /**
     Нажатие кнопки под сообщением. Возвращает новое содержимое того же сообщения:
     бот не шлёт новое, а меняет старое на месте.
     Возвращает null, если нажатие нужно проигнорировать.
     */
    public BotReply handleButton(Long chatId, String data) {
        UserContext ctx = getContext(chatId);

        synchronized (ctx) {
            if (data.startsWith(DONE_PREFIX)) {
                return onDoneButton(chatId, data.substring(DONE_PREFIX.length()));
            }
            if (data.equals(SKIP_MENU)) {
                return skipMenu(chatId);
            }
            if (data.startsWith(SKIP_PREFIX)) {
                return onSkipButton(chatId, data.substring(SKIP_PREFIX.length()));
            }
            if (data.equals(STATS)) {
                return statsScreen(chatId);
            }
            if (data.equals(TODAY)) {
                return today(chatId);
            }
            if (data.equals(LIST)) {
                return listScreen(chatId, null);
            }
            if (data.equals(ADD)) {
                return startAdd(ctx);
            }
            if (data.startsWith(COUNT_PREFIX)) {
                return onCountButton(chatId, ctx, data.substring(COUNT_PREFIX.length()));
            }
            if (data.startsWith(ADD_CAT_PREFIX)) {
                return onAddCategoryButton(chatId, ctx, data.substring(ADD_CAT_PREFIX.length()));
            }
            if (data.equals(ADD_CANCEL)) {
                return onAddCancel(ctx);
            }
            if (data.equals(DELETE_MENU)) {
                return deleteMenu(chatId);
            }
            if (data.startsWith(DELETE_PREFIX)) {
                return onDeleteButton(chatId, data.substring(DELETE_PREFIX.length()));
            }
            if (data.equals(CATS)) {
                return categoriesScreen(chatId);
            }
            if (data.startsWith(CAT_PREFIX)) {
                return categoryView(chatId, data.substring(CAT_PREFIX.length()));
            }
            if (data.startsWith(CAT_DONE_PREFIX)) {
                return onCategoryDoneButton(chatId, data.substring(CAT_DONE_PREFIX.length()));
            }
            return BotReply.of("Эта кнопка устарела. Нажмите «📅 Сегодня».");
        }
    }

    /** Ответ на ввод, когда бот ждёт от пользователя следующий шаг диалога. */
    private BotReply handleDialog(Long chatId, UserContext ctx, String input) {
        switch (ctx.state) {
            case ADD_TITLE: return onAddTitle(ctx, input);
            case ADD_COUNT: return onAddCountTyped(chatId, ctx, input);
            case ADD_CATEGORY:
            case ADD_NEW_CATEGORY: return onAddCategoryTyped(chatId, ctx, input);
            case DONE_INDEX: return BotReply.of(onDone(chatId, ctx, input));
            case SKIP_INDEX: return BotReply.of(onSkip(chatId, ctx, input));
            case DELETE_INDEX: return BotReply.of(onDelete(chatId, ctx, input));
            default: return BotReply.of("Не понимаю. Доступные команды: " + COMMANDS);
        }
    }

    /** Приветствие: что умеет бот и с чего начать */
    private String start() {
        return "👋 Привет! Я трекер привычек.\n\n"
                + "Помогу выработать полезные привычки и не забывать о них:\n\n"
                + "📅 смотри, что нужно сделать сегодня\n"
                + "✅ отмечай выполнение\n"
                + "📊 следи за своим прогрессом\n\n"
                + "Чтобы начать, добавь первую привычку: /add\n"
                + "Все команды в меню слева от поля ввода.";
    }

    private String normalizeCommand(String input) {
        String command = input.split("\\s+")[0].toLowerCase();
        int at = command.indexOf('@');
        if (at > 0) {
            command = command.substring(0, at);
        }
        return command;
    }

    private String handleCommand(Long chatId, UserContext ctx, String command) {
        switch (command) {
            case "/start": return start();
            case "/help": return "Доступные команды: " + COMMANDS;
            case "/cancel": return "Действие отменено.";
            case "/done":
                return askIndex(chatId, ctx, "Какую привычку отметить?", State.DONE_INDEX, false, "Список пуст.");
            case "/skip":
                return askIndex(chatId, ctx, "Какую привычку пропустить сегодня?", State.SKIP_INDEX, true, "Список пуст.");
            case "/delete":
                return askIndex(chatId, ctx, "Какую привычку удалить?", State.DELETE_INDEX, false, "Список пуст. Нечего удалять.");
            default: return "Неизвестная команда. Доступно: " + COMMANDS;
        }
    }

    /**
     * Экран "Все привычки": название и категория каждой привычки, внизу кнопки "Добавить" и "Удалить".
     *
     * @param note короткая пометка внизу или null
     */
    private BotReply listScreen(Long chatId, String note) {
        List<Habit> habits = repository.getAllHabits(chatId);

        StringBuilder sb = new StringBuilder();
        if (habits.isEmpty()) {
            sb.append("Список пуст. Добавьте привычку через /add");
        } else {
            sb.append("📋 Все привычки\n");
            for (int i = 0; i < habits.size(); i++) {
                Habit h = habits.get(i);
                sb.append("\n").append(i + 1).append(". ").append(h.getTitle())
                        .append(" · ").append(h.getCategory());
            }
        }
        if (note != null) {
            sb.append("\n\n").append(note);
        }

        Button add = new Button("＋ Добавить", ADD);
        List<Button> line = habits.isEmpty() ? List.of(add) : List.of(add, new Button("🗑 Удалить", DELETE_MENU));
        return new BotReply(sb.toString(), List.of(line));
    }

    /** Нажали "🗑 Удалить": выбор привычки кнопкой. Подтверждения нет. */
    private BotReply deleteMenu(Long chatId) {
        List<Habit> habits = repository.getAllHabits(chatId);
        if (habits.isEmpty()) return listScreen(chatId, null);

        List<Button> buttons = new ArrayList<>();
        for (Habit h : habits) {
            buttons.add(new Button(h.getTitle(), DELETE_PREFIX + h.getId()));
        }
        List<List<Button>> rows = inRows(buttons, 2);
        rows.add(List.of(new Button("← Назад", LIST)));
        return new BotReply("Какую привычку удалить?", rows);
    }

    /** Выбрали привычку для удаления: удаляем сразу и показываем обновлённый список. */
    private BotReply onDeleteButton(Long chatId, String habitIdText) {
        String note = null;
        try {
            Habit habit = repository.getHabit(chatId, Long.parseLong(habitIdText));
            // Если привычки уже нет (кнопку нажали повторно), просто покажем список
            if (habit != null) {
                repository.removeHabit(chatId, habit.getId());
                note = "🗑 «" + habit.getTitle() + "» удалена";
            }
        } catch (NumberFormatException ignored) {
            // Непонятные данные кнопки: просто покажем список
        }
        return listScreen(chatId, note);
    }

    /** Экран "Сегодня": статус каждой привычки, счёт выполненных, кнопки "✅" для невыполненных и ряд "Пропустить / Статистика". */
    private BotReply today(Long chatId) {
        List<Habit> habits = repository.getAllHabits(chatId);
        if (habits.isEmpty()) return BotReply.of("Список пуст. Добавьте привычку через /add");

        LocalDate date = LocalDate.now();
        StringBuilder sb = new StringBuilder("📅 Сегодня, ")
                .append(date.getDayOfMonth()).append(" ").append(MONTHS[date.getMonthValue() - 1]).append("\n");

        int done = 0;
        List<Button> doneButtons = new ArrayList<>();
        for (Habit h : habits) {
            sb.append("\n").append(statusIcon(h)).append(" ").append(h.getTitle())
                    .append(" — ").append(h.getCurrentCount()).append("/").append(h.getTargetCount());
            if (h.isCompleted()) {
                done++;
            } else {
                doneButtons.add(new Button("✅ " + h.getTitle(), DONE_PREFIX + h.getId()));
            }
        }
        sb.append("\n\nВыполнено: ").append(done).append(" из ").append(habits.size());

        List<List<Button>> rows = inRows(doneButtons, 2);
        rows.add(List.of(new Button("⏭ Пропустить", SKIP_MENU), new Button("📊 Статистика", STATS)));
        return new BotReply(sb.toString(), rows);
    }

    /** Значок статуса привычки для строки в списке. */
    private String statusIcon(Habit h) {
        if (h.isCompleted()) return "✅";
        if (h.isSkipped()) return "❌";
        if (h.getCurrentCount() > 0) return "⏳";
        return "⬜";
    }

    /** Раскладывает кнопки по строкам: в каждой строке не больше perRow кнопок. */
    private List<List<Button>> inRows(List<Button> buttons, int perRow) {
        List<List<Button>> rows = new ArrayList<>();
        for (int i = 0; i < buttons.size(); i += perRow) {
            rows.add(buttons.subList(i, Math.min(i + perRow, buttons.size())));
        }
        return rows;
    }

    /** Добавляет отметку выполнения привычке. Если привычки нет или цель уже выполнена, ничего не делает. */
    private void markDone(Long chatId, String habitIdText) {
        try {
            Habit habit = repository.getHabit(chatId, Long.parseLong(habitIdText));
            // Если привычку уже удалили или цель выполнена (старая кнопка), ничего не добавляем
            if (habit != null && !habit.isCompleted()) {
                habit.increaseCount();
                repository.updateHabit(habit);
            }
        } catch (NumberFormatException ignored) {
            // Непонятные данные кнопки: ничего не отмечаем
        }
    }

    /** Нажали "✅ название" на экране "Сегодня": добавляем отметку и показываем экран заново. */
    private BotReply onDoneButton(Long chatId, String habitIdText) {
        markDone(chatId, habitIdText);
        return today(chatId);
    }

    /** Нажали "⏭ Пропустить": выбор привычки кнопкой. Выполненные и уже пропущенные в списке не показываются. */
    private BotReply skipMenu(Long chatId) {
        List<Habit> habits = repository.getAllHabits(chatId);
        if (habits.isEmpty()) return today(chatId);

        List<Button> skipButtons = new ArrayList<>();
        for (Habit h : habits) {
            if (!h.isCompleted() && !h.isSkipped()) {
                skipButtons.add(new Button("⏭ " + h.getTitle(), SKIP_PREFIX + h.getId()));
            }
        }
        String text = skipButtons.isEmpty()
                ? "Пропускать нечего: все привычки уже выполнены или пропущены."
                : "Какую привычку пропустить сегодня?";

        List<List<Button>> rows = inRows(skipButtons, 2);
        rows.add(List.of(new Button("← Назад", TODAY)));
        return new BotReply(text, rows);
    }

    /** Выбрали привычку для пропуска: отмечаем её и возвращаемся на экран "Сегодня". */
    private BotReply onSkipButton(Long chatId, String habitIdText) {
        try {
            Habit habit = repository.getHabit(chatId, Long.parseLong(habitIdText));
            // Выполненную привычку пропускать не нужно
            if (habit != null && !habit.isCompleted()) {
                habit.skip();
                repository.updateHabit(habit);
            }
        } catch (NumberFormatException ignored) {
            // Непонятные данные кнопки: просто покажем экран "Сегодня"
        }
        return today(chatId);
    }

    /** Экран "Статистика": итоги дня и кнопка возврата к плану. */
    private BotReply statsScreen(Long chatId) {
        List<Habit> habits = repository.getAllHabits(chatId);
        if (habits.isEmpty()) return BotReply.of("Список пуст. Нет данных для статистики.");

        int completed = (int) habits.stream().filter(Habit::isCompleted).count();
        int percent = completed * 100 / habits.size();

        String text = "📊 Статистика за сегодня\n\n"
                + "Всего привычек: " + habits.size() + "\n"
                + "Выполнено полностью: " + completed + "\n"
                + "Процент выполнения: " + percent + "%";
        return new BotReply(text, List.of(List.of(new Button("← К плану на сегодня", TODAY))));
    }

    private String askIndex(Long chatId, UserContext ctx, String question, State next, boolean showStatus, String emptyMessage) {
        List<Habit> habits = repository.getAllHabits(chatId);
        if (habits.isEmpty()) return emptyMessage;

        ctx.lastShownHabitIds.clear();
        StringBuilder sb = new StringBuilder(question);

        for (int i = 0; i < habits.size(); i++) {
            Habit h = habits.get(i);
            ctx.lastShownHabitIds.add(h.getId());
            
            sb.append("\n").append(i + 1).append(". ").append(h.getTitle());
            if (showStatus) sb.append(" ").append(h.getStatus());
        }
        sb.append("\nВведите номер (или /cancel для отмены):");
        ctx.state = next;
        return sb.toString();
    }

    private int parseIndex(String input, int maxSize) {
        try {
            int number = Integer.parseInt(input);
            if (number >= 1 && number <= maxSize) return number - 1;
        } catch (NumberFormatException ignored) {}
        return -1;
    }

    private String onDone(Long chatId, UserContext ctx, String input) {
        int index = parseIndex(input, ctx.lastShownHabitIds.size());
        if (index < 0) return "Ошибка: неверный номер. Введите число из списка или /cancel.";

        ctx.state = State.IDLE;
        Long realHabitId = ctx.lastShownHabitIds.get(index);
        Habit selected = repository.getHabit(chatId, realHabitId);
        
        if (selected == null) return "Ошибка: привычка не найдена.";

        selected.increaseCount();
        repository.updateHabit(selected);

        String reply = "Отметка добавлена: " + selected.getTitle()
                + " (" + selected.getCurrentCount() + "/" + selected.getTargetCount() + ")";
        if (selected.isCompleted()) reply += "\nЦель на день достигнута.";
        
        return reply;
    }

    private String onSkip(Long chatId, UserContext ctx, String input) {
        int index = parseIndex(input, ctx.lastShownHabitIds.size());
        if (index < 0) return "Ошибка: неверный номер. Введите число из списка или /cancel.";

        ctx.state = State.IDLE;
        Long realHabitId = ctx.lastShownHabitIds.get(index);
        Habit toSkip = repository.getHabit(chatId, realHabitId);
        
        if (toSkip == null) return "Ошибка: привычка не найдена.";
        if (toSkip.isCompleted()) return "Привычка '" + toSkip.getTitle() + "' уже выполнена, пропускать нечего.";
        
        toSkip.skip();
        repository.updateHabit(toSkip);
        return "Привычка '" + toSkip.getTitle() + "' отмечена как пропущенная ❌";
    }

    private String onDelete(Long chatId, UserContext ctx, String input) {
        int index = parseIndex(input, ctx.lastShownHabitIds.size());
        if (index < 0) return "Ошибка: неверный номер. Введите число из списка или /cancel.";

        ctx.state = State.IDLE;
        Long realHabitId = ctx.lastShownHabitIds.get(index);
        Habit toRemove = repository.getHabit(chatId, realHabitId);
        
        if (toRemove != null) {
            repository.removeHabit(chatId, realHabitId);
            return "Привычка '" + toRemove.getTitle() + "' удалена.";
        }
        return "Ошибка: привычка не найдена.";
    }

    /** Начало добавления: просим название (его пишут текстом). */
    private BotReply startAdd(UserContext ctx) {
        ctx.state = State.ADD_TITLE;
        return BotReply.of("Как назовём привычку?");
    }

    /** Ввели название: дальше выбираем, сколько раз в день. */
    private BotReply onAddTitle(UserContext ctx, String input) {
        if (input.length() > MAX_TITLE_LENGTH) {
            return BotReply.of("Название слишком длинное (не больше " + MAX_TITLE_LENGTH + " символов). Как назовём привычку?");
        }
        ctx.pendingTitle = input;
        ctx.state = State.ADD_COUNT;
        return countPrompt("Сколько раз в день выполнять?");
    }

    /** Вопрос о количестве раз в день: кнопки 1-5 и "✖ Отмена". */
    private BotReply countPrompt(String text) {
        List<Button> numbers = new ArrayList<>();
        for (int n = 1; n <= 5; n++) {
            numbers.add(new Button(String.valueOf(n), COUNT_PREFIX + n));
        }
        return new BotReply(text, List.of(numbers, List.of(new Button("✖ Отмена", ADD_CANCEL))));
    }

    /** Нажали кнопку с числом. */
    private BotReply onCountButton(Long chatId, UserContext ctx, String numberText) {
        if (ctx.state != State.ADD_COUNT) return null;   // кнопка из закрытого шага: игнорируем
        try {
            int count = Integer.parseInt(numberText);
            if (count >= 1 && count <= MAX_COUNT) return chooseCount(chatId, ctx, count);
        } catch (NumberFormatException ignored) {
            // Непонятные данные кнопки: игнорируем
        }
        return null;
    }

    /** Вместо кнопки число написали текстом: принимаем, если оно разумное. */
    private BotReply onAddCountTyped(Long chatId, UserContext ctx, String input) {
        try {
            int count = Integer.parseInt(input);
            if (count >= 1 && count <= MAX_COUNT) return chooseCount(chatId, ctx, count);
        } catch (NumberFormatException ignored) {
            // Не число: подскажем ниже
        }
        return countPrompt("Выберите кнопкой или напишите число от 1 до " + MAX_COUNT + ". Сколько раз в день выполнять?");
    }

    private BotReply chooseCount(Long chatId, UserContext ctx, int count) {
        ctx.pendingCount = count;
        ctx.state = State.ADD_CATEGORY;
        return categoryPrompt(chatId, ctx);
    }

    /** Выбор категории: уже существующие, "Без категории" и "✏️ Новая", внизу "✖ Отмена". */
    private BotReply categoryPrompt(Long chatId, UserContext ctx) {
        ctx.offeredCategories.clear();
        for (List<Habit> group : groupByCategory(repository.getAllHabits(chatId))) {
            String name = group.get(0).getCategory();
            if (!name.equalsIgnoreCase(NO_CATEGORY)) ctx.offeredCategories.add(name);
        }

        // В данных кнопки - номер категории в этом списке, а не название 
        List<Button> buttons = new ArrayList<>();
        for (int i = 0; i < ctx.offeredCategories.size(); i++) {
            buttons.add(new Button(ctx.offeredCategories.get(i), ADD_CAT_PREFIX + i));
        }
        buttons.add(new Button(NO_CATEGORY, ADD_CAT_PREFIX + ADD_CAT_NONE));
        buttons.add(new Button("✏️ Новая", ADD_CAT_PREFIX + ADD_CAT_NEW));

        List<List<Button>> rows = inRows(buttons, 2);
        rows.add(List.of(new Button("✖ Отмена", ADD_CANCEL)));
        return new BotReply("Выбери категорию:", rows);
    }

    /** Нажали кнопку на шаге выбора категории. */
    private BotReply onAddCategoryButton(Long chatId, UserContext ctx, String choice) {
        if (ctx.state != State.ADD_CATEGORY) return null;   // кнопка из закрытого шага: игнорируем

        if (choice.equals(ADD_CAT_NONE)) {
            return finishAdd(chatId, ctx, "");
        }
        if (choice.equals(ADD_CAT_NEW)) {
            ctx.state = State.ADD_NEW_CATEGORY;
            return new BotReply("Введите название категории:", List.of(List.of(new Button("✖ Отмена", ADD_CANCEL))));
        }
        try {
            int index = Integer.parseInt(choice);
            if (index >= 0 && index < ctx.offeredCategories.size()) {
                return finishAdd(chatId, ctx, ctx.offeredCategories.get(index));
            }
        } catch (NumberFormatException ignored) {
            // Непонятные данные кнопки: игнорируем
        }
        return null;
    }

    /** Вместо кнопки категорию написали текстом (или нажали "✏️ Новая" и ввели название). */
    private BotReply onAddCategoryTyped(Long chatId, UserContext ctx, String input) {
        if (input.length() > MAX_CATEGORY_LENGTH) {
            return new BotReply("Название категории слишком длинное (не больше " + MAX_CATEGORY_LENGTH
                    + " символов). Введите название категории:", List.of(List.of(new Button("✖ Отмена", ADD_CANCEL))));
        }
        return finishAdd(chatId, ctx, input.equals("-") ? "" : input);
    }

    /** Все данные собраны: сохраняем привычку и показываем итог. */
    private BotReply finishAdd(Long chatId, UserContext ctx, String category) {
        Habit habit = new Habit(ctx.pendingTitle, ctx.pendingCount, category);
        repository.addHabit(chatId, habit);
        ctx.state = State.IDLE;

        String text = "✅ Привычка «" + habit.getTitle() + "» добавлена\n"
                + timesPerDay(habit.getTargetCount()) + " · " + habit.getCategory();
        return new BotReply(text, List.of(List.of(new Button("📅 К плану на сегодня", TODAY))));
    }

    /** Отмена добавления. Повторное нажатие (когда добавления уже нет) игнорируем. */
    private BotReply onAddCancel(UserContext ctx) {
        boolean adding = ctx.state == State.ADD_TITLE || ctx.state == State.ADD_COUNT
                || ctx.state == State.ADD_CATEGORY || ctx.state == State.ADD_NEW_CATEGORY;
        if (!adding) return null;
        ctx.state = State.IDLE;
        return BotReply.of("Добавление отменено.");
    }

    /** "1 раз в день", "2 раза в день", "5 раз в день" - с правильным склонением. */
    private String timesPerDay(int n) {
        int last = n % 10;
        int lastTwo = n % 100;
        String word;
        if (last == 1 && lastTwo != 11) {
            word = "раз";
        } else if (last >= 2 && last <= 4 && (lastTwo < 12 || lastTwo > 14)) {
            word = "раза";
        } else {
            word = "раз";
        }
        return n + " " + word + " в день";
    }

    /** Экран "Категории": кнопка на каждую категорию с числом привычек и кнопка "Все привычки". */
    private BotReply categoriesScreen(Long chatId) {
        List<Habit> habits = repository.getAllHabits(chatId);
        if (habits.isEmpty()) return listScreen(chatId, null);

        List<Button> buttons = new ArrayList<>();
        for (List<Habit> group : groupByCategory(habits)) {
            // В данных кнопки - номер первой привычки категории, а не её название
            buttons.add(new Button(group.get(0).getCategory() + " · " + group.size(), CAT_PREFIX + group.get(0).getId()));
        }
        List<List<Button>> rows = inRows(buttons, 2);
        rows.add(List.of(new Button("Все привычки · " + habits.size(), CAT_PREFIX + VIEW_ALL)));
        return new BotReply("📁 Категории\nВыбери, что показать:", rows);
    }

    /**Привычки по категориям: в порядке появления, "Без категории" в конце. */
    private List<List<Habit>> groupByCategory(List<Habit> habits) {
        Map<String, List<Habit>> groups = new LinkedHashMap<>();
        List<Habit> withoutCategory = new ArrayList<>();
        for (Habit h : habits) {
            if (h.getCategory().equalsIgnoreCase(NO_CATEGORY)) {
                withoutCategory.add(h);
            } else {
                groups.computeIfAbsent(h.getCategory().toLowerCase(), k -> new ArrayList<>()).add(h);
            }
        }
        List<List<Habit>> result = new ArrayList<>(groups.values());
        if (!withoutCategory.isEmpty()) result.add(withoutCategory);
        return result;
    }

    /**
     * Открыта одна категория (или "Все привычки"): привычки с отметкой и кнопки "✅" для невыполненных.
     *
     * @param view "all" или номер привычки из нужной категории
     */
    private BotReply categoryView(Long chatId, String view) {
        List<Habit> habits = repository.getAllHabits(chatId);
        String title;
        List<Habit> shown = new ArrayList<>();

        if (view.equals(VIEW_ALL)) {
            title = "Все привычки";
            shown.addAll(habits);
        } else {
            Habit sample = null;
            try {
                sample = repository.getHabit(chatId, Long.parseLong(view));
            } catch (NumberFormatException ignored) {
                // Непонятные данные кнопки: покажем список категорий
            }
            // Привычку удалили или категорию переименовали: список категорий уже другой
            if (sample == null) return categoriesScreen(chatId);
            title = sample.getCategory();
            for (Habit h : habits) {
                if (h.getCategory().equalsIgnoreCase(title)) shown.add(h);
            }
        }
        if (shown.isEmpty()) return categoriesScreen(chatId);

        StringBuilder sb = new StringBuilder("📁 ").append(title).append("\n");
        List<Button> doneButtons = new ArrayList<>();
        for (Habit h : shown) {
            sb.append("\n").append(statusIcon(h)).append(" ").append(h.getTitle())
                    .append(" — ").append(h.getCurrentCount()).append("/").append(h.getTargetCount());
            if (!h.isCompleted()) {
                doneButtons.add(new Button("✅ " + h.getTitle(), CAT_DONE_PREFIX + view + ":" + h.getId()));
            }
        }
        List<List<Button>> rows = inRows(doneButtons, 2);
        rows.add(List.of(new Button("← К категориям", CATS)));
        return new BotReply(sb.toString(), rows);
    }

    /** Нажали "✅ название" внутри категории: добавляем отметку и остаёмся в этой же категории. */
    private BotReply onCategoryDoneButton(Long chatId, String data) {
        int colon = data.indexOf(':');
        if (colon < 0) return categoriesScreen(chatId);

        markDone(chatId, data.substring(colon + 1));
        return categoryView(chatId, data.substring(0, colon));
    }
}