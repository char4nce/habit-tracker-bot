package com.habitbot.repository;

import com.habitbot.model.Habit;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.List;

public class PostgresHabitRepository implements HabitRepository {
    private final SessionFactory sessionFactory;
    private final Long chatId;

    public PostgresHabitRepository(SessionFactory sessionFactory, Long chatId) {
        this.sessionFactory = sessionFactory;
        this.chatId = chatId;
    }

    @Override
    public List<Habit> getAllHabits() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery(
                    "FROM Habit WHERE chatId = :chatId ORDER BY id ASC", Habit.class)
                    .setParameter("chatId", chatId)
                    .getResultList();
        }
    }

    @Override
    public void addHabit(Habit habit) {
        habit.setChatId(chatId);
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            session.persist(habit);
            tx.commit();
        }
    }

    @Override
    public void updateHabit(Habit habit) {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            session.merge(habit);
            tx.commit();
        }
    }

    @Override
    public void removeHabit(int index) {
        List<Habit> habits = getAllHabits();
        if (index >= 0 && index < habits.size()) {
            Habit toRemove = habits.get(index);
            try (Session session = sessionFactory.openSession()) {
                Transaction tx = session.beginTransaction();
                Habit managedHabit = session.merge(toRemove);
                session.remove(managedHabit);
                tx.commit();
            }
        }
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public int size() {
        try (Session session = sessionFactory.openSession()) {
            Long count = session.createQuery(
                    "SELECT COUNT(h) FROM Habit h WHERE h.chatId = :chatId", Long.class)
                    .setParameter("chatId", chatId)
                    .uniqueResult();
            if (count == null) {
                return 0;
            } else {
                return count.intValue();
            }
        }
    }

    @Override
    public Habit getHabit(int index) {
        return getAllHabits().get(index);
    }
}