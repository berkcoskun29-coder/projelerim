package app.snippets;

public class Snippet {
    private final String title;
    private final String code;

    public Snippet(String title, String code) {
        this.title = title;
        this.code = code;
    }

    public String getTitle() {
        return title;
    }

    public String getCode() {
        return code;
    }

    @Override
    public String toString() {
        return title;
    }
}