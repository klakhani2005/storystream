package com.portfolio.storystream.config;

import com.portfolio.storystream.model.Audiobook;
import com.portfolio.storystream.repository.AudiobookRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "storystream.seed-demo-data", havingValue = "true")
public class DemoData {

    @Bean
    CommandLineRunner seed(AudiobookRepository repo) {
        return args -> {
            if (repo.count() > 0) {
                return;
            }
            repo.save(new Audiobook("The Martian", "Andy Weir", "Science Fiction", 660,
                    "An astronaut struggles to survive on Mars.", 4.8));
            repo.save(new Audiobook("Project Hail Mary", "Andy Weir", "Science Fiction", 960,
                    "A lone astronaut attempts to save humanity.", 4.9));
            repo.save(new Audiobook("Dune", "Frank Herbert", "Science Fiction", 1260,
                    "A desert planet, a powerful family, and a prophecy.", 4.7));
            repo.save(new Audiobook("Atomic Habits", "James Clear", "Personal Development", 320,
                    "A guide to building better habits.", 4.7));
            repo.save(new Audiobook("The Psychology of Money", "Morgan Housel", "Personal Development", 350,
                    "Lessons on wealth, greed, and happiness.", 4.6));
            repo.save(new Audiobook("Educated", "Tara Westover", "Memoir", 720,
                    "A memoir about education and self-invention.", 4.6));
            repo.save(new Audiobook("Born a Crime", "Trevor Noah", "Memoir", 530,
                    "Stories from a South African childhood.", 4.8));
            repo.save(new Audiobook("Deep Work", "Cal Newport", "Productivity", 420,
                    "Strategies for focused work.", 4.5));
            repo.save(new Audiobook("Getting Things Done", "David Allen", "Productivity", 690,
                    "A system for stress-free productivity.", 4.3));
        };
    }
}
