import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
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
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Swing interface for the weather app.
 */
public class WeatherGui {

    private static final Locale SV_LOCALE = new Locale("en", "GB");
    private static final Color BLA = new Color(30, 105, 170);
    private static final Color LJUS_BLA = new Color(226, 241, 250);
    private static final Color BAKGRUND = new Color(246, 249, 251);
    private static final Color TEXT = new Color(35, 45, 55);

    /** The names of the hours shown under the hourly chart. */
    private static final String[] TIDSNAVN = {
            "Midnight", "Night", "Morning", "Late morning", "Noon", "Afternoon", "Evening", "Late evening"
    };

    private JFrame frame;
    private JLabel tempLabel;
    private JLabel vindLabel;
    private DefaultTableModel tableModel;
    private JTable tabell;
    private WeatherService.VaderData senasteData;
    private RegnPanel regnPanel;
    private DefaultTableModel intervallModel;
    private JTable intervallTabell;
    private JButton skrivUtKnapp;
    private LocalDate valdDag;
    private JLabel statusLabel;
    private JButton uppdateraKnapp;
    private javax.swing.Timer autoUppdaterare;

    /** Creates and shows the window. */
    public static void visa() {
        try {
            // Use the system look so the app looks like a normal program in Mint
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Does not matter — the default look is fine too
        }

        SwingUtilities.invokeLater(() -> new WeatherGui().skapaFoerestraedelse());
    }

    private void skapaFoerestraedelse() {
        frame = new JFrame("Weather – Ryssby");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(0, 10));
        frame.getContentPane().setBackground(BAKGRUND);
        frame.getRootPane().setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        // --- Top: heading and current temperature ---
        JPanel topp = new JPanel();
        topp.setLayout(new javax.swing.BoxLayout(topp, javax.swing.BoxLayout.Y_AXIS));
        topp.setBackground(LJUS_BLA);
        topp.setBorder(BorderFactory.createEmptyBorder(16, 12, 16, 12));

        JLabel rubrik = new JLabel("Weather for Ryssby", SwingConstants.CENTER);
        rubrik.setFont(rubrik.getFont().deriveFont(Font.BOLD, 18f));
        rubrik.setForeground(BLA);
        rubrik.setAlignmentX(Component.CENTER_ALIGNMENT);

