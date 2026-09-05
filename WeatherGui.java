import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Grafiskt gränssnitt (Swing) för väderappen.
 */
public class WeatherGui {

    private static final Locale SV_LOCALE = Locale.of("sv", "SE");

    private JFrame frame;
    private JLabel tempLabel;
    private JLabel vindLabel;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    private JButton uppdateraKnapp;

    /** Skapar och visar fönstret. */
    public static void visa() {
        try {
            // Använd systemets utseende så att appen ser ut som ett vanligt program i Mint
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Spelar ingen roll – defaultutseendet duger också
        }

        SwingUtilities.invokeLater(() -> new WeatherGui().skapaFoerestraedelse());
    }

    private void skapaFoerestraedelse() {
        frame = new JFrame("Väder – Ryssby");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(0, 10));
        frame.getRootPane().setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        // --- Överst: rubrik + aktuell temperatur ---
        JPanel topp = new JPanel();
        topp.setLayout(new javax.swing.BoxLayout(topp, javax.swing.BoxLayout.Y_AXIS));

        JLabel rubrik = new JLabel("Väder för Ryssby", SwingConstants.CENTER);
        rubrik.setFont(rubrik.getFont().deriveFont(Font.BOLD, 18f));
        rubrik.setAlignmentX(Component.CENTER_ALIGNMENT);

        tempLabel = new JLabel("-- °C", SwingConstants.CENTER);
        tempLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 56));
        tempLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        vindLabel = new JLabel(" ", SwingConstants.CENTER);
        vindLabel.setForeground(new Color(90, 90, 90));
        vindLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        topp.add(rubrik);
        topp.add(tempLabel);
        topp.add(vindLabel);
        frame.add(topp, BorderLayout.NORTH);

        // --- Mitten: 5-dagarsprognosen i en tabell ---
        String[] kolumner = {"Dag", "Max (°C)", "Min (°C)", "Regn (mm)"};
        tableModel = new DefaultTableModel(kolumner, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // tabellen ska inte gå att redigera
            }
        };

        JTable tabell = new JTable(tableModel);
        tabell.setRowHeight(28);
        tabell.setFont(tabell.getFont().deriveFont(14f));
        tabell.getTableHeader().setReorderingAllowed(false);
        tabell.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean selected, boolean focused, int row, int column) {
                super.getTableCellRendererComponent(t, value, selected, focused, row, column);
                setHorizontalAlignment(column == 0 ? SwingConstants.LEFT : SwingConstants.CENTER);
                return this;
            }
        });
        frame.add(new JScrollPane(tabell), BorderLayout.CENTER);

        // --- Nederst: statusrad + uppdateraknapp ---
        JPanel botten = new JPanel(new BorderLayout(8, 0));
        statusLabel = new JLabel("Startar…");
        statusLabel.setForeground(new Color(90, 90, 90));
        uppdateraKnapp = new JButton("Uppdatera");
        uppdateraKnapp.addActionListener(e -> hamtaVader());
        botten.add(statusLabel, BorderLayout.CENTER);
        botten.add(uppdateraKnapp, BorderLayout.EAST);
        frame.add(botten, BorderLayout.SOUTH);

        frame.setSize(440, 480);
        frame.setLocationRelativeTo(null); // centrera på skärmen
        frame.setVisible(true);

        hamtaVader(); // hämta vädret direkt när appen startar
    }

    /** Hämtar väderdatat i bakgrunden så att fönstret inte fryser. */
    private void hamtaVader() {
        uppdateraKnapp.setEnabled(false);
        statusLabel.setText("Hämtar väderdata…");

        new SwingWorker<WeatherService.VaderData, Void>() {
            @Override
            protected WeatherService.VaderData doInBackground() throws Exception {
                return WeatherService.hamtaVader();
            }

            @Override
            protected void done() {
                try {
                    visaVader(get());
                    String tid = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
                    statusLabel.setText("Uppdaterad " + tid);
                } catch (Exception e) {
                    statusLabel.setText("Misslyckades att hämta vädret.");
                    JOptionPane.showMessageDialog(frame,
                            "Kunde inte hämta vädret. Kontrollera din internetanslutning.\n\n"
                                    + e.getMessage(),
                            "Fel", JOptionPane.ERROR_MESSAGE);
                } finally {
                    uppdateraKnapp.setEnabled(true);
                }
            }
        }.execute();
    }

    /** Fyller fönstret med hämtad väderdata. */
    private void visaVader(WeatherService.VaderData vader) {
        tempLabel.setText(formatera(vader.temperatur()) + " °C");
        vindLabel.setText("Vind: " + formatera(vader.vindhastighet()) + " km/h");

        tableModel.setRowCount(0); // töm tabellen
        List<WeatherService.DagsPrognos> dagar = vader.dagar();
        for (int i = 0; i < 5 && i < dagar.size(); i++) {
            WeatherService.DagsPrognos dag = dagar.get(i);
            tableModel.addRow(new Object[] {
                    formateraDag(dag.datum()),
                    formatera(dag.maxTemp()),
                    formatera(dag.minTemp()),
                    formatera(dag.regn())
            });
        }
    }

    /** Formaterar tal med svensk decimal (t.ex. 12,4). */
    private static String formatera(double tal) {
        return String.format(SV_LOCALE, "%.1f", tal);
    }

    /** Formaterar till veckodag och datum (t.ex. "Torsdag 05/09"). */
    private static String formateraDag(LocalDate datum) {
        String dag = datum.format(DateTimeFormatter.ofPattern("EEEE dd/MM", SV_LOCALE));
        return dag.substring(0, 1).toUpperCase(SV_LOCALE) + dag.substring(1);
    }
}