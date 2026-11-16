package com.habitbot.service;

import java.util.List;

/**
 * @param text    текст сообщения
 * @param buttons кнопки под сообщением; пустой список, если кнопок нет
 */
public record BotReply(String text, List<List<Button>> buttons) {

    /**
     * Одна кнопка под сообщением.
     *
     * @param label надпись на кнопке
     * @param data  что бот получит при нажатии, например "done:5" (не длиннее 64 байт)
     */
    public record Button(String label, String data) {
    }

    /** Ответ без кнопок - просто текст. */
    public static BotReply of(String text) {
        return new BotReply(text, List.of());
    }
}
