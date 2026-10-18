package com.habitbot.bot;

import com.habitbot.model.Habit;
import com.habitbot.repository.HabitRepository;

import java.util.LinkedHashSet;
import java.util.Scanner;
import java.util.Set;

public class ConsoleBot implements Bot {
    private final HabitRepository habitRepository;

    public ConsoleBot(HabitRepository habitRepository) {
        this.habitRepository = habitRepository;
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);

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
                    if (habitRepository.isEmpty()) {
                        System.out.println("Список пуст. Добавьте привычку через /add");
                    } else {
                        System.out.println("--- Ваши привычки ---");
                        for (int i = 0; i < habitRepository.size(); i++) {
                            Habit h = habitRepository.getHabit(i);
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

                    habitRepository.addHabit(new Habit(title, count, category));
                    System.out.println("Привычка '" + title + "' сохранена.");
                    break;

                case "/done":
                    if (habitRepository.isEmpty()) {
                        System.out.println("Список пуст.");
                        break;
                    }

                    System.out.println("Какую привычку отметить?");
                    for (int i = 0; i < habitRepository.size(); i++) {
                        Habit h = habitRepository.getHabit(i);
                        System.out.println((i + 1) + ". " + h.getTitle());
                    }

                    System.out.print("Введите номер > ");
                    int doneIndex = scanner.nextInt();
                    scanner.nextLine();

                    if (doneIndex >= 1 && doneIndex <= habitRepository.size()) {
                        Habit selected = habitRepository.getHabit(doneIndex - 1);
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
                    if (habitRepository.isEmpty()) {
                        System.out.println("Список пуст.");
                        break;
                    }

                    System.out.println("Какую привычку пропустить сегодня?");
                    for (int i = 0; i < habitRepository.size(); i++) {
                        Habit h = habitRepository.getHabit(i);
                        System.out.println((i + 1) + ". " + h.getTitle() + " " + h.getStatus());
                    }

                    System.out.print("Введите номер > ");
                    int skipIndex = scanner.nextInt();
                    scanner.nextLine();

                    if (skipIndex >= 1 && skipIndex <= habitRepository.size()) {
                        Habit toSkip = habitRepository.getHabit(skipIndex - 1);

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
                    if (habitRepository.isEmpty()) {
                        System.out.println("Список пуст. Добавьте привычку через /add");
                        break;
                    }

                    System.out.println("--- План на сегодня ---");
                    int todayDone = 0;
                    for (int i = 0; i < habitRepository.size(); i++) {
                        Habit h = habitRepository.getHabit(i);
                        System.out.println((i + 1) + ". " + h.getTitle() +
                                " [" + h.getCurrentCount() + "/" + h.getTargetCount() + "] " + h.getStatus());
                        if (h.isCompleted()) {
                            todayDone++;
                        }
                    }
                    System.out.println("Выполнено: " + todayDone + " из " + habitRepository.size());
                    break;

                case "/category":
                    if (habitRepository.isEmpty()) {
                        System.out.println("Список пуст. Добавьте привычку через /add");
                        break;
                    }

                    Set<String> categories = new LinkedHashSet<>();
                    for (int i = 0; i < habitRepository.size(); i++) {
                        categories.add(habitRepository.getHabit(i).getCategory());
                    }
                    System.out.println("Доступные категории: " + String.join(", ", categories));

                    System.out.print("Введите категорию > ");
                    String filter = scanner.nextLine().trim();

                    System.out.println("--- Категория: " + filter + " ---");
                    boolean found = false;
                    for (int i = 0; i < habitRepository.size(); i++) {
                        Habit h = habitRepository.getHabit(i);
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
                    if (habitRepository.isEmpty()) {
                        System.out.println("Список пуст. Нет данных для статистики.");
                        break;
                    }

                    int totalHabits = habitRepository.size();
                    int completedHabits = 0;
                    for (int i = 0; i < habitRepository.size(); i++) {
                        Habit h = habitRepository.getHabit(i);
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
                    if (habitRepository.isEmpty()) {
                        System.out.println("Список пуст. Нечего удалять.");
                        break;
                    }

                    System.out.println("Какую привычку удалить?");
                    for (int i = 0; i < habitRepository.size(); i++) {
                        Habit h = habitRepository.getHabit(i);
                        System.out.println((i + 1) + ". " + h.getTitle());
                    }

                    System.out.print("Введите номер > ");
                    int deleteIndex = scanner.nextInt();
                    scanner.nextLine();

                    if (deleteIndex >= 1 && deleteIndex <= habitRepository.size()) {
                        Habit removed = habitRepository.getHabit(deleteIndex - 1);
                        habitRepository.removeHabit(deleteIndex - 1);
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
}