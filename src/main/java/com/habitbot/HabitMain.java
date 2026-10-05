package com.habitbot;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Scanner;
import java.util.Set;

/**
 * Класс Habit описывает отдельную привычку пользователя,
 * хранит её название, целевое и текущее количество выполнений
 */
class Habit {
    private String title;
    private int targetCount;
    private int currentCount;
    private String category;
    private boolean skipped;

    /**
     * Создает новую привычку с заданным названием и целью на день
     * newTitle название привычки
     * newTargetCount целевое количество выполнений в день
     */
    public Habit(String newTitle, int newTargetCount) {
        this(newTitle, newTargetCount, "Без категории");
    }

    /**
     * Создает новую привычку с заданным названием, целью на день и категорией
     * Если категория пустая, привычке назначается категория "Без категории"
     */
    public Habit(String newTitle, int newTargetCount, String newCategory) {
        title = newTitle;
        targetCount = newTargetCount;
        currentCount = 0;
        skipped = false;
        if (newCategory == null || newCategory.trim().isEmpty()) {
            category = "Без категории";
        } else {
            category = newCategory.trim();
        }
    }

    /**
     * Возвращает название привычки
     */
    public String getTitle() {
        return title;
    }

    /**
     * Возвращает целевое количество выполнений привычки в день
     */
    public int getTargetCount() {
        return targetCount;
    }

    /**
     * Возвращает текущий прогресс выполнения привычки за сегодня
     */
    public int getCurrentCount() {
        return currentCount;
    }

    /**
     * Возвращает категорию привычки
     */
    public String getCategory() {
        return category;
    }

    /**
     * Проверяет, отмечена ли привычка как пропущенная на сегодня
     */
    public boolean isSkipped() {
        return skipped;
    }

    /**
     * Увеличивает текущий прогресс выполнения привычки на единицу
     * Если привычка была отмечена как пропущенная, отметка снимается
     */
    public void increaseCount() {
        currentCount++;
        skipped = false;
    }

    /**
     * Отмечает привычку как пропущенную на сегодня
     */
    public void skip() {
        skipped = true;
    }

    /**
     * Проверяет, достигнута ли дневная цель по привычке
     * true, если цель выполнена или перевыполнена, иначе false
     */
    public boolean isCompleted() {
        return currentCount >= targetCount;
    }

    /**
     * Возвращает текстовый статус привычки на сегодня:
     * выполнено, пропущено, в процессе или не начато
     */
    public String getStatus() {
        if (isCompleted()) {
            return "✅ выполнено";
        } else if (skipped) {
            return "❌ пропущено";
        } else if (currentCount > 0) {
            return "⏳ в процессе";
        } else {
            return "⬜ не начато";
        }
    }
}

/**
 * Главный класс HabitMain управляет консольным интерфейсом трекера привычек
 * обрабатывает команды пользователя и хранит список привычек
 */
public class HabitMain {

