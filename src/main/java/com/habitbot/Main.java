package com.habitbot;
 
import com.habitbot.bot.Bot;
import com.habitbot.bot.BotFactory;
 
public class Main {
    public static void main(String[] args) {
        Bot bot = BotFactory.create();
        bot.run();
    }
}
 