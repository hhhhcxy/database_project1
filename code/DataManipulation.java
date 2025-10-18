public interface DataManipulation {

    public int addOneMovie(String str);
    public String allContinentNames();
    public String continentsWithCountryCount();
    public String FullInformationOfMoviesRuntime(int min, int max);
    public String findMovieById(int id);
    public String findMoviesByLimited10(String title);
    public void initDatabase();
    public String findFlightsByDay_op(String day_op);
}
