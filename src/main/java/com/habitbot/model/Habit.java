package com.habitbot.model;

import jakarta.persistence.*;

/**
 * Класс Habit описывает отдельную привычку пользователя,
 * хранит её название, целевое и текущее количество выполнений
 */
@Entity
@Table(name = "habits")
public class Habit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(nullable = false)
    private String title;

    @Column(name = "target_count")
    private int targetCount;

    @Column(name = "current_count")
    private int currentCount;

    @Column(name = "category_name")
    private String categoryName;

    private boolean skipped;

    @Column(name = "reminder_time")
    private String reminderTime = "";

    // Пустой конструктор для Hibernate
    protected Habit() {
    }

    public Habit(String newTitle, int newTargetCount) {
        this(0L, newTitle, newTargetCount, "Без категории");
    }

    public Habit(String newTitle, int newTargetCount, String newCategory) {
        this(0L, newTitle, newTargetCount, newCategory);
    }

    public Habit(Long chatId, String newTitle, int newTargetCount, String newCategory) {
        this.chatId = chatId;
        this.title = newTitle;
        this.targetCount = newTargetCount;
        this.currentCount = 0;
        this.skipped = false;
        if (newCategory == null || newCategory.trim().isEmpty()) {
            this.categoryName = "Без категории";
        } else {
            this.categoryName = newCategory.trim();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
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

    public String getReminderTime() {
        return reminderTime;
    }

    public void setReminderTime(String reminderTime) {
        this.reminderTime = reminderTime;
    }
}