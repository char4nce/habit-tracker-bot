package com.habitbot.service;

import com.habitbot.model.Habit;
import com.habitbot.repository.HabitRepository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class HabitCommandHandler {

    private static final String COMMANDS =
            "/add, /list, /done, /skip, /today, /category, /stats, /delete, /cancel";

    private enum State {
        IDLE, ADD_TITLE, ADD_COUNT, ADD_CATEGORY, DONE_INDEX, SKIP_INDEX, DELETE_INDEX, CATEGORY_FILTER
    }

    private class UserContext {
        State state = State.IDLE;
        String pendingTitle;
        int pendingCount;
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

    public String handle(Long chatId, String text) {
        String input = text.trim();
        UserContext ctx = getContext(chatId);

        synchronized (ctx) {
            if (input.startsWith("/")) {
                ctx.state = State.IDLE;
                return handleCommand(chatId, ctx, normalizeCommand(input));
            }

            switch (ctx.state) {
                case ADD_TITLE: return onAddTitle(ctx, input);
                case ADD_COUNT: return onAddCount(ctx, input);
                case ADD_CATEGORY: return onAddCategory(chatId, ctx, input);
                case DONE_INDEX: return onDone(chatId, ctx, input);
                case SKIP_INDEX: return onSkip(chatId, ctx, input);
                case DELETE_INDEX: return onDelete(chatId, ctx, input);
                case CATEGORY_FILTER: return onCategoryFilter(chatId, ctx, input);
                default: return "Не понимаю. Доступные команды: " + COMMANDS;
            }
        }
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
            case "/start": return "Привет! Я трекер привычек.\nДоступные команды: " + COMMANDS;
            case "/help": return "Доступные команды: " + COMMANDS;
            case "/cancel": return "Действие отменено.";
            case "/list": return list(chatId);
            case "/add":
                ctx.state = State.ADD_TITLE;
                return "Введите название привычки:";
            case "/done":
                return askIndex(chatId, ctx, "Какую привычку отметить?", State.DONE_INDEX, false, "Список пуст.");
            case "/skip":
                return askIndex(chatId, ctx, "Какую привычку пропустить сегодня?", State.SKIP_INDEX, true, "Список пуст.");
            case "/today": return today(chatId);
            case "/category": return askCategory(chatId, ctx);
            case "/stats": return stats(chatId);
            case "/delete":
                return askIndex(chatId, ctx, "Какую привычку удалить?", State.DELETE_INDEX, false, "Список пуст. Нечего удалять.");
            default: return "Неизвестная команда. Доступно: " + COMMANDS;
        }
    }

    private String list(Long chatId) {
        List<Habit> habits = repository.getAllHabits(chatId);
        if (habits.isEmpty()) return "Список пуст. Добавьте привычку через /add";
        
        StringBuilder sb = new StringBuilder("--- Ваши привычки ---");
        for (int i = 0; i < habits.size(); i++) {
            Habit h = habits.get(i);
            sb.append("\n").append(i + 1).append(". ").append(h.getTitle())
                    .append(" [").append(h.getCurrentCount()).append("/").append(h.getTargetCount()).append("]")
                    .append(" (").append(h.getCategory()).append(")");
        }
        return sb.toString();
    }

    private String today(Long chatId) {
        List<Habit> habits = repository.getAllHabits(chatId);
        if (habits.isEmpty()) return "Список пуст. Добавьте привычку через /add";
        
        StringBuilder sb = new StringBuilder("--- План на сегодня ---");
        int done = 0;
        for (int i = 0; i < habits.size(); i++) {
            Habit h = habits.get(i);
            sb.append("\n").append(i + 1).append(". ").append(h.getTitle())
                    .append(" [").append(h.getCurrentCount()).append("/").append(h.getTargetCount()).append("] ")
                    .append(h.getStatus());
            if (h.isCompleted()) done++;
        }
        sb.append("\nВыполнено: ").append(done).append(" из ").append(habits.size());
        return sb.toString();
    }

    private String stats(Long chatId) {
        List<Habit> habits = repository.getAllHabits(chatId);
        if (habits.isEmpty()) return "Список пуст. Нет данных для статистики.";
        
        int completed = (int) habits.stream().filter(Habit::isCompleted).count();
        int percent = (int) (((double) completed / habits.size()) * 100);
        
        return "--- Статистика за сегодня ---\n"
                + "Всего привычек: " + habits.size() + "\n"
                + "Выполнено полностью: " + completed + "\n"
                + "Процент выполнения: " + percent + "%";
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

    private String onAddTitle(UserContext ctx, String input) {
        if (input.isEmpty()) return "Название не может быть пустым. Введите название привычки:";
        ctx.pendingTitle = input;
        ctx.state = State.ADD_COUNT;
        return "Сколько раз в день выполнять?";
    }

    private String onAddCount(UserContext ctx, String input) {
        try {
            int count = Integer.parseInt(input);
            if (count >= 1) {
                ctx.pendingCount = count;
                ctx.state = State.ADD_CATEGORY;
                return "Введите категорию (или отправьте - чтобы оставить без категории):";
            }
        } catch (NumberFormatException ignored) {}
        return "Введите целое число от 1. Сколько раз в день выполнять?";
    }

    private String onAddCategory(Long chatId, UserContext ctx, String input) {
        String category = input.equals("-") ? "" : input;
        repository.addHabit(chatId, new Habit(ctx.pendingTitle, ctx.pendingCount, category));
        ctx.state = State.IDLE;
        return "Привычка '" + ctx.pendingTitle + "' сохранена.";
    }

    private String askCategory(Long chatId, UserContext ctx) {
        List<Habit> habits = repository.getAllHabits(chatId);
        if (habits.isEmpty()) return "Список пуст. Добавьте привычку через /add";

        Set<String> categories = new LinkedHashSet<>();
        for (Habit h : habits) {
            categories.add(h.getCategory());
        }
        ctx.state = State.CATEGORY_FILTER;
        return "Доступные категории: " + String.join(", ", categories)
                + "\nВведите категорию (или /cancel для отмены):";
    }

    private String onCategoryFilter(Long chatId, UserContext ctx, String input) {
        ctx.state = State.IDLE;
        List<Habit> habits = repository.getAllHabits(chatId);
        
        StringBuilder sb = new StringBuilder("--- Категория: " + input + " ---");
        boolean found = false;
        for (int i = 0; i < habits.size(); i++) {
            Habit h = habits.get(i);
            if (h.getCategory().equalsIgnoreCase(input)) {
                sb.append("\n").append(i + 1).append(". ").append(h.getTitle())
                        .append(" [").append(h.getCurrentCount()).append("/").append(h.getTargetCount()).append("] ")
                        .append(h.getStatus());
                found = true;
            }
        }
        if (!found) sb.append("\nВ этой категории привычек нет.");
        return sb.toString();
    }
}