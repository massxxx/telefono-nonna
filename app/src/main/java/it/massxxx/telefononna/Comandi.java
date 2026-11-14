package it.massxxx.telefononna;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Formato degli SMS: "NONNA 1234 SQUILLA" (comandi) e "NONNA> ..." (risposte). */
public final class Comandi {
    public static final String PREFISSO = "NONNA";
    public static final String RISPOSTA = "NONNA>";

    public static final String SQUILLA = "SQUILLA";
    public static final String STOP = "STOP";
    public static final String DOVE = "DOVE";
    public static final String STATO = "STATO";

    private static final Pattern COORD = Pattern.compile("q=(-?\\d+(?:\\.\\d+)?),(-?\\d+(?:\\.\\d+)?)");

    private Comandi() {}

    public static String crea(String comando, String pin) {
        return PREFISSO + " " + pin + " " + comando;
    }

    /** Restituisce il comando se il testo è valido e il PIN è giusto, altrimenti null. */
    public static String leggi(String testo, String pin) {
        if (testo == null || pin == null || pin.trim().isEmpty()) return null;
        String[] p = testo.trim().toUpperCase(Locale.ROOT).split("\\s+");
        if (p.length != 3 || !p[0].equals(PREFISSO)) return null;
        String giusto = pin.trim().toUpperCase(Locale.ROOT);
        String parola;
        if (p[1].equals(giusto)) parola = p[2];
        else if (p[2].equals(giusto)) parola = p[1];
        else return null;
        switch (parola) {
            case "SQUILLA": case "SUONA": case "SQUILLO": return SQUILLA;
            case "STOP": case "FERMA": case "BASTA": return STOP;
            case "DOVE": case "POSIZIONE": case "GPS": return DOVE;
            case "STATO": case "TEST": return STATO;
            default: return null;
        }
    }

    public static boolean eRisposta(String testo) {
        return testo != null && testo.trim().toUpperCase(Locale.ROOT).startsWith(RISPOSTA);
    }

    /** Latitudine e longitudine da un link "maps.google.com/?q=lat,lon", oppure null. */
    public static double[] coordinate(String testo) {
        if (testo == null) return null;
        Matcher m = COORD.matcher(testo);
        if (!m.find()) return null;
        try {
            return new double[]{Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2))};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Confronta due numeri ignorando spazi e prefisso internazionale (ultime 9 cifre). */
    public static boolean stessoNumero(String a, String b) {
        if (a == null || b == null) return false;
        String x = a.replaceAll("[^0-9]", ""), y = b.replaceAll("[^0-9]", "");
        if (x.isEmpty() || y.isEmpty()) return false;
        int n = Math.min(9, Math.min(x.length(), y.length()));
        return x.substring(x.length() - n).equals(y.substring(y.length() - n));
    }
}
