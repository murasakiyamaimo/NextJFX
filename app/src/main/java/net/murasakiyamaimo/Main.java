package net.murasakiyamaimo;

public class Main {
    public static void main(String[] args) {
        Settings settings = new Settings();
        Application app = new Application();
        app.launch(new layout(), settings);
    }
}
