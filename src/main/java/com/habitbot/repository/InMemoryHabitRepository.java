package com.habitbot.repository;

import com.habitbot.model.Habit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryHabitRepository implements HabitRepository {
    private final Map<Long, List<Habit>> userHabits = new HashMap<>();
    private long idCounter = 1L;

    private List<Habit> getList(Long chatId) {
        if (!userHabits.containsKey(chatId)) {
            userHabits.put(chatId, new ArrayList<>());
        }
        return userHabits.get(chatId);
    }

    @Override
    public List<Habit> getAllHabits(Long chatId) {
        return getList(chatId);
    }

    @Override
    public void addHabit(Long chatId, Habit habit) {
        habit.setChatId(chatId);
        
        habit.setId(idCounter);
        idCounter++; 
        
        getList(chatId).add(habit);
    }

    @Override
    public void updateHabit(Habit habit) {
    }

    @Override
    public void removeHabit(Long chatId, Long habitId) {
        List<Habit> habits = getList(chatId);
        
        // Обычный цикл для поиска и удаления
        for (int i = 0; i < habits.size(); i++) {
            if (habits.get(i).getId().equals(habitId)) {
                habits.remove(i);
                break;
            }
        }
    }

    @Override
    public boolean isEmpty(Long chatId) {
        return getList(chatId).isEmpty();
    }

    @Override
    public int size(Long chatId) {
        return getList(chatId).size();
    }

    @Override
    public Habit getHabit(Long chatId, Long habitId) {
        List<Habit> habits = getList(chatId);
        
        for (int i = 0; i < habits.size(); i++) {
            Habit h = habits.get(i);
            if (h.getId().equals(habitId)) {
                return h;
            }
        }
        
        return null;
    }
}