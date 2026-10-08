package net.murasakiyamaimo;

public class layout extends JFX {
    public boolean isVisible = false;

    public void view(String message) {
        System.out.println("Hello: " + message);
    }

    public void view(String message, int count) {
        for (int i = 0; i < count; i++) {
            System.out.println("Hello: " + message);
        }
    }

    public String render() {
        return ("""
                <div className="bg-yellow-200 h-96 w-full p-4">
                    <div className="text-9xl w-full bg-blue-300 rounded-lg p-2">Hello</div>
                    <div onClick="view('World')" className="text-8xl w-full bg-green-200 rounded-lg p-2">Click me! (String arg)</div>
                    <div onClick="=> view('NextJFX', 3)" className="text-8xl w-full bg-blue-200 rounded-lg p-2">Click me! (String and int args)</div>
                </div>
                """);
    }
}