    /**
     * Точка входа в приложение, запускает цикл обработки команд
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        ArrayList<Habit> habits = new ArrayList<>();

        System.out.println("Бот-трекер привычек запущен.");
        System.out.println("Доступные команды: /add, /list, /done, /skip, /today, /category, /stats, /delete, /exit");

        while (true) {
            System.out.print("\nВведите команду > ");
            String command = scanner.nextLine().trim();

            if (command.equalsIgnoreCase("/exit")) {
                System.out.println("Бот завершил работу.");
                break;
            }

            switch (command.toLowerCase()) {
                case "/list":
                    if (habits.isEmpty()) {
                        System.out.println("Список пуст. Добавьте привычку через /add");
                    } else {
                        System.out.println("--- Ваши привычки ---");
                        for (int i = 0; i < habits.size(); i++) {
                            Habit h = habits.get(i);
                            System.out.println((i + 1) + ". " + h.getTitle() +
                                    " [" + h.getCurrentCount() + "/" + h.getTargetCount() + "]" +
                                    " (" + h.getCategory() + ")");
                        }
                    }
                    break;

                case "/add":
                    System.out.print("Введите название привычки: ");
                    String title = scanner.nextLine().trim();

                    System.out.print("Сколько раз в день выполнять? ");
                    int count = scanner.nextInt();
                    scanner.nextLine();

                    System.out.print("Введите категорию (Enter - без категории): ");
                    String category = scanner.nextLine().trim();

                    habits.add(new Habit(title, count, category));
                    System.out.println("Привычка '" + title + "' сохранена.");
                    break;

                case "/done":
                    if (habits.isEmpty()) {
                        System.out.println("Список пуст.");
                        break;
                    }

                    System.out.println("Какую привычку отметить?");
                    for (int i = 0; i < habits.size(); i++) {
                        Habit h = habits.get(i);
                        System.out.println((i + 1) + ". " + h.getTitle());
                    }

                    System.out.print("Введите номер > ");
                    int doneIndex = scanner.nextInt();
                    scanner.nextLine();

                    if (doneIndex >= 1 && doneIndex <= habits.size()) {
                        Habit selected = habits.get(doneIndex - 1);
                        selected.increaseCount();

                        System.out.println("Отметка добавлена: " + selected.getTitle() +
                                " (" + selected.getCurrentCount() + "/" + selected.getTargetCount() + ")");

                        if (selected.isCompleted()) {
                            System.out.println("Цель на день достигнута.");
                        }
                    } else {
                        System.out.println("Ошибка: неверный номер.");
                    }
                    break;

                case "/skip":
                    if (habits.isEmpty()) {
                        System.out.println("Список пуст.");
                        break;
                    }

                    System.out.println("Какую привычку пропустить сегодня?");
                    for (int i = 0; i < habits.size(); i++) {
                        Habit h = habits.get(i);
                        System.out.println((i + 1) + ". " + h.getTitle() + " " + h.getStatus());
                    }

                    System.out.print("Введите номер > ");
                    int skipIndex = scanner.nextInt();
                    scanner.nextLine();

                    if (skipIndex >= 1 && skipIndex <= habits.size()) {
                        Habit toSkip = habits.get(skipIndex - 1);

                        if (toSkip.isCompleted()) {
                            System.out.println("Привычка '" + toSkip.getTitle() + "' уже выполнена, пропускать нечего.");
                        } else {
                            toSkip.skip();
                            System.out.println("Привычка '" + toSkip.getTitle() + "' отмечена как пропущенная ❌");
                        }
                    } else {
                        System.out.println("Ошибка: неверный номер.");
                    }
                    break;

                case "/today":
                    if (habits.isEmpty()) {
                        System.out.println("Список пуст. Добавьте привычку через /add");
                        break;
                    }

                    System.out.println("--- План на сегодня ---");
                    int todayDone = 0;
                    for (int i = 0; i < habits.size(); i++) {
                        Habit h = habits.get(i);
                        System.out.println((i + 1) + ". " + h.getTitle() +
                                " [" + h.getCurrentCount() + "/" + h.getTargetCount() + "] " + h.getStatus());
                        if (h.isCompleted()) {
                            todayDone++;
                        }
                    }
                    System.out.println("Выполнено: " + todayDone + " из " + habits.size());
                    break;

                case "/category":
                    if (habits.isEmpty()) {
                        System.out.println("Список пуст. Добавьте привычку через /add");
                        break;
                    }

                    Set<String> categories = new LinkedHashSet<>();
                    for (int i = 0; i < habits.size(); i++) {
                        categories.add(habits.get(i).getCategory());
                    }
                    System.out.println("Доступные категории: " + String.join(", ", categories));

                    System.out.print("Введите категорию > ");
                    String filter = scanner.nextLine().trim();

                    System.out.println("--- Категория: " + filter + " ---");
                    boolean found = false;
                    for (int i = 0; i < habits.size(); i++) {
                        Habit h = habits.get(i);
                        if (h.getCategory().equalsIgnoreCase(filter)) {
                            System.out.println((i + 1) + ". " + h.getTitle() +
                                    " [" + h.getCurrentCount() + "/" + h.getTargetCount() + "] " + h.getStatus());
                            found = true;
                        }
                    }
                    if (!found) {
                        System.out.println("В этой категории привычек нет.");
                    }
                    break;

                case "/stats":
                    if (habits.isEmpty()) {
                        System.out.println("Список пуст. Нет данных для статистики.");
                        break;
                    }

                    int totalHabits = habits.size();
                    int completedHabits = 0;
                    for (int i = 0; i < habits.size(); i++) {
                        Habit h = habits.get(i);
                        if (h.isCompleted()) {
                            completedHabits++;
                        }
                    }

                    double percent = ((double) completedHabits / totalHabits) * 100;

                    System.out.println("--- Статистика за сегодня ---");
                    System.out.println("Всего привычек: " + totalHabits);
                    System.out.println("Выполнено полностью: " + completedHabits);
                    System.out.println("Процент выполнения: " + (int) percent + "%");
                    break;

                case "/delete":
                    if (habits.isEmpty()) {
                        System.out.println("Список пуст. Нечего удалять.");
                        break;
                    }

                    System.out.println("Какую привычку удалить?");
                    for (int i = 0; i < habits.size(); i++) {
                        Habit h = habits.get(i);
                        System.out.println((i + 1) + ". " + h.getTitle());
                    }

                    System.out.print("Введите номер > ");
                    int deleteIndex = scanner.nextInt();
                    scanner.nextLine();

                    if (deleteIndex >= 1 && deleteIndex <= habits.size()) {
                        Habit removed = habits.remove(deleteIndex - 1);
                        System.out.println("Привычка '" + removed.getTitle() + "' удалена.");
                    } else {
                        System.out.println("Ошибка: неверный номер.");
                    }
                    break;

                default:
                    System.out.println("Неизвестная команда. Доступно: /add, /list, /done, /skip, /today, /category, /stats, /delete, /exit");
                    break;
            }
        }

        scanner.close();
    }