import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {
        // Run with the argument "--console" for the old console mode,
        // otherwise the app starts in a graphical window.
        if (args.length > 0 && args[0].equalsIgnoreCase("--console")) {
            try {
                WeatherService.skrivKonsol(WeatherService.hamtaVader());
            } catch (Exception e) {
                System.out.println("An error occurred during the request: " + e.getMessage());
            }
        } else {
            SwingUtilities.invokeLater(() -> WeatherGui.visa());
        }
    }
}