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
 * Grafiskt gränssnitt (Swing) för väderappen.
 */
public class WeatherGui {

    private static final Locale SV_LOCALE = new Locale("sv", "SE");
    private static final Color BLA = new Color(30, 105, 170);
    private static final Color LJUS_BLA = new Color(226, 241, 250);
    private static final Color BAKGRUND = new Color(246, 249, 251);
    private static final Color TEXT = new Color(35, 45, 55);

    /** Namn på klockslagen som visas under timdiagrammet. */
    private static final String[] TIDSNAVN = {
            "Midnatt", "Natt", "Morgon", "Förmiddag", "Lunch", "Eftermiddag", "Kväll", "Sen kväll"
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
        frame.getContentPane().setBackground(BAKGRUND);
        frame.getRootPane().setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        // --- Överst: rubrik + aktuell temperatur ---
        JPanel topp = new JPanel();
        topp.setLayout(new javax.swing.BoxLayout(topp, javax.swing.BoxLayout.Y_AXIS));
        topp.setBackground(LJUS_BLA);
        topp.setBorder(BorderFactory.createEmptyBorder(16, 12, 16, 12));

        JLabel rubrik = new JLabel("Väder för Ryssby", SwingConstants.CENTER);
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
        String[] kolumner = {"Dag", "Max (°C)", "Min (°C)", "Regn (mm)"};
        tableModel = new DefaultTableModel(kolumner, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // tabellen ska inte gå att redigera
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
        // Klicka på en dag i tabellen för att se regnet timme för timme
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

        String[] intervallKolumner = {"Intervall", "Klockslag 1", "Klockslag 2", "Klockslag 3",
            "Klockslag 4", "Klockslag 5", "Klockslag 6"};
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

        skrivUtKnapp = new JButton("Skriv ut intervall");
        skrivUtKnapp.setEnabled(false);
        skrivUtKnapp.addActionListener(e -> skrivUtIntervall());

        JPanel intervallPanel = new JPanel(new BorderLayout(0, 4));
        intervallPanel.setBackground(BAKGRUND);
        intervallPanel.setPreferredSize(new Dimension(0, 245));
        JLabel intervallRubrik = new JLabel("Tidsintervall – varje rad visar sex klockslag");
        intervallRubrik.setFont(intervallRubrik.getFont().deriveFont(Font.BOLD, 13f));
        intervallRubrik.setForeground(TEXT);
        intervallPanel.add(intervallRubrik, BorderLayout.NORTH);
        intervallPanel.add(new JScrollPane(intervallTabell), BorderLayout.CENTER);
        intervallPanel.add(skrivUtKnapp, BorderLayout.SOUTH);

        // Tabell + timdiagram staplade ovanpå varandra
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
        statusLabel = new JLabel("Startar…");
        statusLabel.setForeground(Color.WHITE);
        uppdateraKnapp = new JButton("Uppdatera");
        uppdateraKnapp.setBackground(Color.WHITE);
        uppdateraKnapp.setForeground(BLA);
        uppdateraKnapp.addActionListener(e -> hamtaVader());
        botten.add(statusLabel, BorderLayout.CENTER);
        botten.add(uppdateraKnapp, BorderLayout.EAST);
        frame.add(botten, BorderLayout.SOUTH);

        frame.setSize(700, 850);
        frame.setLocationRelativeTo(null); // centrera på skärmen
        frame.setVisible(true);

        hamtaVader(); // hämta vädret direkt när appen startar

        // Håll vädret aktuellt: uppdatera automatiskt var 10:e minut
        // (open-meteos modeller uppdateras ändå bara ca var 15:e minut)
        autoUppdaterare = new javax.swing.Timer(10 * 60 * 1000, e -> hamtaVader());
        autoUppdaterare.start();
    }

    /** Hämtar väderdatat i bakgrunden så att fönstret inte fryser. */
    private void hamtaVader() {
        // Starta om nedräkningen för automatisk uppdatering
        if (autoUppdaterare != null) {
            autoUppdaterare.restart();
        }

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
        senasteData = vader;
        tempLabel.setText(formatera(vader.temperatur()) + " °C");
        vindLabel.setText("Vind: " + formatera(vader.vindhastighet()) + " m/s");

        tableModel.setRowCount(0); // töm tabellen
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

        // Uppdatera timdiagrammet med vald dag (välj första dagen som standard)
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

    /** Formaterar till veckodag och datum (t.ex. "Torsdag 05/09"). */
    private static String formateraDag(LocalDate datum) {
        String dag = datum.format(DateTimeFormatter.ofPattern("EEEE dd/MM", SV_LOCALE));
        return dag.substring(0, 1).toUpperCase(SV_LOCALE) + dag.substring(1);
    }

    /** Returnerar nederbörden per timme (24 värden) för en viss dag. */
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

    /** Visar alla timvärden samlade i natt-, morgon-, eftermiddags- och kvällsintervall. */
    private void visaIntervall(LocalDate dag, double[] regn) {
        valdDag = dag;
        intervallModel.setRowCount(0);
        String[] namn = {"Natt (00–06)", "Morgon (06–12)", "Eftermiddag (12–18)", "Kväll (18–24)"};
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

    /** Returnerar true när en intervallcell innehåller mer än 0,0 mm regn. */
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
                    new java.text.MessageFormat("Väderintervall – " + formateraDag(valdDag)),
                    null);
            if (!utskriven) {
                statusLabel.setText("Utskriften avbröts.");
            }
        } catch (java.awt.print.PrinterException e) {
            JOptionPane.showMessageDialog(frame,
                    "Kunde inte skriva ut intervallen.\n\n" + e.getMessage(),
                    "Utskriftsfel", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Ritar ett enkelt stapeldiagram med nederbörden timme för timme. */
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
                g.drawString("Välj en dag i tabellen ovan för att se regnet timme för timme.", 12, 22);
                return;
            }

            double maxMm = 0;
            double summa = 0;
            for (double v : regn) {
                maxMm = Math.max(maxMm, v);
                summa += v;
            }

            // Rubrik med dagens total (använd temats textfärg så att den
            // syns i både mörka och ljusa teman)
            g.setColor(getForeground());
            g.setFont(g.getFont().deriveFont(Font.BOLD, 16f));
            g.drawString("Nederbörd per timme – " + formateraDag(dag)
                    + String.format(SV_LOCALE, "   (totalt %.1f mm)", summa), 12, 20);
            // Nederbördssiffrorna ovanför staplarna ska vara extra tydliga.
            g.setFont(g.getFont().deriveFont(20f));

            int botten = hojd - 22;         // staplarna står på denna linje
            int maxBarHojd = botten - 34;   // utrymme för staplarna
            int steg = Math.max(1, (bredd - 24) / regn.length);
            int stapelBredd = Math.max(3, steg - 2);

            // Baslinjen under staplarna
            g.setColor(new Color(200, 200, 200));
            g.drawLine(12, botten, bredd - 12, botten);
            g.setFont(g.getFont().deriveFont(13f));

            // Etiketterna varvar mellan två nivåer när timmar med regn
            // ligger bredvid varandra, så att siffrorna inte krockar
            int forraRegnTimme = -2;
            boolean hogNiva = false;

            for (int i = 0; i < regn.length; i++) {
                int x = 12 + i * steg;
                int bh = maxMm > 0 ? (int) Math.round(regn[i] / maxMm * maxBarHojd) : 0;

                if (bh > 0) {
                    // Mörkare blå för kraftigt regn (>= 1 mm)
                    g.setColor(regn[i] >= 1.0 ? new Color(41, 98, 255) : new Color(108, 160, 220));
                    g.fillRect(x + 1, botten - bh, stapelBredd, bh);

                    // Antal mm ovanför stapeln där det är nederbörd
                    // (hela tal från 10 mm, annars en decimal)
                    String antal = regn[i] >= 10 ? String.format(SV_LOCALE, "%.0f", regn[i])
                                                 : String.format(SV_LOCALE, "%.1f", regn[i]);
                    hogNiva = (i == forraRegnTimme + 1) ? !hogNiva : false;
                    forraRegnTimme = i;

                    int xText = x + stapelBredd / 2 + 1 - g.getFontMetrics().stringWidth(antal) / 2;
                    int yText = Math.max(botten - bh - (hogNiva ? 17 : 5), 34);
                    g.setColor(getForeground());
                    g.drawString(antal, xText, yText);
                }

                // Skriv ut tidsnamn var tredje timme, i två rader så att
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
                g.drawString("Ingen nederbörd den här dagen ☀", 12, botten - maxBarHojd / 2);
            }
        }
    }
}