package com.ucamp.movieus.service;

import com.ucamp.movieus.dto.MovieDTO;
import com.ucamp.movieus.dto.TMDBResponse;
import com.ucamp.movieus.entity.Genre;
import com.ucamp.movieus.entity.Movie;
import com.ucamp.movieus.repository.GenreRepository;
import com.ucamp.movieus.repository.MovieRepository;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final RestTemplate restTemplate;
    private final String API_KEY = "40405429a36ddf7b1d4337a022992fbc";
    private final String BASE_URL = "https://api.themoviedb.org/3/movie/";

    @PostConstruct
    public void init() {
        fetchAndSaveNowPlayingMovies(); // 애플리케이션 시작 시 데이터 로딩
    }

    public void fetchAndSaveNowPlayingMovies() {
        List<Movie> movies = fetchMoviesFromApi();
        List<Movie> existingMovies = movieRepository.findAll();

        Map<Long, Movie> existingMovieMap = existingMovies.stream()
                .collect(Collectors.toMap(Movie::getTmdbId, Function.identity()));

        Set<Long> currentTmdbIds = new HashSet<>();

        // 랭킹 설정
        int rank = 1; // 랭킹은 1부터 시작

        for (Movie movie : movies) {
            currentTmdbIds.add(movie.getTmdbId());
            movie.setRanking(rank++); // 랭킹 설정

            if (existingMovieMap.containsKey(movie.getTmdbId())) {
                Movie existingMovie = existingMovieMap.get(movie.getTmdbId());
                updateMovie(existingMovie, movie);
            } else {
                movieRepository.save(movie);
            }
        }

        // 상영 종료된 영화 처리 (예: 상태 변경 또는 삭제)
        List<Movie> moviesToDelete = existingMovies.stream()
                .filter(movie -> !currentTmdbIds.contains(movie.getTmdbId()))
                .collect(Collectors.toList());
        movieRepository.deleteAll(moviesToDelete);
    }

    private List<Movie> fetchMoviesFromApi() {
        TMDBResponse response;
        int page = 1;
        List<Movie> allMovies = new ArrayList<>();

        do {
            String url = String.format(
                    "%snow_playing?api_key=%s&region=KR&language=ko&sort_by=popularity.desc&page=%d",
                    BASE_URL, API_KEY, page);
            response = restTemplate.getForObject(url, TMDBResponse.class);

            if (response != null && response.getResults() != null) {
                List<Movie> movies = response.toMovies(genreRepository); // GenreRepository를 사용하여 Movie로 변환
                allMovies.addAll(movies);
                page++;
            }
        } while (response != null && page <= response.getTotalPages());

        return allMovies;
    }

    // 기존 Movie 객체 업데이트
    private void updateMovie(Movie existingMovie, Movie newMovie) {
        existingMovie.setTitle(newMovie.getTitle());
        existingMovie.setOriginalTitle(newMovie.getOriginalTitle());
        existingMovie.setOverview(newMovie.getOverview());
        existingMovie.setPosterPath(newMovie.getPosterPath());
        existingMovie.setBackdropPath(newMovie.getBackdropPath());
        existingMovie.setPopularity(newMovie.getPopularity());
        existingMovie.setVoteAverage(newMovie.getVoteAverage());
        existingMovie.setVoteCount(newMovie.getVoteCount());
        existingMovie.setReleaseDate(newMovie.getReleaseDate());
        existingMovie.setGenres(newMovie.getGenres());
    }

    public Movie getMovie(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Movie not found with ID: " + id));
    }

    public List<Movie> getMoviesByGenreName(String genreName) {
        return genreRepository.findByGenreName(genreName);
    }

    public Object getMovieCredits(Long movieId) {
        String url = BASE_URL + movieId + "/credits?api_key=" + API_KEY + "&language=ko-KR";
        return restTemplate.getForObject(url, Object.class);
    }

    public Object getMovieDetail(Long movieId) {
        String url = BASE_URL + movieId + "?api_key=" + API_KEY + "&language=ko-KR";
        return restTemplate.getForObject(url, Object.class);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getPopularMovies() {
        List<Map<String, Object>> moviesWithDbInfo = new ArrayList<>();

        // DB에 저장된 영화의 TMDB ID 목록 가져오기
        List<Integer> dbMovieIds = movieRepository.findAllTmdbIds();
        Set<Integer> dbMovieIdSet = new HashSet<>(dbMovieIds);

        // TMDB API에서 인기 영화 목록 가져오기
        String url = BASE_URL + "popular?api_key=" + API_KEY + "&language=ko-KR&region=KR";

        // TMDB API 요청
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        List<Map<String, Object>> movies = (List<Map<String, Object>>) response.get("results");

        // API에서 가져온 영화 목록에 DB 존재 여부 표시
        for (Map<String, Object> movie : movies) {
            Integer tmdbId = (Integer) movie.get("id");
            boolean existsInDb = dbMovieIdSet.contains(tmdbId);

            // exists_in_db 필드 추가
            movie.put("exists_in_db", existsInDb);
            moviesWithDbInfo.add(movie);
        }

        return moviesWithDbInfo;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAllPopularMovies() {
        List<Map<String, Object>> moviesWithDbInfo = new ArrayList<>();

        // DB에 저장된 영화의 TMDB ID 목록 가져오기
        List<Integer> dbMovieIds = movieRepository.findAllTmdbIds();
        Set<Integer> dbMovieIdSet = new HashSet<>(dbMovieIds);

        // 1페이지부터 5페이지까지 TMDB API에서 인기 영화 목록 가져오기
        for (int page = 1; page <= 5; page++) {
            String url = BASE_URL + "popular?api_key=" + API_KEY + "&language=ko-KR&region=KR&page=" + page;

            // TMDB API 요청
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            List<Map<String, Object>> movies = (List<Map<String, Object>>) response.get("results");

            // API에서 가져온 영화 목록에 DB 존재 여부 표시
            for (Map<String, Object> movie : movies) {
                Integer tmdbId = (Integer) movie.get("id");
                boolean existsInDb = dbMovieIdSet.contains(tmdbId);

                // exists_in_db 필드 추가
                movie.put("exists_in_db", existsInDb);
                if(!existsInDb){
                    moviesWithDbInfo.add(movie);
                }
            }
        }

        return moviesWithDbInfo;
    }

    public List<Map<String, Object>> getPopularMoviesByGenre(String genreName) {
        List<Map<String, Object>> moviesWithDbInfo = new ArrayList<>();

        // DB에서 TMDB ID 목록 가져오기
        List<Integer> dbMovieIds = movieRepository.findAllTmdbIds();
        Set<Integer> dbMovieIdSet = new HashSet<>(dbMovieIds);

        // DB에서 장르명으로 TMDB 장르 ID 조회
        Integer genreId = genreRepository.findIdByName(genreName);
        if (genreId == null) {
            throw new IllegalArgumentException("해당 장르를 찾을 수 없습니다: " + genreName);
        }

        // TMDB API 요청 (1~5페이지)
        for (int page = 1; page <= 5; page++) {
            String url = "https://api.themoviedb.org/3/discover/movie"
                    + "?api_key=" + API_KEY
                    + "&language=ko-KR"
                    + "&region=KR"
                    + "&with_genres=" + genreId
                    + "&page=" + page;

            // TMDB API 요청
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            List<Map<String, Object>> movies = (List<Map<String, Object>>) response.get("results");

            // API에서 가져온 영화 목록에 DB 존재 여부 표시
            for (Map<String, Object> movie : movies) {
                Integer tmdbId = (Integer) movie.get("id");
                boolean existsInDb = dbMovieIdSet.contains(tmdbId);

                // exists_in_db 필드 추가
                movie.put("exists_in_db", existsInDb);
                if(!existsInDb){
                    moviesWithDbInfo.add(movie);
                }
            }
        }

        return moviesWithDbInfo;
    }

}