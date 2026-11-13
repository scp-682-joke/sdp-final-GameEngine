package config;

public final class GameProject {
    private final String name;
    private final String genre;
    private final String dimension;
    private final ActionConfig actionConfig;
    private final HorrorConfig horrorConfig;

    GameProject(String name, String genre, String dimension, ActionConfig actionConfig, HorrorConfig horrorConfig) {
        this.name = name;
        this.genre = genre;
        this.dimension = dimension;
        this.actionConfig = actionConfig;
        this.horrorConfig = horrorConfig;
    }
    public String getName() { return name; }
    public String getGenre() { return genre; }
    public String getDimension() { return dimension; }
    public ActionConfig getActionConfig() { return actionConfig; }
    public HorrorConfig getHorrorConfig() { return horrorConfig; }
}

