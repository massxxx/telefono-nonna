package it.massxxx.telefononna;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.telephony.SmsManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Servizio del telefono della nonna: squillo al massimo volume e invio della posizione. */
public class NonnaService extends Service {
    public static final String CHIUDI = "it.massxxx.telefononna.CHIUDI";
    public static final String PROVA = "PROVA";
    private static final String TAG = "TelefonoNonna";
    private static final int ID_NOTIFICA = 7;

    private final Handler h = new Handler(Looper.getMainLooper());
    private Prefs prefs;
    private SuonoPlayer player;
    private PowerManager.WakeLock wl;
    private boolean squilla, cerca;
    private int volumePrima = -1, filtroPrima = NotificationManager.INTERRUPTION_FILTER_UNKNOWN;

    private LocationManager lm;
    private LocationListener ascolta;
    private Location migliore;
    private final List<String> chiVuolePosizione = new ArrayList<>();

    private final Runnable rinforza = new Runnable() {
        @Override public void run() {
            if (!squilla) return;
            alzaAlMassimo();
            h.postDelayed(this, 1000);
        }
    };
    private final Runnable autoStop = () -> ferma(null);
    private final Runnable fineRicerca = this::mandaPosizione;

    /** Avvia il servizio con un comando (SQUILLA, STOP, DOVE, STATO, PROVA). */
    public static void comando(Context c, String azione, String mittente) {
        Intent i = new Intent(c, NonnaService.class).putExtra("azione", azione).putExtra("mittente", mittente);
        try {
            c.startForegroundService(i);
        } catch (Exception e) {
            Log.w(TAG, "Servizio non avviabile dallo sfondo: " + e);
            new Prefs(c).annota("⚠️ Android ha bloccato l'avvio dallo sfondo: servono \"Batteria senza limitazioni\" e \"Mostra sopra le altre app\" (" + e.getClass().getSimpleName() + ")");
            // Ripiego: apre la schermata, che avvia il servizio quando è visibile
            if (Comandi.SQUILLA.equals(azione) || PROVA.equals(azione)) {
                try {
                    c.startActivity(new Intent(c, SquilloActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .putExtra("avvia", azione)
                            .putExtra("mittente", mittente));
                } catch (Exception e2) {
                    Log.w(TAG, "Neanche la schermata si apre: " + e2);
                }
            }
        }
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = new Prefs(this);
        player = new SuonoPlayer(this);
        Canali.crea(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        primoPiano(); // va chiamato subito, per ogni avvio
        String azione = intent == null ? null : intent.getStringExtra("azione");
        String mittente = intent == null ? null : intent.getStringExtra("mittente");
        if (azione == null) {
            fineSeLibero();
            return START_NOT_STICKY;
        }
        if (!PROVA.equals(azione)) prefs.annota("✅ Servizio avviato per " + azione);
        switch (azione) {
            case Comandi.SQUILLA: squilla(mittente, false); break;
            case PROVA: squilla(null, true); break;
            case Comandi.STOP: ferma(mittente); break;
            case Comandi.DOVE: dove(mittente); break;
            case Comandi.STATO:
                rispondi(mittente, "Telefono acceso e app attiva. "
                        + (squilla ? "Sta squillando. " : "Non sta squillando. ") + batteria());
                fineSeLibero();
                break;
            default: fineSeLibero();
        }
        return START_NOT_STICKY;
    }

    // ---------- Squillo ----------

    private void squilla(String mittente, boolean prova) {
        boolean gia = squilla;
        squilla = true;
        prefs.squillando(true);
        primoPiano();
        if (!gia) {
            salvaVolumi();
            alzaAlMassimo();
            PowerManager pm = getSystemService(PowerManager.class);
            if (pm != null) {
                wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "telefononna:squillo");
                wl.acquire(6 * 60 * 60 * 1000L);
            }
            player.avvia(prefs.suono());
            h.postDelayed(rinforza, 1000);
        }
        apriSchermata();
        h.removeCallbacks(autoStop);
        if (prova) {
            h.postDelayed(autoStop, 10_000);
        } else if (prefs.autostop() > 0) {
            h.postDelayed(autoStop, prefs.autostop() * 60_000L);
        }
        if (mittente != null) {
            rispondi(mittente, (gia ? "Stava già squillando. " : "Sta squillando al massimo volume. ") + batteria());
        }
    }

    private void apriSchermata() {
        try {
            startActivity(new Intent(this, SquilloActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            Log.w(TAG, "Schermata non aperta (resta la notifica a schermo intero): " + e);
        }
    }

    private void salvaVolumi() {
        AudioManager am = getSystemService(AudioManager.class);
        if (am != null && volumePrima < 0) volumePrima = am.getStreamVolume(AudioManager.STREAM_ALARM);
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null && nm.isNotificationPolicyAccessGranted()) {
            filtroPrima = nm.getCurrentInterruptionFilter();
        }
    }

    private void alzaAlMassimo() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null && nm.isNotificationPolicyAccessGranted()
                && nm.getCurrentInterruptionFilter() != NotificationManager.INTERRUPTION_FILTER_ALL) {
            try { nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL); } catch (Exception ignored) {}
        }
        AudioManager am = getSystemService(AudioManager.class);
        if (am == null) return;
        try {
            int max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM);
            if (am.getStreamVolume(AudioManager.STREAM_ALARM) != max) {
                am.setStreamVolume(AudioManager.STREAM_ALARM, max, 0);
            }
        } catch (Exception e) {
            Log.w(TAG, "Volume non modificabile: " + e);
        }
    }

    private void ripristinaVolumi() {
        AudioManager am = getSystemService(AudioManager.class);
        if (am != null && volumePrima >= 0) {
            try { am.setStreamVolume(AudioManager.STREAM_ALARM, volumePrima, 0); } catch (Exception ignored) {}
        }
        volumePrima = -1;
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null && nm.isNotificationPolicyAccessGranted()
                && filtroPrima != NotificationManager.INTERRUPTION_FILTER_UNKNOWN) {
            try { nm.setInterruptionFilter(filtroPrima); } catch (Exception ignored) {}
        }
        filtroPrima = NotificationManager.INTERRUPTION_FILTER_UNKNOWN;
    }

    private void ferma(String mittente) {
        boolean era = squilla;
        squilla = false;
        prefs.squillando(false);
        h.removeCallbacks(rinforza);
        h.removeCallbacks(autoStop);
        player.ferma();
        if (era) ripristinaVolumi();
        if (wl != null && wl.isHeld()) wl.release();
        wl = null;
        sendBroadcast(new Intent(CHIUDI).setPackage(getPackageName()));
        if (mittente != null) {
            rispondi(mittente, (era ? "Squillo fermato. " : "Non stava squillando. ") + batteria());
        }
        fineSeLibero();
    }

    // ---------- Posizione ----------

    private boolean haPosizione() {
        boolean fine = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean sempre = Build.VERSION.SDK_INT < 29
                || checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        return fine && sempre;
    }

    private void dove(String mittente) {
        if (mittente != null && !chiVuolePosizione.contains(mittente)) chiVuolePosizione.add(mittente);
        if (cerca) return;
        cerca = true;
        migliore = null;
        primoPiano();
        lm = getSystemService(LocationManager.class);
        if (lm == null || !haPosizione()) { mandaPosizione(); return; }

        ascolta = new LocationListener() {
            @Override public void onLocationChanged(Location l) {
                if (migliore == null || (l.hasAccuracy() && (!migliore.hasAccuracy() || l.getAccuracy() < migliore.getAccuracy()))) {
                    migliore = l;
                }
                if (l.hasAccuracy() && l.getAccuracy() <= 30) mandaPosizione();
            }
            @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
            @Override public void onProviderEnabled(String provider) {}
            @Override public void onProviderDisabled(String provider) {}
        };
        int attivi = 0;
        for (String p : new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER}) {
            try {
                if (lm.isProviderEnabled(p)) {
                    lm.requestLocationUpdates(p, 1000, 0, ascolta, Looper.getMainLooper());
                    attivi++;
                }
            } catch (SecurityException | IllegalArgumentException e) {
                Log.w(TAG, "Provider " + p + ": " + e);
            }
        }
        if (attivi == 0) { mandaPosizione(); return; }
        h.postDelayed(fineRicerca, 60_000);
    }

    private Location ultimaNota() {
        if (lm == null || !haPosizione()) return null;
        Location best = null;
        for (String p : new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER}) {
            try {
                Location l = lm.getLastKnownLocation(p);
                if (l != null && (best == null || l.getTime() > best.getTime())) best = l;
            } catch (Exception ignored) {}
        }
        return best;
    }

    private void mandaPosizione() {
        if (!cerca) return;
        h.removeCallbacks(fineRicerca);
        if (lm != null && ascolta != null) {
            try { lm.removeUpdates(ascolta); } catch (Exception ignored) {}
        }
        ascolta = null;
        Location l = migliore != null ? migliore : ultimaNota();
        String testo;
        if (l != null) {
            long minuti = (System.currentTimeMillis() - l.getTime()) / 60_000;
            testo = String.format(Locale.US, "Posizione: https://maps.google.com/?q=%.6f,%.6f", l.getLatitude(), l.getLongitude())
                    + (l.hasAccuracy() ? " precisione " + Math.round(l.getAccuracy()) + " m" : "")
                    + (minuti >= 3 ? " (di " + minuti + " min fa)" : "")
                    + ". " + batteria();
        } else if (!haPosizione()) {
            testo = "Posizione non disponibile: manca il permesso posizione \"Consenti sempre\". " + batteria();
        } else {
            testo = "Posizione non disponibile (posizione spenta o senza segnale). " + batteria();
        }
        for (String m : chiVuolePosizione) rispondi(m, testo);
        chiVuolePosizione.clear();
        cerca = false;
        fineSeLibero();
    }

    // ---------- Utilità ----------

    private String batteria() {
        BatteryManager bm = getSystemService(BatteryManager.class);
        int b = bm == null ? -1 : bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        return b >= 0 ? "Batteria " + b + "%." : "";
    }

    private void rispondi(String numero, String testo) {
        if (numero == null) return;
        if (checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            prefs.annota("❌ Risposta non inviata: manca il permesso SMS");
            return;
        }
        prefs.annota("📤 Risposta a " + numero);
        try {
            SmsManager sm = null;
            if (Build.VERSION.SDK_INT >= 31) sm = getSystemService(SmsManager.class);
            if (sm == null) sm = SmsManager.getDefault();
            String t = Comandi.RISPOSTA + " " + testo;
            ArrayList<String> parti = sm.divideMessage(t);
            if (parti.size() > 1) sm.sendMultipartTextMessage(numero, null, parti, null, null);
            else sm.sendTextMessage(numero, null, t, null, null);
        } catch (Exception e) {
            Log.w(TAG, "SMS di risposta non inviato: " + e);
        }
    }

    private Notification notifica() {
        Notification.Builder b = new Notification.Builder(this, squilla ? Canali.SQUILLO : Canali.SERVIZIO)
                .setSmallIcon(R.drawable.ic_notifica)
                .setOngoing(true)
                .setShowWhen(false);
        if (squilla) {
            PendingIntent schermata = PendingIntent.getActivity(this, 1,
                    new Intent(this, SquilloActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            String primaRiga = prefs.messaggio().split("\n")[0];
            b.setContentTitle("Il telefono sta squillando")
                    .setContentText(primaRiga)
                    .setCategory(Notification.CATEGORY_ALARM)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setContentIntent(schermata)
                    .setFullScreenIntent(schermata, true);
        } else {
            PendingIntent app = PendingIntent.getActivity(this, 4,
                    new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            b.setContentTitle("Telefono Nonna")
                    .setContentText(cerca ? "Cerco la posizione..." : "Attivo")
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .setContentIntent(app);
        }
        if (Build.VERSION.SDK_INT >= 31) b.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
        return b.build();
    }

    private void primoPiano() {
        Notification n = notifica();
        if (Build.VERSION.SDK_INT >= 29) {
            int tipo = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK;
            if (cerca && haPosizione()) tipo |= ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;
            try {
                startForeground(ID_NOTIFICA, n, tipo);
                return;
            } catch (Exception e) {
                Log.w(TAG, "startForeground con posizione: " + e);
            }
            try {
                startForeground(ID_NOTIFICA, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
                return;
            } catch (Exception e) {
                Log.w(TAG, "startForeground: " + e);
            }
        }
        try { startForeground(ID_NOTIFICA, n); } catch (Exception e) { Log.w(TAG, "startForeground: " + e); }
    }

    private void fineSeLibero() {
        if (squilla || cerca) {
            primoPiano();
            return;
        }
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        h.removeCallbacksAndMessages(null);
        player.ferma();
        if (squilla) {
            ripristinaVolumi();
            prefs.squillando(false);
            sendBroadcast(new Intent(CHIUDI).setPackage(getPackageName()));
        }
        if (lm != null && ascolta != null) {
            try { lm.removeUpdates(ascolta); } catch (Exception ignored) {}
        }
        if (wl != null && wl.isHeld()) wl.release();
        super.onDestroy();
    }
}
