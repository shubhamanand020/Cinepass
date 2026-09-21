package com.cinepass.show.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cinepass.show.entity.Screen;

public interface ScreenRepository extends JpaRepository<Screen, Long> {
}
