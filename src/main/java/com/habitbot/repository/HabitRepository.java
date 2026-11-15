package com.habitbot.repository;

import com.habitbot.model.Habit;
import java.util.List;

public interface HabitRepository {
    List<Habit> getAllHabits(Long chatId);
    void addHabit(Long chatId, Habit habit);
    void updateHabit(Habit habit);
    void removeHabit(Long chatId, Long habitId);
    boolean isEmpty(Long chatId);
    int size(Long chatId);
    Habit getHabit(Long chatId, Long habitId);
}