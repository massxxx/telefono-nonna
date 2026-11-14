import it.massxxx.telefononna.Comandi;

public class ProvaComandi {
    static int errori = 0;

    static void ok(boolean c, String nome) {
        System.out.println((c ? "OK   " : "ERR  ") + nome);
        if (!c) errori++;
    }

    public static void main(String[] a) {
        ok(Comandi.SQUILLA.equals(Comandi.leggi("NONNA 4821 SQUILLA", "4821")), "comando base");
        ok(Comandi.SQUILLA.equals(Comandi.leggi("  nonna 4821 squilla ", "4821")), "minuscole e spazi");
        ok(Comandi.STOP.equals(Comandi.leggi("Nonna stop 4821", "4821")), "ordine invertito");
        ok(Comandi.DOVE.equals(Comandi.leggi("NONNA 4821 POSIZIONE", "4821")), "sinonimo posizione");
        ok(Comandi.STATO.equals(Comandi.leggi("NONNA 4821 STATO", "4821")), "stato");
        ok(Comandi.leggi("NONNA 1111 SQUILLA", "4821") == null, "PIN sbagliato");
        ok(Comandi.leggi("NONNA 4821 BALLA", "4821") == null, "comando sconosciuto");
        ok(Comandi.leggi("Ciao nonna come stai", "4821") == null, "SMS normale");
        ok(Comandi.leggi("NONNA 4821 SQUILLA", "") == null, "PIN vuoto");
        ok(Comandi.crea(Comandi.DOVE, "4821").equals("NONNA 4821 DOVE"), "crea comando");
        ok(Comandi.eRisposta("NONNA> Sta squillando"), "riconosce risposta");
        ok(!Comandi.eRisposta("NONNA 4821 STOP"), "comando non è risposta");
        double[] xy = Comandi.coordinate("NONNA> Posizione: https://maps.google.com/?q=44.801234,10.327891 precisione 12 m");
        ok(xy != null && Math.abs(xy[0] - 44.801234) < 1e-9 && Math.abs(xy[1] - 10.327891) < 1e-9, "coordinate");
        ok(Comandi.coordinate("NONNA> Posizione non disponibile") == null, "niente coordinate");
        ok(Comandi.stessoNumero("+39 333 123 4567", "3331234567"), "numero con +39");
        ok(Comandi.stessoNumero("0039333-1234567", "+393331234567"), "numero con 0039");
        ok(!Comandi.stessoNumero("3331234567", "3337654321"), "numeri diversi");
        if (errori > 0) {
            System.out.println(errori + " errori");
            System.exit(1);
        }
        System.out.println("Tutto ok");
    }
}
