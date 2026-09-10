package moviebooking.service;

import moviebooking.model.Movie;
import moviebooking.model.Screen;
import moviebooking.model.Show;

import java.util.List;
import java.util.Optional;

public interface CatalogService {
    Movie addMovie(Movie movie);
    Screen addScreen(Screen screen);
    Show addShow(Show show);
    List<Show> findShowsForMovie(String movieTitle);
    Optional<Show> getShow(String showId);
}
