package com.habitbot;

import java.util.ArrayList;
import java.util.Scanner;

/**
 * Класс Habit описывает отдельную привычку пользователя,
 * хранит её название, целевое и текущее количество выполнений
 */
class Habit {
    private String title;
    private int targetCount;
    private int currentCount;

    /**
     * Создает новую привычку с заданным названием и целью на день
     * newTitle название привычки
     * newTargetCount целевое количество выполнений в день
     */
    public Habit(String newTitle, int newTargetCount) {
        title = newTitle;
        targetCount = newTargetCount;
        currentCount = 0;
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
     * Увеличивает текущий прогресс выполнения привычки на единицу
     */
    public void increaseCount() {
        currentCount++;
    }

    /**
     * Проверяет, достигнута ли дневная цель по привычке
     * true, если цель выполнена или перевыполнена, иначе false
     */
    public boolean isCompleted() {
        return currentCount >= targetCount;
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
        System.out.println("Доступные команды: /add, /list, /done, /stats, /delete, /exit");

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
                                    " [" + h.getCurrentCount() + "/" + h.getTargetCount() + "]");
                        }
                    }
                    break;

                case "/add":
                    System.out.print("Введите название привычки: ");
                    String title = scanner.nextLine().trim();

                    System.out.print("Сколько раз в день выполнять? ");
                    int count = scanner.nextInt();
                    scanner.nextLine();

                    habits.add(new Habit(title, count));
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
                    System.out.println("Неизвестная команда. Доступно: /add, /list, /done, /stats, /delete, /exit");
                    break;
            }
        }

        scanner.close();
    }
}