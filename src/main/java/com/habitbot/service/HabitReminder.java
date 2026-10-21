package com.habitbot.service;

import com.habitbot.model.Habit;
import com.habitbot.repository.HabitRepository;

import java.time.LocalTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HabitReminder {
    private final HabitRepository repository;
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();

    public HabitReminder(HabitRepository repository) {
        this.repository = repository;
    }

    public void start() {

        timer.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                LocalTime now = LocalTime.now();
                String currentTime = String.format("%02d:%02d", now.getHour(), now.getMinute());

                for (Habit h : repository.getAllHabits()) {
                    if (!h.isCompleted() && h.getReminderTime() != null && h.getReminderTime().equals(currentTime)) {
                        System.out.println("\nПора выполнять привычку: " + h.getTitle());
                        System.out.print("\nВведите команду > ");
                    }
                }
            }
        }, 0, 1, TimeUnit.MINUTES);
    }

    public void stop() {
        timer.shutdown();
    }
}