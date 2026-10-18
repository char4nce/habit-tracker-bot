package com.habitbot;

import com.habitbot.bot.Bot;
import com.habitbot.bot.ConsoleBot;
import com.habitbot.repository.HabitRepository;
import com.habitbot.repository.InMemoryHabitRepository;
import com.habitbot.service.HabitReminder;

public class Main {
    public static void main(String[] args) {

        HabitRepository repository = new InMemoryHabitRepository();

        HabitReminder reminder = new HabitReminder(repository);
        reminder.start();

        Bot bot = new ConsoleBot(repository);
        bot.run(); 
    }
}