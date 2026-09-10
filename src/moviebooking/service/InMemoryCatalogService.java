package moviebooking.service;

import moviebooking.model.Movie;
import moviebooking.model.Screen;
import moviebooking.model.Show;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class InMemoryCatalogService implements CatalogService {
    private final Map<String, Movie> movies = new LinkedHashMap<>();
    private final Map<String, Screen> screens = new LinkedHashMap<>();
    private final Map<String, Show> shows = new LinkedHashMap<>();

    @Override
    public Movie addMovie(Movie movie) { movies.put(movie.getId(), movie); return movie; }

    @Override
    public Screen addScreen(Screen screen) { screens.put(screen.getId(), screen); return screen; }

    @Override
    public Show addShow(Show show) { shows.put(show.getId(), show); return show; }

    @Override
    public List<Show> findShowsForMovie(String movieTitle) {
        return shows.values().stream()
                .filter(s -> s.getMovie().getTitle().equalsIgnoreCase(movieTitle))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Show> getShow(String showId) {
        return Optional.ofNullable(shows.get(showId));
    }
}
