package com.habitbot.repository;

import com.habitbot.model.Habit;
import java.util.ArrayList;
import java.util.List;

public class InMemoryHabitRepository implements HabitRepository {
    private final List<Habit> habits = new ArrayList<>();

    @Override
    public List<Habit> getAllHabits() {
        return habits;
    }

    @Override
    public void addHabit(Habit habit) {
        habits.add(habit);
    }

    @Override
    public void removeHabit(int index) {
        habits.remove(index);
    }

    @Override
    public boolean isEmpty() {
        return habits.isEmpty();
    }

    @Override
    public int size() {
        return habits.size();
    }

    @Override
    public Habit getHabit(int index) {
        return habits.get(index);
    }
}