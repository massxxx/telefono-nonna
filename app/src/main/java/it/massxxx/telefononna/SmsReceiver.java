package it.massxxx.telefononna;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

/** Riceve gli SMS: sul telefono della nonna legge i comandi, sul telecomando le risposte. */
public class SmsReceiver extends BroadcastReceiver {
    public static final String AGGIORNA = "it.massxxx.telefononna.AGGIORNA";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) return;
        SmsMessage[] msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (msgs == null || msgs.length == 0) return;
        String mittente = msgs[0].getOriginatingAddress();
        StringBuilder sb = new StringBuilder();
        for (SmsMessage m : msgs) {
            if (m != null && m.getMessageBody() != null) sb.append(m.getMessageBody());
        }
        String testo = sb.toString();
        Prefs p = new Prefs(ctx);

        if (Prefs.MODO_NONNA.equals(p.modo())) {
            String comando = Comandi.leggi(testo, p.pin());
            if (comando == null) return;
            String solo = p.soloDa();
            if (!solo.isEmpty() && !Comandi.stessoNumero(solo, mittente)) {
                Log.i("TelefonoNonna", "Comando da numero non autorizzato");
                return;
            }
            NonnaService.comando(ctx, comando, mittente);
        } else if (Prefs.MODO_TELECOMANDO.equals(p.modo())) {
            if (!Comandi.eRisposta(testo)) return;
            String nonna = p.numeroNonna();
            if (!nonna.isEmpty() && !Comandi.stessoNumero(nonna, mittente)) return;
            p.salvaRisposta(testo, System.currentTimeMillis());
            ctx.sendBroadcast(new Intent(AGGIORNA).setPackage(ctx.getPackageName()));
            Canali.risposta(ctx, testo);
        }
    }
}
