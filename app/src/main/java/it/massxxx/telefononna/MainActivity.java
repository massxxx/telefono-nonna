package it.massxxx.telefononna;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.text.InputType;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/** Schermata principale: scelta della modalità, impostazioni del telefono della nonna, telecomando. */
public class MainActivity extends Activity {
    private static final String INVIATO = "it.massxxx.telefononna.INVIATO";
    private static final int RICHIESTA_PERMESSI = 10;

    private Prefs prefs;
    private LinearLayout col;
    private TextView statoInvio;
    private final Handler h = new Handler(Looper.getMainLooper());
    private SuonoPlayer prova;
    private boolean registrato;

    private final BroadcastReceiver ricevi = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            if (INVIATO.equals(i.getAction())) {
                String cosa = i.getStringExtra("cosa");
                if (statoInvio != null) {
                    statoInvio.setText(getResultCode() == Activity.RESULT_OK
                            ? "✅ Inviato: " + cosa + ". Aspetto la risposta..."
                            : "❌ Invio non riuscito (" + cosa + "). Controlla credito e campo.");
                }
            } else {
                mostra();
            }
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = new Prefs(this);
        Canali.crea(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!registrato) {
            IntentFilter f = new IntentFilter();
            f.addAction(INVIATO);
            f.addAction(SmsReceiver.AGGIORNA);
            if (Build.VERSION.SDK_INT >= 33) registerReceiver(ricevi, f, Context.RECEIVER_NOT_EXPORTED);
            else registerReceiver(ricevi, f);
            registrato = true;
        }
        mostra();
    }

    @Override
    protected void onPause() {
        super.onPause();
        fermaProva();
    }

    @Override
    protected void onDestroy() {
        if (registrato) {
            try { unregisterReceiver(ricevi); } catch (Exception ignored) {}
        }
        super.onDestroy();
    }

    private void mostra() {
        ScrollView sv = new ScrollView(this);
        col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(this, 18);
        col.setPadding(p, p, p, Ui.dp(this, 40));
        sv.addView(col);
        statoInvio = null;
        switch (prefs.modo()) {
            case Prefs.MODO_NONNA: schermataNonna(); break;
            case Prefs.MODO_TELECOMANDO: schermataTelecomando(); break;
            default: schermataScelta();
        }
        setContentView(sv);
    }

    // ================= Scelta =================

    private void schermataScelta() {
        col.addView(Ui.titolo(this, "📞 Telefono Nonna"));
        col.addView(Ui.testo(this, "Questa app si installa su due telefoni Android:\n"
                + "• sul telefono della nonna, che squillerà al massimo volume;\n"
                + "• sul tuo telefono, che fa da telecomando via SMS.\n\nChe telefono è questo?"));
        col.addView(Ui.bottone(this, "👵 Questo è il telefono della NONNA\n(quello da far squillare)", Ui.ROSSO, v -> {
            prefs.modo(Prefs.MODO_NONNA);
            prefs.pin();
            mostra();
        }));
        col.addView(Ui.bottone(this, "📱 Questo è il TELECOMANDO\n(per far squillare la nonna)", Ui.BLU, v -> {
            prefs.modo(Prefs.MODO_TELECOMANDO);
            mostra();
        }));
    }

    private void cambiaModalita() {
        new AlertDialog.Builder(this)
                .setTitle("Cambiare modalità?")
                .setMessage("Torni alla scelta tra telefono della nonna e telecomando. Le impostazioni restano salvate.")
                .setPositiveButton("Cambia", (d, w) -> { prefs.modo(""); mostra(); })
                .setNegativeButton("Annulla", null)
                .show();
    }

    // ================= Telefono della nonna =================

    private void schermataNonna() {
        col.addView(Ui.titolo(this, "👵 Telefono della nonna"));

        List<String> mancano = permessiMancantiNonna();
        TextView stato = Ui.testo(this, mancano.isEmpty()
                ? "✅ Pronto! Può essere fatto squillare via SMS."
                : "⚠️ Mancano dei permessi (vedi sotto): senza, potrebbe non squillare.");
        stato.setTextSize(18);
        col.addView(stato);

        // --- Codice ---
        col.addView(Ui.sezione(this, "Codice segreto (PIN)"));
        col.addView(Ui.testo(this, "Scrivi lo stesso codice nel telecomando. Solo gli SMS col codice giusto fanno squillare."));
        EditText pin = Ui.campo(this, "Es. 4821", prefs.pin(), InputType.TYPE_CLASS_NUMBER);
        col.addView(pin);

        // --- Messaggio ---
        col.addView(Ui.sezione(this, "Messaggio a schermo"));
        EditText msg = Ui.campoTesto(this, "Messaggio da mostrare", prefs.messaggio());
        col.addView(msg);

        // --- Suono ---
        col.addView(Ui.sezione(this, "Suono"));
        RadioGroup suoni = new RadioGroup(this);
        String[][] opz = {{"sirena", "🚨 Sirena forte"}, {"suoneria", "🎵 Suoneria del telefono"}, {"sveglia", "⏰ Suono della sveglia"}};
        for (int i = 0; i < opz.length; i++) {
            RadioButton r = new RadioButton(this);
            r.setId(100 + i);
            r.setText(opz[i][1]);
            r.setTextSize(17);
            r.setTag(opz[i][0]);
            suoni.addView(r);
            if (opz[i][0].equals(prefs.suono())) r.setChecked(true);
        }
        col.addView(suoni);
        LinearLayout provaRiga = new LinearLayout(this);
        provaRiga.setOrientation(LinearLayout.HORIZONTAL);
        provaRiga.addView(Ui.piccolo(this, "▶ Ascolta 3 secondi (volume attuale)", v -> {
            RadioButton r = findViewById(suoni.getCheckedRadioButtonId());
            ascoltaProva(r == null ? "sirena" : (String) r.getTag());
        }));
        col.addView(provaRiga);

        // --- Sicurezza ---
        col.addView(Ui.sezione(this, "Chi può farlo squillare"));
        col.addView(Ui.testo(this, "Facoltativo: accetta comandi solo da questo numero (il tuo). Lascia vuoto per accettare chiunque conosca il codice."));
        EditText solo = Ui.campo(this, "Es. 333 1234567", prefs.soloDa(), InputType.TYPE_CLASS_PHONE);
        col.addView(solo);

        col.addView(Ui.sezione(this, "Stop automatico di sicurezza"));
        col.addView(Ui.testo(this, "Normalmente si ferma solo col comando STOP. Puoi aggiungere uno stop automatico nel caso l'SMS di stop non arrivi."));
        RadioGroup auto = new RadioGroup(this);
        int[] minuti = {0, 5, 15, 30};
        for (int i = 0; i < minuti.length; i++) {
            RadioButton r = new RadioButton(this);
            r.setId(200 + i);
            r.setText(minuti[i] == 0 ? "Mai (solo da remoto)" : "Dopo " + minuti[i] + " minuti");
            r.setTextSize(17);
            r.setTag(minuti[i]);
            auto.addView(r);
            if (minuti[i] == prefs.autostop()) r.setChecked(true);
        }
        col.addView(auto);

        col.addView(Ui.bottone(this, "💾 Salva", Ui.VERDE, v -> {
            String nuovoPin = pin.getText().toString().trim();
            if (nuovoPin.length() < 4 || nuovoPin.contains(" ")) {
                Toast.makeText(this, "Il codice deve avere almeno 4 cifre", Toast.LENGTH_LONG).show();
                return;
            }
            prefs.pin(nuovoPin);
            String m = msg.getText().toString().trim();
            prefs.messaggio(m.isEmpty() ? Prefs.MESSAGGIO_PREDEFINITO : m);
            RadioButton rs = findViewById(suoni.getCheckedRadioButtonId());
            if (rs != null) prefs.suono((String) rs.getTag());
            RadioButton ra = findViewById(auto.getCheckedRadioButtonId());
            if (ra != null) prefs.autostop((Integer) ra.getTag());
            prefs.soloDa(solo.getText().toString());
            Toast.makeText(this, "Salvato ✅", Toast.LENGTH_SHORT).show();
            mostra();
        }));

        // --- Permessi ---
        col.addView(Ui.sezione(this, "Permessi (tutti ✅ per funzionare bene)"));
        rigaPermesso("SMS (ricevere comandi e rispondere)", haSms(), this::chiediSms);
        rigaPermesso("Posizione", haPermesso(Manifest.permission.ACCESS_FINE_LOCATION),
                () -> requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, RICHIESTA_PERMESSI));
        if (Build.VERSION.SDK_INT >= 29) {
            rigaPermesso("Posizione \"Consenti sempre\"", haPermesso(Manifest.permission.ACCESS_BACKGROUND_LOCATION), () -> {
                if (!haPermesso(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    Toast.makeText(this, "Prima concedi \"Posizione\"", Toast.LENGTH_LONG).show();
                    return;
                }
                Toast.makeText(this, "Scegli \"Consenti sempre\"", Toast.LENGTH_LONG).show();
                requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, RICHIESTA_PERMESSI);
            });
        }
        if (Build.VERSION.SDK_INT >= 33) {
            rigaPermesso("Notifiche", haPermesso(Manifest.permission.POST_NOTIFICATIONS),
                    () -> requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, RICHIESTA_PERMESSI));
        }
        NotificationManager nm = getSystemService(NotificationManager.class);
        rigaPermesso("Superare \"Non disturbare\"", nm != null && nm.isNotificationPolicyAccessGranted(),
                () -> apri(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)));
        rigaPermesso("Mostra sopra le altre app", Settings.canDrawOverlays(this),
                () -> apri(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))));
        PowerManager pm = getSystemService(PowerManager.class);
        rigaPermesso("Batteria senza limitazioni", pm != null && pm.isIgnoringBatteryOptimizations(getPackageName()),
                () -> apri(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + getPackageName()))));
        if (Build.VERSION.SDK_INT >= 34) {
            rigaPermesso("Schermata a tutto schermo", nm != null && nm.canUseFullScreenIntent(),
                    () -> apri(new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:" + getPackageName()))));
        }
        col.addView(Ui.testo(this, "Se Android dice \"Impostazione con restrizioni\" per gli SMS: apri Info app → menu ⋮ in alto → \"Consenti impostazioni con restrizioni\", poi riprova."));
        col.addView(Ui.piccolo(this, "Apri Info app", v -> apri(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())))));

        // --- Prova ---
        col.addView(Ui.sezione(this, "Prova"));
        col.addView(Ui.testo(this, "Fa squillare questo telefono al massimo volume per 10 secondi, con la schermata del messaggio."));
        col.addView(Ui.bottone(this, "🔔 Prova completa (10 secondi)", Ui.ARANCIO, v -> NonnaService.comando(this, NonnaService.PROVA, null)));

        // --- Comandi ---
        col.addView(Ui.sezione(this, "Comandi SMS"));
        String c = prefs.pin();
        col.addView(Ui.riquadro(this, Ui.testo(this,
                "Dal telecomando basta premere i pulsanti. Da qualsiasi altro telefono (anche iPhone) puoi scrivere un SMS a questo numero:\n\n"
                        + "NONNA " + c + " SQUILLA  → squilla al massimo\n"
                        + "NONNA " + c + " STOP  → smette di squillare\n"
                        + "NONNA " + c + " DOVE  → risponde con la posizione\n"
                        + "NONNA " + c + " STATO  → risponde se è acceso e la batteria")));

        col.addView(Ui.bottone(this, "Cambia modalità", Ui.GRIGIO, v -> cambiaModalita()));
    }

    private List<String> permessiMancantiNonna() {
        List<String> m = new ArrayList<>();
        if (!haSms()) m.add("sms");
        if (!haPermesso(Manifest.permission.ACCESS_FINE_LOCATION)) m.add("posizione");
        if (Build.VERSION.SDK_INT >= 29 && !haPermesso(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) m.add("sempre");
        if (Build.VERSION.SDK_INT >= 33 && !haPermesso(Manifest.permission.POST_NOTIFICATIONS)) m.add("notifiche");
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm == null || !nm.isNotificationPolicyAccessGranted()) m.add("dnd");
        if (!Settings.canDrawOverlays(this)) m.add("sopra");
        PowerManager pm = getSystemService(PowerManager.class);
        if (pm == null || !pm.isIgnoringBatteryOptimizations(getPackageName())) m.add("batteria");
        if (Build.VERSION.SDK_INT >= 34 && (nm == null || !nm.canUseFullScreenIntent())) m.add("schermo");
        return m;
    }

    private void ascoltaProva(String tipo) {
        fermaProva();
        prova = new SuonoPlayer(this);
        prova.avvia(tipo);
        h.postDelayed(this::fermaProva, 3000);
    }

    private void fermaProva() {
        h.removeCallbacksAndMessages(null);
        if (prova != null) { prova.ferma(); prova = null; }
    }

    // ================= Telecomando =================

    private void schermataTelecomando() {
        col.addView(Ui.titolo(this, "📱 Telecomando"));

        boolean pronto = !prefs.numeroNonna().isEmpty() && prefs.pinTelecomando().length() >= 4;

        col.addView(Ui.sezione(this, "Telefono della nonna"));
        EditText numero = Ui.campo(this, "Numero della nonna (es. 333 1234567)", prefs.numeroNonna(), InputType.TYPE_CLASS_PHONE);
        col.addView(numero);
        EditText pin = Ui.campo(this, "Codice segreto (PIN) scritto sul telefono della nonna", prefs.pinTelecomando(), InputType.TYPE_CLASS_NUMBER);
        col.addView(pin);
        col.addView(Ui.piccolo(this, "💾 Salva numero e codice", v -> {
            String n = numero.getText().toString().trim();
            String c = pin.getText().toString().trim();
            if (n.replaceAll("[^0-9]", "").length() < 6) { Toast.makeText(this, "Numero non valido", Toast.LENGTH_LONG).show(); return; }
            if (c.length() < 4) { Toast.makeText(this, "Il codice ha almeno 4 cifre", Toast.LENGTH_LONG).show(); return; }
            prefs.numeroNonna(n);
            prefs.pinTelecomando(c);
            Toast.makeText(this, "Salvato ✅", Toast.LENGTH_SHORT).show();
            mostra();
        }));

        col.addView(Ui.sezione(this, "Permessi"));
        rigaPermesso("SMS (mandare comandi e ricevere risposte)", haSms(), this::chiediSms);
        if (Build.VERSION.SDK_INT >= 33) {
            rigaPermesso("Notifiche", haPermesso(Manifest.permission.POST_NOTIFICATIONS),
                    () -> requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, RICHIESTA_PERMESSI));
        }
        if (!haSms()) {
            col.addView(Ui.testo(this, "Se premendo \"Concedi\" non compare niente: Info app → Autorizzazioni → SMS → Consenti. "
                    + "Se è grigio: Info app → menu ⋮ in alto → \"Consenti impostazioni con restrizioni\"."));
            col.addView(Ui.piccolo(this, "Apri Info app", v -> apri(new Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())))));
        }

        col.addView(Ui.sezione(this, "Comandi"));
        if (!pronto) col.addView(Ui.testo(this, "Prima salva il numero e il codice della nonna."));
        Button sq = Ui.bottone(this, "🔔  FAI SQUILLARE", Ui.ROSSO, v -> invia(Comandi.SQUILLA, "fai squillare"));
        sq.setTextSize(24);
        sq.setMinHeight(Ui.dp(this, 90));
        col.addView(sq);
        col.addView(Ui.bottone(this, "⏹  FERMA LO SQUILLO", Ui.VERDE, v -> invia(Comandi.STOP, "ferma")));
        col.addView(Ui.bottone(this, "📍  DOVE SI TROVA?", Ui.BLU, v -> invia(Comandi.DOVE, "posizione")));
        col.addView(Ui.bottone(this, "ℹ️  È ACCESO? (batteria)", Ui.GRIGIO, v -> invia(Comandi.STATO, "stato")));

        statoInvio = Ui.testo(this, "");
        statoInvio.setTextSize(17);
        col.addView(statoInvio);

        // --- Ultima risposta ---
        col.addView(Ui.sezione(this, "Ultima risposta dalla nonna"));
        String r = prefs.ultimaRisposta();
        if (r.isEmpty()) {
            col.addView(Ui.testo(this, "Ancora nessuna risposta."));
        } else {
            String corpo = r.trim();
            if (corpo.toUpperCase().startsWith(Comandi.RISPOSTA)) corpo = corpo.substring(Comandi.RISPOSTA.length()).trim();
            String ora = DateFormat.format("dd/MM HH:mm", prefs.ultimaOra()).toString();
            TextView t = Ui.testo(this, ora + "\n" + corpo);
            t.setTextSize(18);
            double[] xy = Comandi.coordinate(r);
            if (xy != null) {
                Button mappa = Ui.bottone(this, "🗺️ Apri sulla mappa", Ui.BLU, v -> apriMappa(xy[0], xy[1]));
                col.addView(Ui.riquadro(this, t, mappa));
            } else {
                col.addView(Ui.riquadro(this, t));
            }
        }

        col.addView(Ui.sezione(this, "Come funziona"));
        col.addView(Ui.testo(this, "Ogni pulsante manda un SMS al telefono della nonna, che risponde con un SMS. "
                + "Il telefono della nonna deve essere acceso e avere campo; non serve internet. "
                + "Lo squillo continua finché non premi \"Ferma lo squillo\"."));

        col.addView(Ui.bottone(this, "Cambia modalità", Ui.GRIGIO, v -> cambiaModalita()));
    }

    private void invia(String comando, String etichetta) {
        String numero = prefs.numeroNonna();
        String pin = prefs.pinTelecomando();
        if (numero.isEmpty() || pin.length() < 4) {
            if (statoInvio != null) statoInvio.setText("❌ Non inviato: prima scrivi numero e codice della nonna e premi Salva.");
            Toast.makeText(this, "Prima salva numero e codice della nonna", Toast.LENGTH_LONG).show();
            return;
        }
        if (!haSms()) {
            if (statoInvio != null) statoInvio.setText("❌ Non inviato: manca il permesso SMS.");
            new AlertDialog.Builder(this)
                    .setTitle("Manca il permesso SMS")
                    .setMessage("Senza questo permesso il telecomando non può mandare i comandi.\n\n"
                            + "Prova \"Concedi\". Se non compare nessuna richiesta, premi \"Apri impostazioni\" e vai su "
                            + "Autorizzazioni → SMS → Consenti.\n\n"
                            + "Se SMS è grigio o dice \"Impostazione con restrizioni\": nella stessa pagina tocca il menu ⋮ "
                            + "in alto a destra → \"Consenti impostazioni con restrizioni\", poi riprova.")
                    .setPositiveButton("Concedi", (d, w) -> chiediSms())
                    .setNeutralButton("Apri impostazioni", (d, w) -> apri(new Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()))))
                    .setNegativeButton("Annulla", null)
                    .show();
            return;
        }
        try {
            SmsManager sm = null;
            if (Build.VERSION.SDK_INT >= 31) sm = getSystemService(SmsManager.class);
            if (sm == null) sm = SmsManager.getDefault();
            PendingIntent inviato = PendingIntent.getBroadcast(this, comando.hashCode(),
                    new Intent(INVIATO).setPackage(getPackageName()).putExtra("cosa", etichetta),
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            sm.sendTextMessage(numero, null, Comandi.crea(comando, pin), inviato, null);
            if (statoInvio != null) statoInvio.setText("⏳ Invio in corso: " + etichetta + "...");
        } catch (Exception e) {
            Toast.makeText(this, "Errore invio SMS: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void apriMappa(double lat, double lon) {
        Uri geo = Uri.parse("geo:" + lat + "," + lon + "?q=" + lat + "," + lon + "(Telefono della nonna)");
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, geo));
        } catch (Exception e) {
            apri(new Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=" + lat + "," + lon)));
        }
    }

    // ================= Permessi =================

    private boolean haPermesso(String p) {
        return checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean haSms() {
        return haPermesso(Manifest.permission.RECEIVE_SMS) && haPermesso(Manifest.permission.SEND_SMS);
    }

    private void chiediSms() {
        List<String> l = new ArrayList<>();
        l.add(Manifest.permission.RECEIVE_SMS);
        l.add(Manifest.permission.SEND_SMS);
        if (Build.VERSION.SDK_INT >= 33) l.add(Manifest.permission.POST_NOTIFICATIONS);
        requestPermissions(l.toArray(new String[0]), RICHIESTA_PERMESSI);
    }

    private void rigaPermesso(String nome, boolean ok, Runnable concedi) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setLayoutParams(Ui.riga(this, 4));
        TextView t = Ui.testo(this, (ok ? "✅ " : "❌ ") + nome);
        t.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        r.addView(t);
        if (!ok) r.addView(Ui.piccolo(this, "Concedi", v -> concedi.run()));
        col.addView(r);
    }

    private void apri(Intent i) {
        try {
            startActivity(i);
        } catch (Exception e) {
            try {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())));
            } catch (Exception ignored) {
                Toast.makeText(this, "Apri le impostazioni dell'app a mano", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED
                    && !Manifest.permission.POST_NOTIFICATIONS.equals(permissions[i])
                    && !shouldShowRequestPermissionRationale(permissions[i])) {
                // Android non mostra più la richiesta: si va nelle impostazioni dell'app
                new AlertDialog.Builder(this)
                        .setTitle("Permesso da dare a mano")
                        .setMessage("Android non mostra più la richiesta. Apri Info app → Autorizzazioni e scegli \"Consenti\".\n\n"
                                + "Se il permesso è grigio: menu ⋮ in alto a destra → \"Consenti impostazioni con restrizioni\".")
                        .setPositiveButton("Apri impostazioni", (d, w) -> apri(new Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()))))
                        .setNegativeButton("Annulla", null)
                        .show();
                break;
            }
        }
        mostra();
    }
}
