import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {
        // Kör med argumentet "--console" för det gamla textläget,
        // annars startar appen med grafiskt fönster.
        if (args.length > 0 && args[0].equalsIgnoreCase("--console")) {
            try {
                WeatherService.skrivKonsol(WeatherService.hamtaVader());
            } catch (Exception e) {
                System.out.println("Ett fel uppstod vid anropet: " + e.getMessage());
            }
        } else {
            SwingUtilities.invokeLater(() -> WeatherGui.visa());
        }
    }
}