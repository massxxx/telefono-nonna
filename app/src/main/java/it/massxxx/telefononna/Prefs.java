package it.massxxx.telefononna;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Random;

/** Impostazioni salvate sul telefono. */
public final class Prefs {
    public static final String MODO_NONNA = "nonna";
    public static final String MODO_TELECOMANDO = "telecomando";

    public static final String MESSAGGIO_PREDEFINITO =
            "Nonna, il telefono è qui!\nTi stiamo cercando: quando lo trovi chiamaci.";

    private final SharedPreferences sp;

    public Prefs(Context c) {
        sp = c.getApplicationContext().getSharedPreferences("nonna", Context.MODE_PRIVATE);
    }

    public String modo() { return sp.getString("modo", ""); }
    public void modo(String v) { sp.edit().putString("modo", v).apply(); }

    public String pin() {
        String p = sp.getString("pin", "");
        if (p.isEmpty()) {
            p = String.valueOf(1000 + new Random().nextInt(9000));
            sp.edit().putString("pin", p).apply();
        }
        return p;
    }
    public void pin(String v) { sp.edit().putString("pin", v.trim()).apply(); }

    public String messaggio() { return sp.getString("messaggio", MESSAGGIO_PREDEFINITO); }
    public void messaggio(String v) { sp.edit().putString("messaggio", v).apply(); }

    /** "sirena", "suoneria" o "sveglia". */
    public String suono() { return sp.getString("suono", "sirena"); }
    public void suono(String v) { sp.edit().putString("suono", v).apply(); }

    /** Minuti dopo cui lo squillo si ferma da solo; 0 = mai (solo da remoto). */
    public int autostop() { return sp.getInt("autostop", 0); }
    public void autostop(int v) { sp.edit().putInt("autostop", v).apply(); }

    /** Se non vuoto, accetta comandi solo da questo numero. */
    public String soloDa() { return sp.getString("soloDa", ""); }
    public void soloDa(String v) { sp.edit().putString("soloDa", v.trim()).apply(); }

    public String numeroNonna() { return sp.getString("numeroNonna", ""); }
    public void numeroNonna(String v) { sp.edit().putString("numeroNonna", v.trim()).apply(); }

    public String pinTelecomando() { return sp.getString("pinTelecomando", ""); }
    public void pinTelecomando(String v) { sp.edit().putString("pinTelecomando", v.trim()).apply(); }

    public String ultimaRisposta() { return sp.getString("ultimaRisposta", ""); }
    public long ultimaOra() { return sp.getLong("ultimaOra", 0); }
    public void salvaRisposta(String testo, long ora) {
        sp.edit().putString("ultimaRisposta", testo).putLong("ultimaOra", ora).apply();
    }

    public boolean squillando() { return sp.getBoolean("squillando", false); }
    public void squillando(boolean v) { sp.edit().putBoolean("squillando", v).commit(); }
}
