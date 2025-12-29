package net.murasakiyamaimo;

public class layout extends JFX {
    public boolean isVisible = false;

    public void view() {
        isVisible = !isVisible;
    }

    public String render() {
        return ("""
                <div className="bg-yellow-200 h-96" onClick="view">
                    <div className="text-9xl bg-blue-300 text-amber-500">My name is</div>
                </div>
                """);
    }
}
