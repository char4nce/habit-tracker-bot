package com.habitbot;

/**
 * Класс Habit описывает отдельную привычку пользователя,
 * хранит её название, целевое и текущее количество выполнений
 */
public class Habit {
    private String title;
    private int targetCount;
    private int currentCount;
    private int category; // или String category (оставляем String ниже)
    private String categoryName;
    private boolean skipped;

    /**
     * Создает новую привычку с заданным названием и целью на день
     */
    public Habit(String newTitle, int newTargetCount) {
        this(newTitle, newTargetCount, "Без категории");
    }

    /**
     * Создает новую привычку с заданным названием, целью на день и категорией
     */
    public Habit(String newTitle, int newTargetCount, String newCategory) {
        title = newTitle;
        targetCount = newTargetCount;
        currentCount = 0;
        skipped = false;
        if (newCategory == null || newCategory.trim().isEmpty()) {
            categoryName = "Без категории";
        } else {
            categoryName = newCategory.trim();
        }
    }

    public String getTitle() {
        return title;
    }

    public int getTargetCount() {
        return targetCount;
    }

    public int getCurrentCount() {
        return currentCount;
    }

    public String getCategory() {
        return categoryName;
    }

    public boolean isSkipped() {
        return skipped;
    }

    public void increaseCount() {
        currentCount++;
        skipped = false;
    }

    public void skip() {
        skipped = true;
    }

    public boolean isCompleted() {
        return currentCount >= targetCount;
    }

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