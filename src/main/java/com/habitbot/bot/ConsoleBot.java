package com.habitbot.bot;

import com.habitbot.model.Habit;
import com.habitbot.repository.HabitRepository;
import com.habitbot.service.HabitReminder;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public class ConsoleBot implements Bot {
    private final HabitRepository habitRepository;
    private final HabitReminder reminder;
    private final Long CHAT_ID = 0L; 

    public ConsoleBot(HabitRepository habitRepository) {
        this.habitRepository = habitRepository;

        // Напоминалки запускаются внутри бота
        this.reminder = new HabitReminder(habitRepository);
        this.reminder.start();
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
                reminder.stop();
                System.out.println("Бот завершил работу.");
                break;
            }

            switch (command.toLowerCase()) {
                case "/list":
                    List<Habit> allHabits = habitRepository.getAllHabits(CHAT_ID);
                    if (allHabits.isEmpty()) {
                        System.out.println("Список пуст. Добавьте привычку через /add");
                    } else {
                        System.out.println("--- Ваши привычки ---");
                        for (int i = 0; i < allHabits.size(); i++) {
                            Habit h = allHabits.get(i);
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

                    System.out.print("Во сколько напомнить? (в формате ЧЧ:ММ или Enter чтобы пропустить): ");
                    String time = scanner.nextLine().trim();
                    
                    Habit newHabit = new Habit(title, count, category);
                    newHabit.setReminderTime(time);

                    // ИЗМЕНЕНО: Передаем CHAT_ID
                    habitRepository.addHabit(CHAT_ID, newHabit); 
                    System.out.println("Привычка '" + title + "' сохранена.");
                    break;

                case "/done":
                    List<Habit> doneHabits = habitRepository.getAllHabits(CHAT_ID);
                    if (doneHabits.isEmpty()) {
                        System.out.println("Список пуст.");
                        break;
                    }

                    System.out.println("Какую привычку отметить?");
                    for (int i = 0; i < doneHabits.size(); i++) {
                        System.out.println((i + 1) + ". " + doneHabits.get(i).getTitle());
                    }

                    System.out.print("Введите номер > ");
                    int doneIndex = scanner.nextInt();
                    scanner.nextLine();

                    if (doneIndex >= 1 && doneIndex <= doneHabits.size()) {
                        Habit selected = doneHabits.get(doneIndex - 1);
                        selected.increaseCount();
                        habitRepository.updateHabit(selected);

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
                    List<Habit> skipHabits = habitRepository.getAllHabits(CHAT_ID);
                    if (skipHabits.isEmpty()) {
                        System.out.println("Список пуст.");
                        break;
                    }

                    System.out.println("Какую привычку пропустить сегодня?");
                    for (int i = 0; i < skipHabits.size(); i++) {
                        Habit h = skipHabits.get(i);
                        System.out.println((i + 1) + ". " + h.getTitle() + " " + h.getStatus());
                    }

                    System.out.print("Введите номер > ");
                    int skipIndex = scanner.nextInt();
                    scanner.nextLine();

                    if (skipIndex >= 1 && skipIndex <= skipHabits.size()) {
                        Habit toSkip = skipHabits.get(skipIndex - 1);

                        if (toSkip.isCompleted()) {
                            System.out.println("Привычка '" + toSkip.getTitle() + "' уже выполнена, пропускать нечего.");
                        } else {
                            toSkip.skip();
                            habitRepository.updateHabit(toSkip);
                            System.out.println("Привычка '" + toSkip.getTitle() + "' отмечена как пропущенная ❌");
                        }
                    } else {
                        System.out.println("Ошибка: неверный номер.");
                    }
                    break;

                case "/today":
                    List<Habit> todayHabits = habitRepository.getAllHabits(CHAT_ID);
                    if (todayHabits.isEmpty()) {
                        System.out.println("Список пуст. Добавьте привычку через /add");
                        break;
                    }

                    System.out.println("--- План на сегодня ---");
                    int todayDone = 0;
                    for (int i = 0; i < todayHabits.size(); i++) {
                        Habit h = todayHabits.get(i);
                        System.out.println((i + 1) + ". " + h.getTitle() +
                                " [" + h.getCurrentCount() + "/" + h.getTargetCount() + "] " + h.getStatus());
                        if (h.isCompleted()) {
                            todayDone++;
                        }
                    }
                    System.out.println("Выполнено: " + todayDone + " из " + todayHabits.size());
                    break;

                case "/category":
                    List<Habit> catHabits = habitRepository.getAllHabits(CHAT_ID);
                    if (catHabits.isEmpty()) {
                        System.out.println("Список пуст. Добавьте привычку через /add");
                        break;
                    }

                    Set<String> categories = new LinkedHashSet<>();
                    for (Habit h : catHabits) {
                        categories.add(h.getCategory());
                    }
                    System.out.println("Доступные категории: " + String.join(", ", categories));

                    System.out.print("Введите категорию > ");
                    String filter = scanner.nextLine().trim();

                    System.out.println("--- Категория: " + filter + " ---");
                    boolean found = false;
                    for (int i = 0; i < catHabits.size(); i++) {
                        Habit h = catHabits.get(i);
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
                    List<Habit> statHabits = habitRepository.getAllHabits(CHAT_ID);
                    if (statHabits.isEmpty()) {
                        System.out.println("Список пуст. Нет данных для статистики.");
                        break;
                    }

                    int totalHabits = statHabits.size();
                    int completedHabits = 0;
                    for (Habit h : statHabits) {
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
                    List<Habit> delHabits = habitRepository.getAllHabits(CHAT_ID);
                    if (delHabits.isEmpty()) {
                        System.out.println("Список пуст. Нечего удалять.");
                        break;
                    }

                    System.out.println("Какую привычку удалить?");
                    for (int i = 0; i < delHabits.size(); i++) {
                        System.out.println((i + 1) + ". " + delHabits.get(i).getTitle());
                    }

                    System.out.print("Введите номер > ");
                    int deleteIndex = scanner.nextInt();
                    scanner.nextLine();

                    if (deleteIndex >= 1 && deleteIndex <= delHabits.size()) {
                        // ИЗМЕНЕНО: используем реальный ID из базы данных для удаления
                        Habit toRemove = delHabits.get(deleteIndex - 1);
                        habitRepository.removeHabit(CHAT_ID, toRemove.getId());
                        System.out.println("Привычка '" + toRemove.getTitle() + "' удалена.");
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