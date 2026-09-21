package com.cinepass.show.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cinepass.show.entity.Show;

public interface ShowRepository extends JpaRepository<Show, Long> {

    List<Show> findByMovieId(Long movieId);
}