        tempLabel = new JLabel("-- °C", SwingConstants.CENTER);
        tempLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 68));
        tempLabel.setForeground(TEXT);
        tempLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        vindLabel = new JLabel(" ", SwingConstants.CENTER);
        vindLabel.setFont(vindLabel.getFont().deriveFont(Font.BOLD, 22f));
        vindLabel.setForeground(TEXT);
        vindLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        topp.add(rubrik);
        topp.add(tempLabel);
        topp.add(vindLabel);
        frame.add(topp, BorderLayout.NORTH);

        // --- Mitten: 5-dagarsprognosen i en tabell ---
        String[] kolumner = {"Day", "Max (°C)", "Min (°C)", "Rain (mm)"};
        tableModel = new DefaultTableModel(kolumner, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // the table must not be editable
            }
        };

        tabell = new JTable(tableModel);
        tabell.setRowHeight(40);
        tabell.setFont(tabell.getFont().deriveFont(18f));
        tabell.getTableHeader().setFont(tabell.getTableHeader().getFont().deriveFont(17f));
        tabell.getTableHeader().setReorderingAllowed(false);
        tabell.setSelectionBackground(BLA);
        tabell.setSelectionForeground(Color.WHITE);
        tabell.getTableHeader().setBackground(BLA);
        tabell.getTableHeader().setForeground(Color.WHITE);
        tabell.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean selected, boolean focused, int row, int column) {
                super.getTableCellRendererComponent(t, value, selected, focused, row, column);
                setHorizontalAlignment(column == 0 ? SwingConstants.LEFT : SwingConstants.CENTER);
                if (!selected) {
                    setBackground(row % 2 == 0 ? Color.WHITE : new Color(238, 244, 248));
                    setForeground(TEXT);
                }
                return this;
            }
        });
        // Click a day in the table to see the rain hour by hour
        tabell.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabell.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && senasteData != null) {
                int rad = tabell.getSelectedRow();
                if (rad >= 0 && rad < senasteData.dagar().size()) {
                    WeatherService.DagsPrognos dag = senasteData.dagar().get(rad);
                    double[] regn = regnForDag(dag.datum());
                    regnPanel.visa(dag.datum(), regn);
                    visaIntervall(dag.datum(), regn);
                }
            }
        });

        regnPanel = new RegnPanel();
        regnPanel.setPreferredSize(new Dimension(0, 290));
        regnPanel.setBackground(Color.WHITE);
        regnPanel.setForeground(TEXT);

        String[] intervallKolumner = {"Band", "Hour 1", "Hour 2", "Hour 3",
            "Hour 4", "Hour 5", "Hour 6"};
        intervallModel = new DefaultTableModel(intervallKolumner, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        intervallTabell = new JTable(intervallModel);
        intervallTabell.setRowHeight(38);
        intervallTabell.setFont(intervallTabell.getFont().deriveFont(17f));
        intervallTabell.getTableHeader().setFont(intervallTabell.getTableHeader().getFont().deriveFont(16f));
        intervallTabell.setSelectionBackground(BLA);
        intervallTabell.setSelectionForeground(Color.WHITE);
        intervallTabell.getTableHeader().setBackground(BLA);
        intervallTabell.getTableHeader().setForeground(Color.WHITE);
        intervallTabell.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean selected, boolean focused, int row, int column) {
                super.getTableCellRendererComponent(table, value, selected, focused, row, column);
                setHorizontalAlignment(column == 0 ? SwingConstants.LEFT : SwingConstants.CENTER);
                if (!selected) {
                    boolean harRegn = column > 0 && value != null && harNederbord(value.toString());
                    setBackground(harRegn ? new Color(82, 180, 245)
                            : (row % 2 == 0 ? Color.WHITE : new Color(238, 244, 248)));
                    setForeground(harRegn ? new Color(8, 55, 105) : TEXT);
                    setFont(getFont().deriveFont(harRegn ? Font.BOLD : Font.PLAIN));
                }
                return this;
            }
        });
        intervallTabell.getColumnModel().getColumn(0).setPreferredWidth(115);
        for (int kolumn = 1; kolumn < intervallKolumner.length; kolumn++) {
            intervallTabell.getColumnModel().getColumn(kolumn).setPreferredWidth(58);
        }

        skrivUtKnapp = new JButton("Print bands");
        skrivUtKnapp.setEnabled(false);
        skrivUtKnapp.addActionListener(e -> skrivUtIntervall());

        JPanel intervallPanel = new JPanel(new BorderLayout(0, 4));
        intervallPanel.setBackground(BAKGRUND);
        intervallPanel.setPreferredSize(new Dimension(0, 245));
        JLabel intervallRubrik = new JLabel("Time bands – each row shows six hours");
        intervallRubrik.setFont(intervallRubrik.getFont().deriveFont(Font.BOLD, 13f));
        intervallRubrik.setForeground(TEXT);
        intervallPanel.add(intervallRubrik, BorderLayout.NORTH);
        intervallPanel.add(new JScrollPane(intervallTabell), BorderLayout.CENTER);
        intervallPanel.add(skrivUtKnapp, BorderLayout.SOUTH);

        // Table and hourly chart stacked on top of each other
        JPanel mitten = new JPanel();
        mitten.setLayout(new javax.swing.BoxLayout(mitten, javax.swing.BoxLayout.Y_AXIS));
        mitten.setBackground(BAKGRUND);
        JScrollPane tabellVy = new JScrollPane(tabell);
        tabellVy.setPreferredSize(new Dimension(0, 225));
        mitten.add(tabellVy);
        mitten.add(regnPanel);
        mitten.add(intervallPanel);
        JScrollPane innehallVy = new JScrollPane(mitten,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        innehallVy.getVerticalScrollBar().setUnitIncrement(24);
        frame.add(innehallVy, BorderLayout.CENTER);

        // --- Nederst: statusrad + uppdateraknapp ---
        JPanel botten = new JPanel(new BorderLayout(8, 0));
        botten.setBackground(BLA);
        botten.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        statusLabel = new JLabel("Starting…");
        statusLabel.setForeground(Color.WHITE);
        uppdateraKnapp = new JButton("Refresh");
        uppdateraKnapp.setBackground(Color.WHITE);
        uppdateraKnapp.setForeground(BLA);
        uppdateraKnapp.addActionListener(e -> hamtaVader());
        botten.add(statusLabel, BorderLayout.CENTER);
        botten.add(uppdateraKnapp, BorderLayout.EAST);
        frame.add(botten, BorderLayout.SOUTH);

        frame.setSize(700, 850);
        frame.setLocationRelativeTo(null); // centre on screen
        frame.setVisible(true);

        hamtaVader(); // fetch the weather as soon as the app starts

        // Keep the weather current: refresh automatically every 10 minutes
        // (open-meteo's models are only updated roughly every 15 minutes anyway)
        autoUppdaterare = new javax.swing.Timer(10 * 60 * 1000, e -> hamtaVader());
        autoUppdaterare.start();
    }

    /** Fetches the weather data in the background so the window does not freeze. */
    private void hamtaVader() {
        // Restart the countdown for the automatic refresh
        if (autoUppdaterare != null) {
            autoUppdaterare.restart();
        }

        uppdateraKnapp.setEnabled(false);
        statusLabel.setText("Fetching weather data…");

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
                    statusLabel.setText("Updated " + tid);
                } catch (Exception e) {
                    statusLabel.setText("Failed to fetch the weather.");
                    JOptionPane.showMessageDialog(frame,
                            "Could not fetch the weather. Check your internet connection.\n\n"
                                    + e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    uppdateraKnapp.setEnabled(true);
                }
            }
        }.execute();
    }

    /** Fills the window with the fetched weather data. */
    private void visaVader(WeatherService.VaderData vader) {
        senasteData = vader;
        tempLabel.setText(formatera(vader.temperatur()) + " °C");
        vindLabel.setText("Wind: " + formatera(vader.vindhastighet()) + " m/s");

        tableModel.setRowCount(0); // clear the table
        List<WeatherService.DagsPrognos> dagar = vader.dagar();
        for (int i = 0; i < 7 && i < dagar.size(); i++) {
            WeatherService.DagsPrognos dag = dagar.get(i);
            tableModel.addRow(new Object[] {
                    formateraDag(dag.datum()),
                    formatera(dag.maxTemp()),
                    formatera(dag.minTemp()),
                    formatera(dag.regn())
            });
        }

        // Update the hourly chart with the selected day (the first day by default)
        int rad = tabell.getSelectedRow();
        if ((rad < 0 || rad >= dagar.size()) && !dagar.isEmpty()) {
            tabell.setRowSelectionInterval(0, 0);
            rad = 0;
        }
        if (rad >= 0 && rad < dagar.size()) {
            WeatherService.DagsPrognos dag = dagar.get(rad);
            double[] regn = regnForDag(dag.datum());
            regnPanel.visa(dag.datum(), regn);
            visaIntervall(dag.datum(), regn);
        }
    }

    /** Formaterar tal med svensk decimal (t.ex. 12,4). */
    private static String formatera(double tal) {
        return String.format(SV_LOCALE, "%.1f", tal);
    }

    /** Formaterar till veckodag och datum (t.ex. "Thursday 05/09"). */
    private static String formateraDag(LocalDate datum) {
        String dag = datum.format(DateTimeFormatter.ofPattern("EEEE dd/MM", SV_LOCALE));
        return dag.substring(0, 1).toUpperCase(SV_LOCALE) + dag.substring(1);
    }

    /** Returns the precipitation per hour (24 values) for a given day. */
    private double[] regnForDag(LocalDate dag) {
        double[] res = new double[24];
        if (senasteData != null) {
            for (WeatherService.TimmarsNederbord t : senasteData.timmar()) {
                if (t.tid().toLocalDate().equals(dag)) {
                    res[t.tid().getHour()] = t.mm();
                }
            }
        }
        return res;
    }

    /** Shows all hourly values grouped into night, morning, afternoon and evening bands. */
    private void visaIntervall(LocalDate dag, double[] regn) {
        valdDag = dag;
        intervallModel.setRowCount(0);
        String[] namn = {"Night (00–06)", "Morning (06–12)", "Afternoon (12–18)", "Evening (18–24)"};
        for (int intervall = 0; intervall < 4; intervall++) {
            int start = intervall * 6;
            Object[] rad = new Object[7];
            rad[0] = namn[intervall];
            for (int timme = start; timme < start + 6; timme++) {
                rad[timme - start + 1] = String.format(SV_LOCALE,
                        "%02d:00  %.1f mm", timme, regn[timme]);
            }
            intervallModel.addRow(rad);
        }
        skrivUtKnapp.setEnabled(true);
    }

    /** Returns true when a band cell holds more than 0.0 mm of rain. */
    private static boolean harNederbord(String text) {
        try {
            String mm = text.replace("mm", "").trim().replace(',', '.');
            return Double.parseDouble(mm) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** Skriver ut den valda dagens samlade intervalltabell. */
    private void skrivUtIntervall() {
        if (valdDag == null) {
            return;
        }
        try {
            boolean utskriven = intervallTabell.print(
                    JTable.PrintMode.FIT_WIDTH,
                    new java.text.MessageFormat("Weather bands – " + formateraDag(valdDag)),
                    null);
            if (!utskriven) {
                statusLabel.setText("Printing cancelled.");
            }
        } catch (java.awt.print.PrinterException e) {
            JOptionPane.showMessageDialog(frame,
                    "Could not print the bands.\n\n" + e.getMessage(),
                    "Print error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Draws a simple bar chart with the precipitation hour by hour. */
    private class RegnPanel extends JPanel {
        private LocalDate dag;
        private double[] regn = new double[0];

        void visa(LocalDate dag, double[] regn) {
            this.dag = dag;
            this.regn = regn;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int bredd = getWidth();
            int hojd = getHeight();

            if (regn.length == 0) {
                g.setColor(getForeground());
                g.drawString("Pick a day in the table above to see the rain hour by hour.", 12, 22);
                return;
            }

            double maxMm = 0;
            double summa = 0;
            for (double v : regn) {
                maxMm = Math.max(maxMm, v);
                summa += v;
            }

            // Heading with the day's total (use the theme's text colour so that it
            // shows in both dark and light themes)
            g.setColor(getForeground());
            g.setFont(g.getFont().deriveFont(Font.BOLD, 16f));
            g.drawString("Precipitation per hour – " + formateraDag(dag)
                    + String.format(SV_LOCALE, "   (totalt %.1f mm)", summa), 12, 20);
            // The precipitation numbers above the bars should be extra clear.
            g.setFont(g.getFont().deriveFont(20f));

            int botten = hojd - 22;         // the bars stand on this line
            int maxBarHojd = botten - 34;   // room for the bars
            int steg = Math.max(1, (bredd - 24) / regn.length);
            int stapelBredd = Math.max(3, steg - 2);

            // Baslinjen under staplarna
            g.setColor(new Color(200, 200, 200));
            g.drawLine(12, botten, bredd - 12, botten);
            g.setFont(g.getFont().deriveFont(13f));

            // The labels alternate between two levels when hours with rain
            // sit next to each other, so the numbers do not collide
            int forraRegnTimme = -2;
            boolean hogNiva = false;

            for (int i = 0; i < regn.length; i++) {
                int x = 12 + i * steg;
                int bh = maxMm > 0 ? (int) Math.round(regn[i] / maxMm * maxBarHojd) : 0;

                if (bh > 0) {
                    // Darker blue for heavy rain (>= 1 mm)
                    g.setColor(regn[i] >= 1.0 ? new Color(41, 98, 255) : new Color(108, 160, 220));
                    g.fillRect(x + 1, botten - bh, stapelBredd, bh);

                    // Number of mm above the bar where there is precipitation
                    // (whole numbers from 10 mm, otherwise one decimal)
                    String antal = regn[i] >= 10 ? String.format(SV_LOCALE, "%.0f", regn[i])
                                                 : String.format(SV_LOCALE, "%.1f", regn[i]);
                    hogNiva = (i == forraRegnTimme + 1) ? !hogNiva : false;
                    forraRegnTimme = i;

                    int xText = x + stapelBredd / 2 + 1 - g.getFontMetrics().stringWidth(antal) / 2;
                    int yText = Math.max(botten - bh - (hogNiva ? 17 : 5), 34);
                    g.setColor(getForeground());
                    g.drawString(antal, xText, yText);
                }

                // Print the hour name every third hour, on two lines so that
                // namnen inte krockar med varandra
                if (i % 3 == 0) {
                    g.setColor(getForeground());
                    String namn = TIDSNAVN[i / 3];
                    int xText = x + steg / 2 - g.getFontMetrics().stringWidth(namn) / 2;
                    if (i % 6 == 0) {
                        g.drawString(namn, xText, hojd - 16);
                    } else {
                        g.drawString(namn, xText, hojd - 4);
                    }
                }
            }

            if (summa == 0) {
                g.setColor(getForeground());
                g.drawString("No precipitation this day ☀", 12, botten - maxBarHojd / 2);
            }
        }
    }
}