package com.habitbot.repository;

import com.habitbot.model.Habit;
import java.util.List;

public interface HabitRepository {
    List<Habit> getAllHabits();
    void addHabit(Habit habit);
    void removeHabit(int index);
    boolean isEmpty();
    int size();
    Habit getHabit(int index);
}