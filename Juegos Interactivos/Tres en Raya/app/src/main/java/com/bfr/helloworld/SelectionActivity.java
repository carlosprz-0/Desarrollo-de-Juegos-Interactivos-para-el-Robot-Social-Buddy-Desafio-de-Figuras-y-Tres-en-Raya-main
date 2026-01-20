package com.bfr.helloworld;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.bfr.buddy.usb.shared.IUsbCommadRsp;
import com.bfr.buddysdk.BuddyActivity;
import com.bfr.buddysdk.BuddySDK;
import com.bfr.buddy.speech.shared.ITTSCallback;
import android.media.MediaPlayer;

//C:\Users\Carlos\AppData\Local\Android\Sdk\platform-tools
//>adb install -r "C:/Users/Carlos/Desktop/JuegoImagenes/app/build/outputs/apk/debug/app-debug.apk"

public class SelectionActivity extends BuddyActivity {
    private Button buttonPvp;
    private Button buttonPve;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.selection_activity_main); // Carga el layout con los botones

        // Vinculación de los botones con los elementos del XML
        buttonPvp = findViewById(R.id.buttonPvp);
        buttonPve = findViewById(R.id.buttonPve);

        // Ocultarlos inicialmente, se mostrarán luego de que Buddy hable
        buttonPve.setVisibility(View.GONE);
        buttonPvp.setVisibility(View.GONE);

        // Botón para modo PvP (jugador contra jugador)
        buttonPvp.setOnClickListener(view -> {
            startGame(false);               // No se juega contra IA
            playSound(R.raw.game_start);   // Sonido de inicio
        });

        // Botón para modo PvE (jugador contra IA)
        buttonPve.setOnClickListener(view -> {
            playSound(R.raw.game_start);   // Sonido de inicio
            Intent intent = new Intent(SelectionActivity.this, DifficultySelectionActivity.class);
            startActivity(intent);         // Ir a la pantalla de selección de dificultad
            finish();                      // Cerrar esta actividad si no se quiere volver atrás
        });
    }

    // Se llama cuando el SDK de Buddy está listo
    @Override
    public void onSDKReady() {
        welcomeMessage();         // Buddy da la bienvenida con voz
        centerBuddyHeadFully();   // Centra la cabeza del robot
    }

    // Mensaje de bienvenida con síntesis de voz
    private void welcomeMessage() {
        BuddySDK.Speech.startSpeaking("Bienvenido al tres en raya. Por favor, elige un modo de juego.", new ITTSCallback.Stub() {
            @Override public void onSuccess(String s) {
                runOnUiThread(() -> {
                    // Muestra los botones con animación una vez que Buddy termina de hablar
                    animateShow(buttonPve);
                    animateShow(buttonPvp);
                });
            }

            @Override public void onError(String s) {}
            @Override public void onPause() {}
            @Override public void onResume() {}
            @Override public android.os.IBinder asBinder() { return null; }
        });
    }

    // Reproduce un sonido dado un recurso
    private void playSound(int resId) {
        MediaPlayer mediaPlayer = MediaPlayer.create(this, resId);
        if (mediaPlayer != null) {
            mediaPlayer.setOnCompletionListener(MediaPlayer::release); // Libera recursos al terminar
            mediaPlayer.start();
        }
    }

    // Inicia el juego con la opción seleccionada (PvP o PvE)
    private void startGame(boolean playAgainstAI) {
        getSharedPreferences("GamePrefs", MODE_PRIVATE)
                .edit()
                .putBoolean("playAgainstAI", playAgainstAI)  // Guarda si se juega contra IA
                .apply();

        startActivity(new Intent(this, MainActivity.class)); // Lanza la actividad principal del juego
        finish(); // Cierra esta pantalla
    }

    // Aplica animación para mostrar un botón con efecto de escala y opacidad
    public void animateShow(View view) {
        view.setVisibility(View.VISIBLE);
        view.setScaleX(0.8f);
        view.setScaleY(0.8f);
        view.setAlpha(0f);
        view.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .start();
    }

    // Centra la cabeza de Buddy en posición neutra (horizontal y vertical)
    private void centerBuddyHeadFully() {
        BuddySDK.USB.enableYesMove(1, new IUsbCommadRsp.Stub() {
            @Override
            public void onSuccess(String s1) {
                BuddySDK.USB.enableNoMove(1, new IUsbCommadRsp.Stub() {
                    @Override
                    public void onSuccess(String s2) {
                        Log.d("BUDDY_HEAD", "YES and NO motors enabled");

                        // Centrar eje vertical (asentir)
                        BuddySDK.USB.buddySayYes(20f, 0f, new IUsbCommadRsp.Stub() {
                            @Override
                            public void onSuccess(String s3) {
                                Log.d("BUDDY_HEAD", "Vertical head centered");

                                // Centrar eje horizontal (negar)
                                BuddySDK.USB.buddySayNo(20f, 0f, new IUsbCommadRsp.Stub() {
                                    @Override
                                    public void onSuccess(String s4) {
                                        Log.d("BUDDY_HEAD", "Horizontal head centered");
                                    }

                                    @Override
                                    public void onFailed(String e) {
                                        Log.e("BUDDY_HEAD", "Failed to center horizontal: " + e);
                                    }
                                });
                            }

                            @Override
                            public void onFailed(String e) {
                                Log.e("BUDDY_HEAD", "Failed to center vertical: " + e);
                            }
                        });
                    }

                    @Override
                    public void onFailed(String e) {
                        Log.e("BUDDY_HEAD", "Failed to enable NO motor: " + e);
                    }
                });
            }

            @Override
            public void onFailed(String e) {
                Log.e("BUDDY_HEAD", "Failed to enable YES motor: " + e);
            }
        });
    }
}


