package com.habitbot.repository;

import com.habitbot.model.Habit;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.List;

public class PostgresHabitRepository implements HabitRepository {
    private final SessionFactory sessionFactory;

    public PostgresHabitRepository(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public List<Habit> getAllHabits(Long chatId) {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery(
                    "FROM Habit WHERE chatId = :chatId ORDER BY id ASC", Habit.class)
                    .setParameter("chatId", chatId)
                    .getResultList();
        } catch (Exception e) {
            System.err.println("Ошибка БД при получении списка: " + e.getMessage());
            return List.of();
        }
    }

    @Override
    public void addHabit(Long chatId, Habit habit) {
        habit.setChatId(chatId);
        Transaction tx = null;
        try (Session session = sessionFactory.openSession()) {
            tx = session.beginTransaction();
            session.persist(habit);
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            System.err.println("Ошибка БД при добавлении: " + e.getMessage());
        }
    }

    @Override
    public void updateHabit(Habit habit) {
        Transaction tx = null;
        try (Session session = sessionFactory.openSession()) {
            tx = session.beginTransaction();
            session.merge(habit);
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            System.err.println("Ошибка БД при обновлении: " + e.getMessage());
        }
    }

    @Override
    public void removeHabit(Long chatId, Long habitId) {
        Transaction tx = null;
        try (Session session = sessionFactory.openSession()) {
            tx = session.beginTransaction();
            Habit toRemove = session.createQuery(
                    "FROM Habit WHERE chatId = :chatId AND id = :habitId", Habit.class)
                    .setParameter("chatId", chatId)
                    .setParameter("habitId", habitId)
                    .uniqueResult();
            
            if (toRemove != null) {
                session.remove(toRemove);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            System.err.println("Ошибка БД при удалении: " + e.getMessage());
        }
    }

    @Override
    public boolean isEmpty(Long chatId) {
        return size(chatId) == 0;
    }

    @Override
    public int size(Long chatId) {
        try (Session session = sessionFactory.openSession()) {
            Long count = session.createQuery(
                    "SELECT COUNT(h) FROM Habit h WHERE h.chatId = :chatId", Long.class)
                    .setParameter("chatId", chatId)
                    .uniqueResult();
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            System.err.println("Ошибка БД при подсчете: " + e.getMessage());
            return 0;
        }
    }

    @Override
    public Habit getHabit(Long chatId, Long habitId) {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery(
                    "FROM Habit WHERE chatId = :chatId AND id = :habitId", Habit.class)
                    .setParameter("chatId", chatId)
                    .setParameter("habitId", habitId)
                    .uniqueResult();
        } catch (Exception e) {
            System.err.println("Ошибка БД при получении привычки: " + e.getMessage());
            return null;
        }
    }
}