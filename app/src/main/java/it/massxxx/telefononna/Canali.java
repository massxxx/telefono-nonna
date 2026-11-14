package it.massxxx.telefononna;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

/** Canali di notifica e notifica delle risposte per il telecomando. */
public final class Canali {
    public static final String SQUILLO = "squillo";
    public static final String SERVIZIO = "servizio";
    public static final String RISPOSTE = "risposte";

    private Canali() {}

    public static void crea(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null) return;

        NotificationChannel sq = new NotificationChannel(SQUILLO, "Squillo", NotificationManager.IMPORTANCE_HIGH);
        sq.setDescription("Mostrata mentre il telefono squilla");
        sq.setSound(null, null);
        sq.enableVibration(false);
        sq.setBypassDnd(true);
        sq.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(sq);

        NotificationChannel se = new NotificationChannel(SERVIZIO, "Ricerca posizione", NotificationManager.IMPORTANCE_LOW);
        se.setDescription("Mostrata mentre il telefono cerca la sua posizione");
        nm.createNotificationChannel(se);

        NotificationChannel ri = new NotificationChannel(RISPOSTE, "Risposte dal telefono della nonna", NotificationManager.IMPORTANCE_HIGH);
        ri.setDescription("Avvisi quando il telefono della nonna risponde");
        nm.createNotificationChannel(ri);
    }

    public static void risposta(Context c, String testo) {
        crea(c);
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null) return;
        PendingIntent pi = PendingIntent.getActivity(c, 2,
                new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        String corpo = testo.trim();
        if (corpo.toUpperCase().startsWith(Comandi.RISPOSTA)) corpo = corpo.substring(Comandi.RISPOSTA.length()).trim();
        Notification n = new Notification.Builder(c, RISPOSTE)
                .setSmallIcon(R.drawable.ic_notifica)
                .setContentTitle("Telefono della nonna")
                .setContentText(corpo)
                .setStyle(new Notification.BigTextStyle().bigText(corpo))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build();
        try {
            nm.notify(3, n);
        } catch (SecurityException ignored) {
        }
    }
}
