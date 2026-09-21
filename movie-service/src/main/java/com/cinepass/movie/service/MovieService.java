package com.cinepass.movie.service;

import com.cinepass.movie.dto.MovieRequest;
import com.cinepass.movie.dto.MovieResponse;
import com.cinepass.movie.entity.Movie;
import com.cinepass.movie.repository.MovieRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MovieService {

    private final MovieRepository movieRepository;

    public MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    public MovieResponse createMovie(MovieRequest request) {

        Movie movie = new Movie();

        movie.setTitle(request.getTitle());
        movie.setDescription(request.getDescription());
        movie.setGenre(request.getGenre());
        movie.setLanguage(request.getLanguage());
        movie.setDuration(request.getDuration());
        movie.setReleaseDate(request.getReleaseDate());
        movie.setPosterUrl(request.getPosterUrl());

        Movie savedMovie = movieRepository.save(movie);

        return mapToResponse(savedMovie);
    }

    public List<MovieResponse> getAllMovies() {

        return movieRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public MovieResponse getMovieById(Long id) {

        Movie movie = movieRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Movie not found with id: " + id)
                );

        return mapToResponse(movie);
    }

    public MovieResponse updateMovie(Long id, MovieRequest request) {

        Movie movie = movieRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Movie not found with id: " + id)
                );

        movie.setTitle(request.getTitle());
        movie.setDescription(request.getDescription());
        movie.setGenre(request.getGenre());
        movie.setLanguage(request.getLanguage());
        movie.setDuration(request.getDuration());
        movie.setReleaseDate(request.getReleaseDate());
        movie.setPosterUrl(request.getPosterUrl());

        Movie updatedMovie = movieRepository.save(movie);

        return mapToResponse(updatedMovie);
    }

    public void deleteMovie(Long id) {

        if (!movieRepository.existsById(id)) {
            throw new RuntimeException("Movie not found with id: " + id);
        }

        movieRepository.deleteById(id);
    }

    private MovieResponse mapToResponse(Movie movie) {

        return new MovieResponse(
                movie.getId(),
                movie.getTitle(),
                movie.getDescription(),
                movie.getGenre(),
                movie.getLanguage(),
                movie.getDuration(),
                movie.getReleaseDate(),
                movie.getPosterUrl()
        );
    }
}