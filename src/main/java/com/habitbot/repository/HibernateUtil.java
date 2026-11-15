package com.habitbot.repository;

import com.habitbot.model.Habit;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {

    public SessionFactory buildSessionFactory() {
        try {
            Configuration cfg = new Configuration();
            cfg.setProperty("hibernate.connection.driver_class", "org.postgresql.Driver");
            cfg.setProperty("hibernate.connection.url",
                    System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/habitbot?user=postgres&password=postgress"));

            cfg.setProperty("hibernate.hbm2ddl.auto", "update");
            cfg.setProperty("hibernate.show_sql", "false");

            cfg.addAnnotatedClass(Habit.class);
            return cfg.buildSessionFactory();
        } catch (Throwable ex) {
            throw new RuntimeException("Ошибка инициализации Hibernate: " + ex, ex);
        }
    }
}