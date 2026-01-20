// MainActivity.java con puzzles aleatorios y soporte para temporizador (sin modo táctil)

package com.bfr.helloworld;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.bfr.buddy.usb.shared.IUsbCommadRsp;
import com.bfr.buddysdk.BuddyActivity;
import com.bfr.buddysdk.BuddySDK;
import com.bfr.buddysdk.services.speech.STTTask;
import com.bfr.buddy.speech.shared.ISTTCallback;
import com.bfr.buddy.speech.shared.STTResultsData;
import com.bfr.buddy.speech.shared.ITTSCallback;
import com.bfr.buddy.ui.shared.FacialExpression;
import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

import java.net.NetworkInterface;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.util.Enumeration;
import java.io.IOException;

import java.io.File;
import java.io.FileWriter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import com.bumptech.glide.load.engine.DiskCacheStrategy;



import org.json.JSONArray;
import org.json.JSONObject;

import com.bfr.helloworld.Puzzle;
import com.bfr.helloworld.PuzzleGenerator;




public class MainActivity extends BuddyActivity {


    // Elementos de la interfaz relacionados con el puzzle
    private RelativeLayout puzzleContainer;         // Contenedor principal que envuelve los elementos visuales del puzzle
    private ImageView mainImage;                    // Imagen principal del enigma a resolver
    private ImageView[] optionImages = new ImageView[3]; // Arreglo de imágenes que representan las opciones de respuesta

    // Información del jugador
    private TextView scoreText, livesText;          // Textos para mostrar la puntuación actual y las vidas restantes
    private EditText nameInput;                     // Campo de entrada para que el usuario escriba su nombre
    private Button nameConfirmButton;               // Botón para confirmar el nombre introducido
    private View nameLayout;                        // Layout que contiene la UI del nombre (entrada + botón)

    // Temporizador visual
    private ProgressBar timerProgress;              // Barra de progreso que actúa como temporizador visual

    // Lógica del juego
    private List<Puzzle> puzzles;                   // Lista de puzzles cargados que se presentan al usuario
    private int currentPuzzleIndex = 0;             // Índice del puzzle actual en la lista
    private int score = 0;                          // Puntuación acumulada del jugador
    private int lives = 5;                          // Número de vidas iniciales del jugador

    // Reconocimiento de voz
    private STTTask sttTask;                        // Tarea de reconocimiento de voz (Speech-To-Text)
    private String playerName = "";                 // Nombre del jugador

    // Temporizador y estadísticas
    private CountDownTimer timer;                   // Temporizador que limita el tiempo para responder
    private long preguntaStartTime;                 // Tiempo en milisegundos cuando comenzó la pregunta actual
    private List<RespuestaStats> respuestas = new ArrayList<>(); // Lista con estadísticas de respuestas dadas

    // Interfaz adicional
    private LinearLayout optionsContainer;          // Contenedor visual de las opciones de respuesta
    private Button pauseButton;                     // Botón para pausar el juego
    private WebPuzzleUploader server;               // Referencia a un servidor web si se está ejecutando en modo edición
    private boolean isPaused = false;               // Bandera para indicar si el juego está pausado
    private CardView infoBar;                       // Tarjeta visual que muestra información adicional como vidas/puntaje
    private String currentDifficulty = "";          // Dificultad de la pregunta actual
    private String selectedGame = "Juego de Figuras 1"; // Nombre del juego actualmente seleccionado

    // Manejo de animaciones
    private final Queue<Runnable> animationQueue = new LinkedList<>(); // Cola de animaciones pendientes por ejecutar
    private boolean isPlayingAnimation = false;     // Indica si actualmente se está reproduciendo una animación

    // Resultado final
    private int porcentaje_final = 0;               // Porcentaje de aciertos al final del juego

    // Frases para pedir el nombre del jugador, se elige una aleatoriamente
    String[] namePrompts = {
            "¿Cómo te llamas, jugador?",
            "Dime tu nombre para empezar.",
            "¿Quién jugará conmigo hoy?",
            "Necesito saber tu nombre primero.",
            "¿Puedes decirme tu nombre jugador?",
            "¿Cómo quieres que te llame?"
    };

    // Frases de saludo personalizadas que se muestran tras ingresar el nombre
    String[] greetings = {
            "¡Qué nombre tan genial, %s!",
            "Encantado de conocerte, %s.",
            "¡Perfecto, %s! Vamos a jugar.",
            "Un placer, %s. ¡Estoy listo!",
            "Genial, %s. Un placer conocerte"
    };

    // Frases que se muestran cuando el jugador no acierta una pregunta a tiempo o se equivoca
    String[] timesOver = {
            "Vaya, no lo has logrado... ¡inténtalo en la siguiente!",
            "Que pena... Vamos con la próxima.",
            "Lástima... probemos con otra.",
            "No te preocupes... probemos a la siguiente",
            "No pasa nada, vamos con otra y verás que lo haces mejor.",
            "Esta vez no fue, pero vamos a por la siguiente."
    };

    // Frases de elogio cuando el jugador responde correctamente, personalizadas con su nombre
    String[] praisePhrases = {
            "¡Excelente trabajo, %s!",
            "¡Muy bien hecho, %s!",
            "¡Eres genial, %s!",
            "¡Impresionante respuesta, %s!",
            "¡Correcto, %s! Sigue así.",
            "¡Eso estuvo perfecto, %s!",
            "¡Lo lograste, %s!",
            "¡Buen trabajo, %s!"
    };

    // Frases motivadoras para pasar a la siguiente pregunta
    String[] continuePhrases = {
            "Vamos con la siguiente.",
            "Sigamos jugando.",
            "¡A por la próxima!",
            "Vamos por más.",
            "Veamos si aciertas otra.",
            "¿Listo para otra?",
            "¡Seguimos con otra!",
            "A ver qué viene ahora."
    };




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Establece el diseño de la interfaz desde el archivo XML correspondiente
        setContentView(R.layout.activity_main);

        try {
            // Crea una instancia del servidor web que permite la edición de puzzles desde el navegador.
            // Se asocia al juego actualmente seleccionado (selectedGame).
            server = new WebPuzzleUploader(getApplicationContext(), selectedGame) {
                @Override
                public void onServerClosed() {
                    // Lógica personalizada que se ejecuta cuando el servidor se cierra correctamente
                    Log.i("ADMIN_SERVER", "Servidor detenido correctamente.");
                }
            };
        } catch (IOException e) {
            // Maneja errores que puedan ocurrir al iniciar el servidor, como problemas de red o permisos
            Log.e("ADMIN_SERVER", "Error iniciando el servidor web", e);
        }
    }


    @Override
    public void onSDKReady() {
        // Posiciona la cabeza del robot Buddy en el centro de la pantalla
        centerBuddyHeadFully2();

        // Establece la pantalla de bienvenida como layout activo
        setContentView(R.layout.welcome_screen);

        // Obtiene las referencias a los elementos de la UI
        View welcomeContainer = findViewById(R.id.welcomeLayout);
        TextView loadingText = findViewById(R.id.loadingText);
        ProgressBar loadingCircle = findViewById(R.id.loadingCircle);

        // Inicialmente oculta el texto y el círculo de carga
        loadingText.setAlpha(0f);
        loadingCircle.setAlpha(0f);

        // Aplica animaciones de aparición para ambos elementos (1 segundo)
        loadingText.animate().alpha(1f).setDuration(1000).start();
        loadingCircle.animate().alpha(1f).setDuration(1000).start();

        // Cambia la expresión facial del robot a neutral
        BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL);

        // Inicia una locución usando la síntesis de voz de Buddy
        BuddySDK.Speech.startSpeaking(
                "Bienvenido al juego de desafio de figuras. Cargando los datos del juego...",
                new ITTSCallback.Stub() {
                    @Override public IBinder asBinder() { return null; }
                    @Override public void onError(String s) {}    // Manejo de error si falla el TTS
                    @Override public void onPause() {}            // Callback si se pausa
                    @Override public void onResume() {}           // Callback si se reanuda

                    @Override
                    public void onSuccess(String s) {
                        // Una vez completado el mensaje, transiciona a la siguiente pantalla
                        runOnUiThread(() -> {
                            new Handler().postDelayed(() -> {
                                welcomeContainer.animate()
                                        .alpha(0f)
                                        .setDuration(700)
                                        .withEndAction(() -> {
                                            // Lanza el selector de partidas tras desvanecer la bienvenida
                                            returnMatchSelector();
                                        }).start();
                            }, 800); // Espera 800 ms tras la locución antes de transicionar
                        });
                    }
                }
        );
    }

    /**
     * Inicializa la entrada de nombre del jugador, incluyendo botón y comportamiento.
     */
    private void initializeNameInput() {
        nameInput = findViewById(R.id.nameInput); // Campo de texto para el nombre
        nameConfirmButton = findViewById(R.id.nameConfirmButton); // Botón de confirmación

        // Al pulsar el botón, se reproduce un sonido y se solicita el nombre
        nameConfirmButton.setOnClickListener(v -> {
            playSound(R.raw.game_start); // Efecto sonoro
            askForName(true);            // Llama al método que procesa el nombre
        });
    }

    /**
     * Solicita al jugador que diga su nombre usando una frase aleatoria.
     * Luego de una breve pausa, llama a askForName(false).
     */
    private void promptForName() {
        // Selecciona una frase aleatoria del array de preguntas
        String prompt = namePrompts[new Random().nextInt(namePrompts.length)];

        // La pronuncia usando el sistema de voz de Buddy
        BuddySDK.Speech.startSpeaking(prompt);

        // Espera 1 segundo antes de invocar el reconocimiento de voz
        new Handler().postDelayed(() -> askForName(false), 1000);
    }

    /**
     * Solicita y procesa el nombre del jugador, ya sea mediante entrada escrita (si fromButton = true)
     * o por reconocimiento de voz (fromButton = false).
     */
    private void askForName(boolean fromButton) {
        // Elige un saludo aleatorio para usar más adelante
        String greet = greetings[new Random().nextInt(greetings.length)];

        if (fromButton) {
            // Si el usuario hizo clic en el botón para confirmar el nombre escrito
            String typedName = nameInput.getText().toString().trim();

            // Detiene cualquier tarea STT activa
            if (sttTask != null) sttTask.stop();

            if (!typedName.isEmpty()) {
                // Modo administrador: si se introduce "administrador", lanza el servidor y el modo admin
                if (typedName.equalsIgnoreCase("administrador")) {
                    try {
                        server = new WebPuzzleUploader(getApplicationContext(), selectedGame) {
                            @Override
                            public void onServerClosed() {
                                server.stop(); // Detiene el servidor cuando se cierra
                            }
                        };
                        server.startServer();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    runAdminMode(); // Cambia a la vista del modo administrador
                } else {
                    // Guarda el nombre del jugador
                    playerName = typedName;

                    // Oculta el layout de entrada del nombre con una animación
                    runOnUiThread(() -> {
                        View nameLayout = findViewById(R.id.nameInputLayout);
                        if (nameLayout != null) {
                            nameLayout.animate()
                                    .alpha(0f)
                                    .scaleX(0.8f)
                                    .scaleY(0.8f)
                                    .setDuration(500)
                                    .withEndAction(() -> nameLayout.setVisibility(View.GONE))
                                    .start();
                        }
                    });

                    // Saluda al jugador por su nombre y pregunta si quiere comenzar
                    BuddySDK.Speech.startSpeaking(String.format(greet, playerName), new ITTSCallback.Stub() {
                        @Override
                        public void onSuccess(String s) {
                            runOnUiThread(() -> {
                                BuddySDK.Speech.startSpeaking("¿Comenzamos la partida?", new ITTSCallback.Stub() {
                                    @Override
                                    public void onSuccess(String s) {
                                        runOnUiThread(() ->
                                                new Handler().postDelayed(MainActivity.this::waitForStartConfirmation, 100)
                                        );
                                    }
                                    @Override public IBinder asBinder() { return null; }
                                    @Override public void onError(String s) {}
                                    @Override public void onPause() {}
                                    @Override public void onResume() {}
                                });
                            });
                        }

                        @Override public IBinder asBinder() { return null; }
                        @Override public void onError(String s) {}
                        @Override public void onPause() {}
                        @Override public void onResume() {}
                    });
                }
                return; // Termina aquí si se usó entrada escrita
            }
        }

        // Si no se usó entrada escrita, activar reconocimiento de voz
        if (sttTask != null) sttTask.stop();

        sttTask = BuddySDK.Speech.createGoogleSTTTask(Locale.forLanguageTag("es-ES"));
        sttTask.start(true, new ISTTCallback.Stub() {

            @Override
            public void onSuccess(STTResultsData result) {
                // Si se detectó alguna entrada hablada
                if (!result.getResults().isEmpty()) {
                    String spoken = result.getResults().get(0).getUtterance().trim();

                    if (sttTask != null) {
                        sttTask.stop();
                        sttTask = null;
                    }

                    if (spoken.equalsIgnoreCase("administrador")) {
                        // Si el jugador dijo "administrador", activa modo administrador
                        try {
                            server = new WebPuzzleUploader(getApplicationContext(), selectedGame) {
                                @Override
                                public void onServerClosed() {
                                    server.stop();
                                }
                            };
                            server.startServer();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        runOnUiThread(() -> runAdminMode());
                    } else {
                        // Nombre del jugador reconocido por voz
                        playerName = spoken;

                        // Oculta el layout de entrada con animación
                        runOnUiThread(() -> {
                            View nameLayout = findViewById(R.id.nameInputLayout);
                            if (nameLayout != null) {
                                nameLayout.animate()
                                        .alpha(0f)
                                        .scaleX(0.8f)
                                        .scaleY(0.8f)
                                        .setDuration(500)
                                        .withEndAction(() -> nameLayout.setVisibility(View.GONE))
                                        .start();
                            }
                        });

                        // Saluda al jugador y pregunta si quiere comenzar la partida
                        BuddySDK.Speech.startSpeaking(String.format(greet, playerName), new ITTSCallback.Stub() {
                            @Override
                            public void onSuccess(String s) {
                                runOnUiThread(() -> {
                                    BuddySDK.Speech.startSpeaking("¿Comenzamos la partida?", new ITTSCallback.Stub() {
                                        @Override
                                        public void onSuccess(String s) {
                                            runOnUiThread(() ->
                                                    new Handler().postDelayed(MainActivity.this::waitForStartConfirmation, 100)
                                            );
                                        }

                                        @Override public IBinder asBinder() { return null; }
                                        @Override public void onError(String s) {}
                                        @Override public void onPause() {}
                                        @Override public void onResume() {}
                                    });
                                });
                            }

                            @Override public IBinder asBinder() { return null; }
                            @Override public void onError(String s) {}
                            @Override public void onPause() {}
                            @Override public void onResume() {}
                        });
                    }
                }
            }

            @Override
            public void onError(String s) {
                // Si hay un error con el reconocimiento de voz, se vuelve a solicitar el nombre
                Log.e("BUDDY_VOICE", "Error en reconocimiento del nombre: " + s);
                runOnUiThread(() -> promptForName());
            }
        });
    }

    /**
     * Espera la confirmación del jugador para comenzar el juego mediante reconocimiento de voz.
     */
    private void waitForStartConfirmation() {
        if (sttTask != null) {
            sttTask.stop(); // Asegura que no haya un reconocimiento previo activo
        }

        // Inicia nueva tarea de reconocimiento de voz en español
        sttTask = BuddySDK.Speech.createGoogleSTTTask(Locale.forLanguageTag("es-ES"));
        sttTask.start(true, new ISTTCallback.Stub() {
            @Override
            public void onSuccess(STTResultsData result) {
                if (!result.getResults().isEmpty()) {
                    String answer = result.getResults().get(0).getUtterance().toLowerCase();

                    if (sttTask != null) {
                        sttTask.stop();
                        sttTask = null;
                    }

                    // Si el usuario responde afirmativamente, inicia el juego
                    if (answer.contains("sí") || answer.contains("si") || answer.contains("vale") ||
                            answer.contains("empecemos") || answer.contains("vamos")) {

                        BuddySDK.Speech.startSpeaking("Muy bien, empecemos.", new ITTSCallback.Stub() {
                            @Override
                            public void onSuccess(String s) {
                                runOnUiThread(() -> {
                                    // Cambia la expresión del robot a feliz y reproduce sonido de inicio
                                    BuddySDK.UI.setFacialExpression(FacialExpression.HAPPY);
                                    BuddySDK.UI.setMood(FacialExpression.HAPPY, 1.0, null);
                                    playSound(R.raw.gamestart);

                                    // Anima la cabeza de Buddy antes de comenzar
                                    rotate360(() -> {
                                        BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL);
                                        startGame(); // Lanza el juego
                                    });
                                });
                            }

                            @Override public void onError(String s) {}
                            @Override public void onPause() {}
                            @Override public void onResume() {}
                            @Override public IBinder asBinder() { return null; }
                        });

                    } else {
                        // Si el jugador no acepta, finaliza la actividad tras un mensaje de despedida
                        BuddySDK.Speech.startSpeaking("De acuerdo, nos vemos pronto.");
                        new Handler().postDelayed(() -> finish(), 3000);
                    }
                }
            }

            @Override
            public void onError(String s) {
                // En caso de error, vuelve a intentarlo
                runOnUiThread(MainActivity.this::waitForStartConfirmation);
            }
        });
    }

    /**
     * Inicializa las referencias a los elementos visuales del juego.
     * Se asocian los componentes de la interfaz con sus elementos en el layout XML.
     */
    private void initializeGameViews() {
        // Configura el estado de ánimo inicial de Buddy
        BuddySDK.UI.setMood(FacialExpression.NEUTRAL, 1.0, null);

        // Vincula elementos de la interfaz gráfica
        puzzleContainer = findViewById(R.id.puzzleContainer);
        mainImage = findViewById(R.id.mainImage);
        optionImages[0] = findViewById(R.id.option1);
        optionImages[1] = findViewById(R.id.option2);
        optionImages[2] = findViewById(R.id.option3);
        scoreText = findViewById(R.id.scoreText);
        livesText = findViewById(R.id.livesText);
        infoBar = findViewById(R.id.infoCard);
        timerProgress = findViewById(R.id.timerProgress);
        optionsContainer = findViewById(R.id.optionsContainer);

        Log.d("DEBUG", "initializeGameViews ejecutado");
    }



    /**
     * Carga el siguiente puzzle de la lista.
     * Si ya se han mostrado todos los puzzles, finaliza el juego.
     */
    private void loadNextPuzzle() {
        // Detiene el temporizador actual si está activo
        if (timer != null) timer.cancel();

        // Si ya se mostraron todos los puzzles, termina el juego
        if (currentPuzzleIndex >= puzzles.size()) {
            endGame();
            return;
        }

        // Obtiene el puzzle actual según el índice
        Puzzle p = puzzles.get(currentPuzzleIndex);

        // Comprueba si la dificultad ha cambiado para anunciarla
        String newDifficulty = p.difficulty != null ? p.difficulty : "";  // Si no hay dificultad, se asigna cadena vacía

        if (!newDifficulty.equals(currentDifficulty)) {
            // Si la dificultad ha cambiado, se actualiza y se anuncia antes de continuar
            currentDifficulty = newDifficulty;
            announceDifficultyThenContinue(currentDifficulty, () -> loadPuzzleContent(p));
        } else {
            // Si no ha cambiado, se carga el contenido directamente
            loadPuzzleContent(p);
        }
    }


    /**
     * Carga el contenido visual y auditivo del puzzle en la interfaz del usuario.
     * Prepara las imágenes, el texto, y los botones interactivos.
     * @param p Puzzle que se va a mostrar
     */
    private void loadPuzzleContent(Puzzle p) {
        // Carga la imagen principal del puzzle usando Glide, desactivando la caché para asegurar que se vea la imagen más reciente
        Glide.with(this)
                .load(new File(p.mainImagePath))
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(mainImage);

        // Oculta temporalmente contenedores hasta que se complete la animación de entrada
        optionsContainer.setVisibility(View.GONE);
        timerProgress.setVisibility(View.GONE);
        puzzleContainer.setVisibility(View.VISIBLE);

        // Ejecuta en el hilo principal para actualizar UI y usar la voz de Buddy
        runOnUiThread(() -> {
            // Buddy dice el enunciado del puzzle (prompt)
            BuddySDK.Speech.startSpeaking(p.prompt, new ITTSCallback.Stub() {
                @Override
                public void onSuccess(String s) {
                    runOnUiThread(() -> {
                        // Anima el temporizador con opacidad y escala
                        timerProgress.setAlpha(0f);
                        timerProgress.setScaleX(0.8f);
                        timerProgress.setScaleY(0.8f);
                        timerProgress.setVisibility(View.VISIBLE);
                        timerProgress.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(500).start();

                        // Anima el contenedor de opciones
                        optionsContainer.setAlpha(0f);
                        optionsContainer.setScaleX(0.8f);
                        optionsContainer.setScaleY(0.8f);
                        optionsContainer.setVisibility(View.VISIBLE);
                        optionsContainer.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(500).start();

                        // Mezcla aleatoriamente las posiciones de las opciones
                        Integer[] indices = {0, 1, 2};
                        Collections.shuffle(Arrays.asList(indices));

                        // Muestra las tres opciones con imágenes mezcladas
                        for (int i = 0; i < 3; i++) {
                            int idx = indices[i];
                            Glide.with(MainActivity.this)
                                    .load(new File(p.optionPaths[idx]))
                                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                                    .skipMemoryCache(true)
                                    .into(optionImages[i]);

                            int finalIdx = idx;

                            // Asocia un listener a cada opción para verificar si es la respuesta correcta
                            optionImages[i].setOnClickListener(v -> {
                                playSound(R.raw.button_selection); // Sonido al hacer clic
                                if (timer != null) timer.cancel(); // Detiene el temporizador
                                checkAnswer(finalIdx == p.correctOptionIndex); // Verifica la respuesta
                            });
                        }

                        // Registra el tiempo de inicio para estadísticas
                        preguntaStartTime = System.currentTimeMillis();

                        // Inicia el temporizador para responder
                        startCountdown();
                    });
                }

                @Override public void onError(String s) {}
                @Override public void onPause() {}
                @Override public void onResume() {}
                @Override public IBinder asBinder() { return null; }
            });
        });
    }

    /**
     * Anuncia la dificultad actual del puzzle mediante voz y sonido,
     * luego ejecuta la continuación del flujo (por ejemplo, cargar el puzzle).
     *
     * @param difficulty    Dificultad actual del puzzle ("facil", "normal", "dificil").
     * @param continuation  Acción a ejecutar después del anuncio (típicamente cargar el puzzle).
     */
    private void announceDifficultyThenContinue(String difficulty, Runnable continuation) {
        String message;

        // Selecciona el mensaje y sonido según la dificultad
        switch (difficulty) {
            case "facil":
                playSound(R.raw.level_up); // Reproduce sonido de nivel
                message = "Empezamos con las preguntas fáciles.";
                break;
            case "normal":
                playSound(R.raw.level_up);
                message = "Comenzamos las preguntas de dificultad media.";
                break;
            case "dificil":
                playSound(R.raw.level_up);
                message = "Prepárate, estas son las preguntas más difíciles.";
                break;
            default:
                message = ""; // En caso de dificultad desconocida, no hay anuncio
        }

        // Oculta el contenedor del puzzle mientras se hace el anuncio
        runOnUiThread(() -> puzzleContainer.setVisibility(View.GONE));

        // Si hay mensaje que anunciar, se realiza con voz
        if (!message.isEmpty()) {
            BuddySDK.Speech.startSpeaking(message, new ITTSCallback.Stub() {
                @Override
                public void onSuccess(String s) {
                    // Al terminar de hablar, vuelve a mostrar el puzzle con una animación y continúa
                    runOnUiThread(() -> {
                        puzzleContainer.setAlpha(0f); // Lo hace transparente
                        puzzleContainer.setVisibility(View.VISIBLE); // Lo vuelve visible
                        puzzleContainer.animate().alpha(1f).setDuration(500).start(); // Animación de aparición
                        continuation.run(); // Ejecuta la acción principal (ej. cargar puzzle)
                    });
                }

                @Override public IBinder asBinder() { return null; }
                @Override public void onError(String s) {}
                @Override public void onPause() {}
                @Override public void onResume() {}
            });
        } else {
            // Si no hay mensaje que decir, simplemente continúa
            runOnUiThread(continuation);
        }
    }

    /**
     * Verifica si la respuesta del usuario es correcta y actualiza el estado del juego.
     *
     * @param correct true si la respuesta fue correcta, false si fue incorrecta.
     */
    private void checkAnswer(boolean correct) {
        if (correct) {
            // Si es correcta, poner a Buddy contento (LEDs amarillos)
            BuddySDK.UI.setMood(FacialExpression.HAPPY, 1.0, null);
            score++; // Aumenta la puntuación
            updateUI(); // Actualiza el texto de la interfaz
            currentPuzzleIndex++; // Avanza al siguiente puzzle
            showBuddyFace(true); // Muestra cara feliz
        } else {
            // Si es incorrecta, expresión triste
            BuddySDK.UI.setMood(FacialExpression.SAD, 1.0, null);
            lives--; // Resta una vida
            updateUI(); // Actualiza vidas en pantalla
            currentPuzzleIndex++; // Avanza igualmente al siguiente puzzle

            String finalText;
            if (lives <= 0) {
                // Si ya no quedan vidas, finaliza el juego
                puzzleContainer.setVisibility(View.GONE);
                playSound(R.raw.game_fail);

                finalText = "Vaya, te has quedado sin vidas. La próxima te saldrá mejor.";

                // Buddy habla y luego se finaliza el juego
                BuddySDK.Speech.startSpeaking(finalText, new ITTSCallback.Stub() {
                    @Override
                    public void onSuccess(String s) {
                        runOnUiThread(() -> endGame());
                    }

                    @Override
                    public void onError(String s) {
                        runOnUiThread(() -> endGame()); // fallback por si falla la voz
                    }

                    @Override public void onPause() {}
                    @Override public void onResume() {}
                    @Override public IBinder asBinder() { return null; }
                });
            } else {
                // Si todavía hay vidas, muestra cara triste pero continúa
                showBuddyFace(false);
            }
        }

        // Calcula el tiempo que el jugador tardó en responder en segundos
        long tiempoRespuesta = (System.currentTimeMillis() - preguntaStartTime) / 1000;

        // Guarda esta respuesta para las estadísticas
        respuestas.add(new RespuestaStats(currentPuzzleIndex, tiempoRespuesta, correct));
    }

    /**
     * Actualiza la interfaz con el nombre del jugador, puntuación y vidas restantes.
     */
    @SuppressLint("SetTextI18n")
    private void updateUI() {
        // Muestra nombre y puntos del jugador
        scoreText.setText("Nombre: " + playerName + " | Puntos: " + score);

        // Muestra las vidas restantes con un corazón
        livesText.setText("❤\uFE0F: " + lives);
    }

    /**
     * Muestra una expresión facial y animación en Buddy según si la respuesta fue correcta o incorrecta.
     * También reproduce un sonido y da retroalimentación hablada antes de avanzar al siguiente puzzle.
     *
     * @param happy Indica si la respuesta fue correcta (true) o incorrecta (false).
     */
    private void showBuddyFace(boolean happy) {
        runOnUiThread(() -> {
            // Oculta el contenedor de puzzle con una animación de desvanecimiento
            puzzleContainer.animate().alpha(0f).setDuration(500).withEndAction(() -> {
                puzzleContainer.setVisibility(View.GONE);

                if (happy) {
                    // ✅ RESPUESTA CORRECTA
                    BuddySDK.UI.setMood(FacialExpression.HAPPY, 1.0, null); // Cara feliz
                    playSound(R.raw.winbanjo); // Sonido de victoria

                    // Elegir frases aleatorias de felicitación y continuación
                    String praise = String.format(praisePhrases[new Random().nextInt(praisePhrases.length)], playerName);
                    String continuation = continuePhrases[new Random().nextInt(continuePhrases.length)];
                    String fullText = praise + " " + continuation;

                    // Variables para coordinar animación y voz
                    AtomicInteger doneCount = new AtomicInteger(0);
                    AtomicBoolean avanzado = new AtomicBoolean(false);

                    Runnable tryContinue = () -> {
                        int count = doneCount.incrementAndGet();
                        Log.d("DEBUG", "tryContinue llamado: count = " + count);
                        if (count >= 2 && !avanzado.getAndSet(true)) {
                            Log.d("DEBUG", "✅ Ambas tareas completas, avanzando");
                            runOnUiThread(MainActivity.this::showNextPuzzle);
                        }
                    };

                    // Lanza animación y habla en paralelo
                    playHappyAnimation(tryContinue);

                    BuddySDK.Speech.startSpeaking(fullText, new ITTSCallback.Stub() {
                        @Override public void onSuccess(String s) { tryContinue.run(); }
                        @Override public void onError(String s) {}
                        @Override public void onPause() {}
                        @Override public void onResume() {}
                        @Override public IBinder asBinder() { return null; }
                    });

                } else {
                    // ❌ RESPUESTA INCORRECTA
                    BuddySDK.UI.setMood(FacialExpression.SAD, 1.0, null); // Cara triste
                    playSound(R.raw.game_fail); // Sonido de error

                    // Frase de ánimo y recordatorio de vidas restantes
                    String phrase = timesOver[new Random().nextInt(timesOver.length)];
                    String intentoTexto = (lives == 1) ? "Te queda 1 vida." : "Te quedan " + lives + " vidas.";
                    String fullText = phrase + " " + intentoTexto;

                    AtomicInteger doneCount = new AtomicInteger(0);
                    Runnable tryContinue = () -> {
                        if (doneCount.incrementAndGet() == 2) {
                            runOnUiThread(MainActivity.this::showNextPuzzle);
                        }
                    };

                    playSadAnimation(tryContinue);

                    BuddySDK.Speech.startSpeaking(fullText, new ITTSCallback.Stub() {
                        @Override public void onSuccess(String s) { tryContinue.run(); }
                        @Override public void onError(String s) {}
                        @Override public void onPause() {}
                        @Override public void onResume() {}
                        @Override public IBinder asBinder() { return null; }
                    });
                }
            }).start(); // Inicia la animación y lógica
        });
    }

    /**
     * Prepara y muestra el siguiente puzzle en pantalla.
     * Reinicia la vista principal y la interfaz del juego.
     */
    private void showNextPuzzle() {
        setContentView(R.layout.activity_main); // Recarga la vista principal
        initializeGameViews(); // Reinicializa referencias a vistas del layout

        // Anima el contenedor del puzzle de vuelta a pantalla
        puzzleContainer.setAlpha(0f);
        puzzleContainer.setVisibility(View.VISIBLE);
        puzzleContainer.animate().alpha(1f).setDuration(500).start();

        // Reinicia la expresión facial de Buddy
        BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL);
        BuddySDK.UI.setMood(FacialExpression.NEUTRAL, 1.0, null);

        // Asocia el layout como cara de Buddy
        BuddySDK.UI.setViewAsFace(findViewById(R.id.puzzleContainer));

        updateUI();       // Actualiza puntuación y vidas
        loadNextPuzzle(); // Carga el próximo puzzle
    }

    /**
     * Inicia una nueva partida cargando los puzzles y configurando la vista del juego.
     */
    private void startGame() {
        setContentView(R.layout.activity_main); // Carga la vista principal del juego
        initializeGameViews(); // Inicializa las vistas y componentes

        // Configura a Buddy con expresión neutral
        BuddySDK.UI.setMood(FacialExpression.NEUTRAL, 1.0, null);
        BuddySDK.UI.setViewAsFace(findViewById(R.id.puzzleContainer));

        // Genera la lista de puzzles a usar
        puzzles = PuzzleGenerator.generate(this, selectedGame);

        updateUI();       // Muestra puntuación inicial y vidas
        loadNextPuzzle(); // Carga el primer puzzle
    }

    /**
     * Guarda en un archivo local las estadísticas del jugador en formato JSON y
     * construye un objeto resumen con los datos agregados.
     *
     * @return Un objeto ResumenStats con datos agregados de la sesión actual.
     */
    private ResumenStats saveAndUploadStats() {
        try {
            int correctas = 0;
            int tiempoTotal = 0;
            JSONArray respuestaArray = new JSONArray(); // Lista de respuestas individuales

            // Recorre las respuestas registradas
            for (RespuestaStats r : respuestas) {
                JSONObject obj = new JSONObject();
                obj.put("pregunta", r.pregunta);       // Número de pregunta
                obj.put("tiempo", r.tiempo);           // Tiempo en responder
                obj.put("correcta", r.correcta);       // ¿Fue correcta?
                respuestaArray.put(obj);              // Agrega al arreglo de resultados

                tiempoTotal += r.tiempo;
                if (r.correcta) correctas++;
            }

            int total = respuestas.size();
            int incorrectas = total - correctas;
            int porcentajeAciertos = total > 0 ? (correctas * 100) / total : 0;
            int porcentajeFallos = total > 0 ? (incorrectas * 100) / total : 0;
            int mediaTiempo = total > 0 ? tiempoTotal / total : 0;
            porcentaje_final = porcentajeAciertos; // Guarda para uso futuro en la clase

            // Construcción del objeto JSON final
            JSONObject json = new JSONObject();
            json.put("nombre", playerName);
            json.put("fecha", new java.text.SimpleDateFormat("dd--MM--yyyy HH:mm:ss", Locale.getDefault()).format(new java.util.Date()));
            json.put("correctas", correctas);
            json.put("incorrectas", incorrectas);
            json.put("totalPreguntas", total);
            json.put("porcentajeAciertos", porcentajeAciertos);
            json.put("porcentajeFallos", porcentajeFallos);
            json.put("mediaTiempoSegundos", mediaTiempo);
            json.put("tiempoTotalSegundos", tiempoTotal);
            json.put("respuestas", respuestaArray);

            // Guarda el archivo localmente con nombre personalizado
            File statsFile = new File(getFilesDir(), "estadisticas_" + playerName + ".json");
            FileWriter writer = new FileWriter(statsFile);
            writer.write(json.toString(4)); // Indenta con 4 espacios
            writer.close();

            // Devuelve un objeto con los datos resumidos
            return new ResumenStats(correctas, incorrectas, total, porcentajeAciertos, porcentajeFallos, mediaTiempo, tiempoTotal);

        } catch (Exception e) {
            e.printStackTrace();
            // En caso de error, devuelve datos vacíos
            return new ResumenStats(0, 0, 0, 0, 0, 0, 0);
        }
    }

    /**
     * Inicia una cuenta regresiva para responder el puzzle actual.
     * El tiempo total es de 30 segundos. Cambia los colores del temporizador y los LEDs
     * a medida que se acorta el tiempo.
     */
    private void startCountdown() {
        final int totalTime = 30; // 30 segundos por pregunta

        // Cancela cualquier cuenta regresiva anterior
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        // Configura el ProgressBar visual
        timerProgress.setMax(totalTime);
        timerProgress.setProgress(totalTime);

        // Inicia el conteo con un pequeño retraso
        new Handler().postDelayed(() -> {
            timer = new CountDownTimer(totalTime * 1000L, 1000) {
                public void onTick(long millisUntilFinished) {
                    int secondsLeft = (int) (millisUntilFinished / 1000);
                    timerProgress.setProgress(secondsLeft);

                    // Cambia el color del progreso y LEDs según el tiempo restante
                    if (secondsLeft <= 10) {
                        timerProgress.setProgressDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.progress_red));
                        BuddySDK.USB.updateAllLed("#cc0000", null); // Rojo
                    } else if (secondsLeft <= 20) {
                        timerProgress.setProgressDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.progress_orange));
                        BuddySDK.USB.updateAllLed("#ffc700", null); // Amarillo
                    } else {
                        timerProgress.setProgressDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.progress_green));
                        BuddySDK.USB.updateAllLed("#53b200", null); // Verde
                    }
                }

                @Override
                public void onFinish() {
                    // Si se acaba el tiempo, se marca la respuesta como incorrecta automáticamente
                    runOnUiThread(() -> checkAnswer(false));
                }
            }.start();
        }, 400); // Espera 400 ms antes de comenzar la cuenta regresiva
    }

    /**
     * Finaliza la partida actual, cancela el temporizador y muestra el mensaje de cierre.
     * Guarda las estadísticas del jugador y muestra un resumen de resultados.
     */
    private void endGame() {
        if (timer != null) timer.cancel();  // Cancela el temporizador si aún está activo

        puzzleContainer.setVisibility(View.GONE); // Oculta el contenedor de puzzles

        // Muestra cara feliz y anuncia que la partida terminó
        BuddySDK.UI.setMood(FacialExpression.HAPPY, 1.0, null);
        BuddySDK.Speech.startSpeaking("La partida ha terminado. Estos son tus resultados.", new ITTSCallback.Stub() {
            @Override public IBinder asBinder() { return null; }

            @Override
            public void onError(String s) {
                // En caso de error al hablar, guarda resultados igual y continúa
                saveAndUploadStats();
                sayFinalResults();
            }

            @Override public void onPause() {}
            @Override public void onResume() {}

            @Override
            public void onSuccess(String s) {
                // Al terminar de hablar exitosamente, guarda y muestra resultados
                saveAndUploadStats();
                sayFinalResults();
            }
        });
    }

    /**
     * Muestra un mensaje personalizado según el porcentaje de aciertos del jugador.
     * Usa expresiones faciales de Buddy y habla el resultado.
     */
    private void sayFinalResults() {
        int porcentaje = porcentaje_final; // Recupera el porcentaje guardado

        // Define el mensaje según el rango del porcentaje
        String feedback;
        if (porcentaje < 50) {
            feedback = "Tu puntuación ha sido baja. ¡La próxima te irá mejor!";
        } else if (porcentaje < 70) {
            feedback = "Tu puntuación ha sido medianamente buena.";
        } else if (porcentaje < 90) {
            feedback = "Has obtenido una buena puntuación.";
        } else {
            feedback = "¡Has tenido una puntuación excelente!";
        }

        // Cambia la expresión a neutral antes de hablar
        BuddySDK.UI.setMood(FacialExpression.NEUTRAL, 1.0, null);

        // Buddy dice el mensaje de retroalimentación
        BuddySDK.Speech.startSpeaking(feedback, new ITTSCallback.Stub() {
            @Override public IBinder asBinder() { return null; }

            @Override
            public void onError(String s) {
                // En caso de error, pasa al siguiente paso igualmente
                runOnUiThread(() -> continueAfterSpeech());
            }

            @Override public void onPause() {}
            @Override public void onResume() {}

            @Override
            public void onSuccess(String s) {
                // Al terminar de hablar correctamente, continúa el flujo
                runOnUiThread(() -> continueAfterSpeech());
            }
        });
    }

    /**
     * Se llama después de que Buddy termina de hablar el resumen del juego.
     * Muestra una pantalla con las estadísticas del jugador y permite reiniciar la partida.
     */
    private void continueAfterSpeech() {
        // Restaura expresión facial y estado emocional a neutral
        BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL);
        BuddySDK.UI.setMood(FacialExpression.NEUTRAL, 1.0, null);

        // Reproduce sonido de nivel completado
        playSound(R.raw.game_level_complete);

        runOnUiThread(() -> {
            // Cambia al layout del resumen de juego
            setContentView(R.layout.activity_game_summary);
            View summaryCard = findViewById(R.id.summaryCard);

            // Aplica animación de aparición al resumen
            if (summaryCard != null) {
                summaryCard.setScaleX(0.8f);
                summaryCard.setScaleY(0.8f);
                summaryCard.setAlpha(0f);
                summaryCard.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(1f)
                        .setDuration(500)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
            }

            // Referencias a elementos de UI para mostrar estadísticas
            TextView summaryStats = findViewById(R.id.summaryStats);
            ProgressBar summaryProgress = findViewById(R.id.summaryProgress);
            TextView percentageText = findViewById(R.id.percentageText);
            Button restartButton = findViewById(R.id.restartGameButton);

            // Calcula resultados del juego
            int correctas = 0;
            int tiempoTotal = 0;

            for (RespuestaStats r : respuestas) {
                tiempoTotal += r.tiempo;
                if (r.correcta) correctas++;
            }

            int total = respuestas.size();
            int incorrectas = total - correctas;
            int porcentaje = (total > 0) ? (correctas * 100 / total) : 0;
            int porcentajeFallos = (total > 0) ? (incorrectas * 100 / total) : 0;

            // Arma el resumen textual de estadísticas
            String resumen = "👤 Jugador: " + playerName + "\n"
                    + "❓ Preguntas: " + total + "\n"
                    + "✅ Correctas: " + correctas + "\n"
                    + "❌ Incorrectas: " + incorrectas + "\n"
                    + "🎯 Aciertos: " + porcentaje + "%\n"
                    + "💥 Fallos: " + porcentajeFallos + "%\n"
                    + "⏱️ Tiempo: " + tiempoTotal + " segundos";

            summaryStats.setText(resumen); // Muestra resumen

            // Anima la barra de progreso del porcentaje de aciertos
            ValueAnimator animator = ValueAnimator.ofInt(0, porcentaje);
            animator.setDuration(1000);
            animator.setInterpolator(new DecelerateInterpolator());

            animator.addUpdateListener(animation -> {
                int progress = (int) animation.getAnimatedValue();
                summaryProgress.setProgress(progress);
                percentageText.setText(progress + "%");
            });

            animator.start();

            // Lógica para reiniciar el juego al pulsar el botón
            restartButton.setOnClickListener(v -> {
                playSound(R.raw.game_start);

                // Reset de variables de juego
                score = 0;
                lives = 5;
                currentPuzzleIndex = 0;
                playerName = "";
                respuestas.clear();

                // Oculta el resumen con animación
                View summaryLayout = findViewById(R.id.summaryCard);
                if (summaryLayout != null) {
                    summaryLayout.animate()
                            .alpha(0f)
                            .scaleX(0.8f)
                            .scaleY(0.8f)
                            .setDuration(500)
                            .withEndAction(() -> {
                                // Vuelve a la pantalla de bienvenida
                                setContentView(R.layout.welcome_screen);

                                View welcomeLayout = findViewById(R.id.welcomeLayout);
                                TextView loadingText = findViewById(R.id.loadingText);
                                ProgressBar loadingCircle = findViewById(R.id.loadingCircle);

                                loadingText.setAlpha(0f);
                                loadingCircle.setAlpha(0f);

                                loadingText.animate().alpha(1f).setDuration(1000).start();
                                loadingCircle.animate().alpha(1f).setDuration(1000).start();

                                // Buddy da la bienvenida de nuevo y lanza el selector
                                BuddySDK.Speech.startSpeaking("Volviendo al inicio del juego...", new ITTSCallback.Stub() {
                                    @Override public IBinder asBinder() { return null; }
                                    @Override public void onError(String s) {}
                                    @Override public void onPause() {}
                                    @Override public void onResume() {}
                                    @Override public void onSuccess(String s) {
                                        runOnUiThread(() -> {
                                            new Handler().postDelayed(() -> {
                                                welcomeLayout.animate()
                                                        .alpha(0f)
                                                        .setDuration(700)
                                                        .withEndAction(() -> returnMatchSelector())
                                                        .start();
                                            }, 1000);
                                        });
                                    }
                                });
                            })
                            .start();
                }
            });
        });
    }

    /**
     * Activa el "Modo Administrador", mostrando una interfaz con la IP local del servidor web
     * y permitiendo al usuario salir del modo para volver al flujo del juego normal.
     */
    private void runAdminMode() {
        // Cambia el layout a la pantalla de IP del administrador
        setContentView(R.layout.ip_admin_layout);
        View adminLayout = findViewById(R.id.ip_localhost);  // ID del contenedor principal (ConstraintLayout)

        // Aplica una animación de entrada si el layout existe
        if (adminLayout != null) {
            adminLayout.setAlpha(0f);
            adminLayout.setScaleX(0.8f);
            adminLayout.setScaleY(0.8f);
            adminLayout.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(500)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        // Muestra la IP del servidor web en la interfaz
        TextView ipText = findViewById(R.id.ipText);
        Button exitAdminButton = findViewById(R.id.exitAdminButton);
        String ip = getLocalIpAddress();
        String url = "http://" + ip + ":8085";
        ipText.setText("Dirección web local:\n" + url);

        // Muestra una expresión de "pensando" en Buddy y un mensaje hablado
        BuddySDK.UI.setFacialExpression(FacialExpression.THINKING);
        BuddySDK.UI.setMood(FacialExpression.THINKING, 1.0, null);
        BuddySDK.Speech.startSpeaking("Modo administrador activado. Puedes conectarte a la dirección mostrada.");

        // Intenta iniciar el servidor web para editar preguntas desde navegador
        try {
            server = new WebPuzzleUploader(this, selectedGame) {
                @Override
                public void onServerClosed() {
                    runOnUiThread(() -> promptForName()); // Vuelve a pedir nombre al cerrar servidor
                }
            };
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("ADMIN_SERVER", "Error iniciando el servidor web", e);
        }

        // Define el comportamiento del botón para salir del modo administrador
        exitAdminButton.setOnClickListener(v -> {
            playSound(R.raw.game_start); // Sonido al salir

            // Detiene el servidor si está corriendo
            if (server != null) {
                server.stop();
                server = null;
            }

            // Cambia la expresión de Buddy a neutral y anuncia cierre
            BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL);
            BuddySDK.UI.setMood(FacialExpression.NEUTRAL, 1.0, null);
            BuddySDK.Speech.startSpeaking("Cerrando modo administrador", new ITTSCallback.Stub() {
                @Override
                public void onSuccess(String s) throws RemoteException {
                    runOnUiThread(() -> {
                        // Asegura que el servidor se cierra correctamente
                        if (server != null) {
                            server.onServerClosed();
                            server = null;
                        }

                        // Oculta la pantalla de admin con animación y vuelve a bienvenida
                        View adminLayout = findViewById(R.id.ip_localhost);
                        if (adminLayout != null) {
                            adminLayout.animate()
                                    .alpha(0f)
                                    .scaleX(0.8f)
                                    .scaleY(0.8f)
                                    .setDuration(500)
                                    .withEndAction(() -> {
                                        // Carga pantalla de bienvenida
                                        setContentView(R.layout.welcome_screen);

                                        View welcomeLayout = findViewById(R.id.welcomeLayout);
                                        TextView loadingText = findViewById(R.id.loadingText);
                                        ProgressBar loadingCircle = findViewById(R.id.loadingCircle);

                                        loadingText.setAlpha(0f);
                                        loadingCircle.setAlpha(0f);
                                        loadingText.animate().alpha(1f).setDuration(1000).start();
                                        loadingCircle.animate().alpha(1f).setDuration(1000).start();

                                        // Habla Buddy y vuelve al flujo de pedir nombre
                                        BuddySDK.Speech.startSpeaking("Volviendo al inicio del juego...", new ITTSCallback.Stub() {
                                            @Override public IBinder asBinder() { return null; }
                                            @Override public void onError(String s) {}
                                            @Override public void onPause() {}
                                            @Override public void onResume() {}
                                            @Override public void onSuccess(String s) {
                                                runOnUiThread(() -> {
                                                    new Handler().postDelayed(() -> {
                                                        welcomeLayout.animate()
                                                                .alpha(0f)
                                                                .setDuration(700)
                                                                .withEndAction(() -> {
                                                                    // Reinicia el estado y pide nombre de nuevo
                                                                    score = 0;
                                                                    lives = 5;
                                                                    currentPuzzleIndex = 0;
                                                                    playerName = "";
                                                                    respuestas.clear();

                                                                    setContentView(R.layout.name_input_layout);
                                                                    initializeNameInput();
                                                                    promptForName();
                                                                }).start();
                                                    }, 1000);
                                                });
                                            }
                                        });
                                    })
                                    .start();
                        }
                    });
                }

                @Override public void onError(String s) {}
                @Override public void onPause() throws RemoteException {}
                @Override public void onResume() throws RemoteException {}
                @Override public IBinder asBinder() { return null; }
            });
        });
    }


    /**
     * Obtiene la dirección IP local del dispositivo (IPv4).
     * Se usa, por ejemplo, para mostrar en modo administrador la URL de acceso al servidor web local.
     *
     * @return Dirección IP en formato string o "127.0.0.1" si hay error.
     */
    public String getLocalIpAddress() {
        try {
            // Recorre todas las interfaces de red del dispositivo
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();

                // Recorre todas las direcciones IP de esa interfaz
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();

                    // Filtra solo direcciones IPv4 no loopback (no 127.0.0.1)
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress(); // Devuelve la IP local
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace(); // Imprime error si falla
        }

        return "127.0.0.1"; // Valor por defecto en caso de error
    }


    /**
     * Lanza la actividad que permite al usuario seleccionar la partida (juego) desde una lista.
     * También reproduce una voz con instrucciones antes de abrir el selector.
     */
    private void returnMatchSelector() {
        // Buddy habla antes de mostrar el selector
        BuddySDK.Speech.startSpeaking("Selecciona la partida", new ITTSCallback.Stub() {
            @Override public IBinder asBinder() { return null; }

            @Override public void onError(String s) {}

            @Override public void onPause() {}

            @Override public void onResume() {}

            @Override
            public void onSuccess(String s) {
                // Reproduce sonido y lanza el selector de partidas
                playSound(R.raw.pop_panel);
                Intent intent = new Intent(MainActivity.this, GameSelectorActivity.class);
                startActivityForResult(intent, 123); // Código arbitrario para recibir respuesta
            }
        });
    }

    /**
     * Reproduce un sonido de recurso (por ejemplo, efecto al pulsar botón).
     * @param resId ID del recurso de sonido a reproducir.
     */
    private void playSound(int resId) {
        MediaPlayer mediaPlayer = MediaPlayer.create(this, resId);
        if (mediaPlayer != null) {
            // Libera recursos cuando termine
            mediaPlayer.setOnCompletionListener(MediaPlayer::release);
            mediaPlayer.start();
        }
    }


    /**
     * Método llamado automáticamente cuando se retorna de otra actividad con un resultado.
     * En este caso, se usa para manejar la respuesta del selector de partidas (código 123).
     *
     * @param requestCode Código usado para identificar qué actividad respondió.
     * @param resultCode Resultado de la actividad (RESULT_OK si fue exitoso).
     * @param data Intent que contiene datos de resultado, como el nombre del juego seleccionado.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Verifica que la respuesta venga del selector de juegos y que fue exitosa
        if (requestCode == 123 && resultCode == RESULT_OK) {
            // Obtiene el nombre del juego que el usuario ha seleccionado
            selectedGame = data.getStringExtra("selectedGame");

            // Cambia la vista al layout para introducir el nombre del jugador
            setContentView(R.layout.name_input_layout);

            // Anima la aparición del formulario de nombre
            View nameLayout = findViewById(R.id.nameInputLayout);
            if (nameLayout != null) {
                nameLayout.setAlpha(0f);
                nameLayout.setScaleX(0.8f);
                nameLayout.setScaleY(0.8f);
                nameLayout.animate()
                        .alpha(1f)                    // Aumenta opacidad a 100%
                        .scaleX(1f).scaleY(1f)        // Escala de vuelta al 100%
                        .setDuration(500)             // Duración de animación
                        .setInterpolator(new DecelerateInterpolator()) // Suavizado
                        .start();
            }

            // Inicializa los campos del formulario y pide el nombre al jugador
            initializeNameInput();
            promptForName();
        }
    }

    /**
     * Ejecuta una animación feliz aleatoria del personaje Buddy.
     * Reinicia el sistema de animación, configura la expresión facial y el estado de ánimo,
     * y lanza una de las cuatro secuencias posibles.
     *
     * @param onFinish Acción a ejecutar al finalizar la animación.
     */
    private void playHappyAnimation(Runnable onFinish) {
        // Establece expresión y estado de ánimo feliz
        BuddySDK.UI.setFacialExpression(FacialExpression.HAPPY);
        BuddySDK.UI.setMood(FacialExpression.HAPPY, 1.0, null);

        // Limpia la cola de animaciones y resetea el estado
        animationQueue.clear();
        isPlayingAnimation = false;

        // Selecciona aleatoriamente una de las 4 secuencias posibles
        int variant = new Random().nextInt(4);
        switch (variant) {
            case 0: playHappySequence1(onFinish); break;
            case 1: playHappySequence2(onFinish); break;
            case 2: playHappySequence3(onFinish); break;
            case 3: playHappySequence4(onFinish); break;
        }
    }

    /**
     * Secuencia 1: Giro de 360 grados.
     */
    private void playHappySequence1(Runnable onFinish) {
        enqueueAnimation(() -> rotate360(() -> finishAnimation(onFinish)));
    }

    /**
     * Secuencia 2: Movimiento de afirmación + giro a la izquierda y derecha.
     */
    private void playHappySequence2(Runnable onFinish) {
        enqueueAnimation(() -> moveHeadYes(25f, 40f, () -> playNextAnimation()));
        enqueueAnimation(() -> turnLeft(() -> playNextAnimation()));
        enqueueAnimation(() -> turnRight(() -> finishAnimation(onFinish)));
    }

    /**
     * Secuencia 3: Solo movimiento de afirmación.
     */
    private void playHappySequence3(Runnable onFinish) {
        enqueueAnimation(() -> moveHeadYes(30f, 40f, () -> finishAnimation(onFinish)));
    }

    /**
     * Secuencia 4: Movimiento hacia atrás, giro, y movimiento hacia adelante.
     */
    private void playHappySequence4(Runnable onFinish) {
        enqueueAnimation(() -> moveBackward(20f, 0.2f, () -> playNextAnimation()));
        enqueueAnimation(() -> rotate360(() -> playNextAnimation()));
        enqueueAnimation(() -> moveForward(20f, 0.2f, () -> finishAnimation(onFinish)));
    }

    /**
     * Ejecuta una animación triste aleatoria del personaje Buddy.
     * Se utiliza para representar visualmente una respuesta incorrecta o un fallo.
     *
     * @param onFinish Acción a ejecutar al finalizar la animación.
     */
    private void playSadAnimation(Runnable onFinish) {
        // Establece la expresión y el estado de ánimo triste
        BuddySDK.UI.setFacialExpression(FacialExpression.SAD);
        BuddySDK.UI.setMood(FacialExpression.SAD, 1.0, null);

        // Reinicia la cola de animaciones y el estado
        animationQueue.clear();
        isPlayingAnimation = false;

        // Selecciona aleatoriamente una de las 3 secuencias tristes disponibles
        int variant = new Random().nextInt(3);
        switch (variant) {
            case 0: playSadSequence1(onFinish); break;
            case 1: playSadSequence2(onFinish); break;
            case 2: playSadSequence3(onFinish); break;
        }
    }

    /**
     * Secuencia triste 1: retrocede, niega con la cabeza, y avanza.
     */
    private void playSadSequence1(Runnable onFinish) {
        enqueueAnimation(() -> moveBackward(15f, 0.2f, () -> playNextAnimation()));
        enqueueAnimation(() -> moveHeadNo(35f, 40f, () -> playNextAnimation()));
        enqueueAnimation(() -> moveForward(15f, 0.2f, () -> finishAnimation(onFinish)));
    }

    /**
     * Secuencia triste 2: gira a la derecha, niega con la cabeza, y gira a la izquierda.
     */
    private void playSadSequence2(Runnable onFinish) {
        enqueueAnimation(() -> turnRight(() -> playNextAnimation()));
        enqueueAnimation(() -> moveHeadNo(35f, 40f, () -> playNextAnimation()));
        enqueueAnimation(() -> turnLeft(() -> finishAnimation(onFinish)));
    }

    /**
     * Secuencia triste 3: solo movimiento de negación con la cabeza.
     */
    private void playSadSequence3(Runnable onFinish) {
        enqueueAnimation(() -> moveHeadNo(25f, 30f, () -> finishAnimation(onFinish)));
    }

    /**
     * Añade una animación a la cola y, si no se está ejecutando otra, la lanza inmediatamente.
     *
     * @param animation Acción animada que se desea ejecutar.
     */
    private void enqueueAnimation(Runnable animation) {
        animationQueue.add(animation);
        if (!isPlayingAnimation) {
            playNextAnimation();
        }
    }

    /**
     * Ejecuta la siguiente animación de la cola, si hay alguna pendiente.
     */
    private void playNextAnimation() {
        Runnable next = animationQueue.poll();
        if (next != null) {
            isPlayingAnimation = true;
            next.run();
        } else {
            Log.w("ANIM", "⚠ Cola vacía o animación no terminó, limpiando estado");
            isPlayingAnimation = false;
        }
    }

    /**
     * Finaliza la animación actual y ejecuta la acción final proporcionada.
     *
     * @param onFinish Acción a ejecutar tras la animación.
     */
    private void finishAnimation(Runnable onFinish) {
        Log.d("ANIMACIÓN", "🎬 Animación finalizada");
        if (onFinish != null) onFinish.run();
    }

    /**
     * Centra completamente la cabeza de Buddy en ambos ejes:
     * - Vertical (movimiento "sí")
     * - Horizontal (movimiento "no")
     */
    private void centerBuddyHeadFully2() {
        // Habilita el motor vertical (YES)
        BuddySDK.USB.enableYesMove(1, new IUsbCommadRsp.Stub() {
            @Override
            public void onSuccess(String s1) {
                // Si se habilita correctamente, habilita el motor horizontal (NO)
                BuddySDK.USB.enableNoMove(1, new IUsbCommadRsp.Stub() {
                    @Override
                    public void onSuccess(String s2) {
                        Log.d("BUDDY_HEAD", "YES and NO motors enabled");

                        // Centra la cabeza verticalmente (movimiento "sí")
                        BuddySDK.USB.buddySayYes(20f, 0f, new IUsbCommadRsp.Stub() {
                            @Override
                            public void onSuccess(String s3) {
                                Log.d("BUDDY_HEAD", "Vertical head centered");

                                // Luego centra horizontalmente (movimiento "no")
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

    /**
     * Mueve la cabeza de Buddy hacia adelante (como diciendo “sí”) y la vuelve a centrar.
     *
     * @param angle Ángulo máximo al que mover (1° a 45° recomendado)
     * @param speed Velocidad del movimiento (0.1 a 49.2)
     * @param onFinish Acción a ejecutar al finalizar el movimiento
     */
    private void moveHeadYes(float angle, float speed, Runnable onFinish) {
        // Asegura que el ángulo y la velocidad estén dentro de un rango seguro
        float safeSpeed = Math.min(Math.max(speed, 0.1f), 49.2f);
        float safeAngle = Math.min(Math.max(angle, 1f), 45f);

        Log.e("Dentro de moveHeadYes", "angle: " + safeAngle + ", speed: " + safeSpeed);
        Log.d("BUDDY_HEAD", "Estado del motor YES antes de mover: " + BuddySDK.Actuators.getYesStatus());

        // Protección para asegurar que solo se llame una vez a onFinish
        AtomicBoolean terminado = new AtomicBoolean(false);
        Runnable seguroFinish = () -> {
            if (!terminado.getAndSet(true)) {
                Log.d("ANIMACIÓN", "🎬 Animación finalizada");
                if (onFinish != null) runOnUiThread(onFinish);
            }
        };

        // Si el motor está ocupado, reintenta después de 100 ms
        if ("SET".equals(BuddySDK.Actuators.getYesStatus())) {
            Log.w("BUDDY_HEAD", "⚠ Motor YES ocupado, esperando para reintentar...");
            new Handler(Looper.getMainLooper()).postDelayed(() -> moveHeadYes(angle, speed, onFinish), 100);
            return;
        }

        // Reinicia el motor (deshabilita y vuelve a habilitar)
        BuddySDK.USB.enableYesMove(0, new IUsbCommadRsp.Stub() {
            @Override
            public void onSuccess(String s1) {
                BuddySDK.USB.enableYesMove(1, new IUsbCommadRsp.Stub() {
                    @Override
                    public void onSuccess(String s2) {
                        Log.d("BUDDY_HEAD", "✅ Motor YES reiniciado y habilitado");

                        // Realiza el movimiento hacia adelante (sí)
                        BuddySDK.USB.buddySayYes(safeSpeed, safeAngle, new IUsbCommadRsp.Stub() {
                            @Override
                            public void onSuccess(String result) {
                                // Si el movimiento terminó correctamente, vuelve al centro
                                if ("YES_MOVE_FINISHED".equals(result)) {
                                    Log.d("BUDDY_HEAD", "✅ Movimiento YES completado, volviendo al centro");
                                    BuddySDK.USB.buddySayYes(safeSpeed, 0f, new IUsbCommadRsp.Stub() {
                                        @Override
                                        public void onSuccess(String s) {
                                            seguroFinish.run();
                                        }

                                        @Override
                                        public void onFailed(String error) {
                                            Log.e("BUDDY_HEAD", "❌ Error al centrar cabeza YES: " + error);
                                            seguroFinish.run();
                                        }
                                    });
                                } else {
                                    // Si no se recibió "YES_MOVE_FINISHED", terminar igual
                                    seguroFinish.run();
                                }
                            }

                            @Override
                            public void onFailed(String error) {
                                Log.e("BUDDY_HEAD", "❌ Fallo en movimiento YES: " + error);
                                seguroFinish.run();
                            }
                        });
                    }

                    @Override
                    public void onFailed(String e) {
                        Log.e("BUDDY_HEAD", "❌ Error al re-habilitar motor YES: " + e);
                        seguroFinish.run();
                    }
                });
            }

            @Override
            public void onFailed(String e) {
                Log.e("BUDDY_HEAD", "❌ Error al deshabilitar motor YES: " + e);
                seguroFinish.run();
            }
        });
    }

    /**
     * Mueve la cabeza de Buddy hacia los lados (como diciendo "no") y la vuelve a centrar.
     *
     * @param angle Ángulo máximo de giro (1° a 90°)
     * @param speed Velocidad del movimiento (0.1 a 140)
     * @param onFinish Acción a ejecutar al finalizar
     */
    private void moveHeadNo(float angle, float speed, Runnable onFinish) {
        // Limita velocidad y ángulo a rangos seguros
        float safeSpeed = Math.max(0.1f, Math.min(speed, 140f));
        float safeAngle = Math.max(1f, Math.min(Math.abs(angle), 90f));

        Log.e("Dentro de moveHeadNo", "angle: " + safeAngle + ", speed: " + safeSpeed);
        Log.d("BUDDY_HEAD", "Estado del motor NO antes de mover: " + BuddySDK.Actuators.getNoStatus());

        // Prevención para que onFinish no se llame más de una vez
        AtomicBoolean terminado = new AtomicBoolean(false);
        Runnable seguroFinish = () -> {
            if (!terminado.getAndSet(true)) {
                Log.d("ANIMACIÓN", "🎬 Animación finalizada");
                if (onFinish != null) runOnUiThread(onFinish);
            }
        };

        // Si el motor está ocupado, reintenta más tarde
        if ("SET".equals(BuddySDK.Actuators.getNoStatus())) {
            Log.w("BUDDY_HEAD", "⚠ Motor NO ocupado, esperando para reintentar...");
            new Handler(Looper.getMainLooper()).postDelayed(() -> moveHeadNo(angle, speed, onFinish), 100);
            return;
        }

        // Reinicia el motor NO (deshabilita y habilita)
        BuddySDK.USB.enableNoMove(0, new IUsbCommadRsp.Stub() {
            @Override
            public void onSuccess(String s1) {
                BuddySDK.USB.enableNoMove(1, new IUsbCommadRsp.Stub() {
                    @Override
                    public void onSuccess(String s2) {
                        Log.d("BUDDY_HEAD", "✅ Motor NO reiniciado y habilitado");

                        // Ejecuta el movimiento lateral (NO)
                        BuddySDK.USB.buddySayNo(safeSpeed, safeAngle, new IUsbCommadRsp.Stub() {
                            @Override
                            public void onSuccess(String result) {
                                // Si el movimiento terminó correctamente, vuelve al centro
                                if ("NO_MOVE_FINISHED".equals(result)) {
                                    Log.d("BUDDY_HEAD", "✅ Movimiento NO completado, volviendo al centro");
                                    BuddySDK.USB.buddySayNo(safeSpeed, 0f, new IUsbCommadRsp.Stub() {
                                        @Override
                                        public void onSuccess(String s) {
                                            seguroFinish.run();
                                        }

                                        @Override
                                        public void onFailed(String error) {
                                            Log.e("BUDDY_HEAD", "❌ Error al centrar cabeza NO: " + error);
                                            seguroFinish.run();
                                        }
                                    });
                                } else {
                                    seguroFinish.run();
                                }
                            }

                            @Override
                            public void onFailed(String error) {
                                Log.e("BUDDY_HEAD", "❌ Fallo en movimiento NO: " + error);
                                seguroFinish.run();
                            }
                        });
                    }

                    @Override
                    public void onFailed(String e) {
                        Log.e("BUDDY_HEAD", "❌ Error al re-habilitar motor NO: " + e);
                        seguroFinish.run();
                    }
                });
            }

            @Override
            public void onFailed(String e) {
                Log.e("BUDDY_HEAD", "❌ Error al deshabilitar motor NO: " + e);
                seguroFinish.run();
            }
        });
    }

    /**
     * Realiza una rotación completa (360 grados) de Buddy.
     *
     * @param onFinish Callback que se ejecuta cuando termina la rotación.
     */
    private void rotate360(Runnable onFinish) {
        // Habilita las ruedas del robot antes de moverlo
        BuddySDK.USB.enableWheels(1, 1, new IUsbCommadRsp.Stub() {
            @Override
            public void onSuccess(String s) {
                // Inicia la rotación en sentido horario 360 grados
                BuddySDK.USB.rotateBuddy(100f, 360f, new IUsbCommadRsp.Stub() {
                    @Override
                    public void onSuccess(String result) {
                        // Cuando termina la rotación, ejecuta el callback (si existe)
                        if ("WHEEL_MOVE_FINISHED".equals(result) && onFinish != null)
                            runOnUiThread(onFinish);
                    }

                    @Override
                    public void onFailed(String error) {
                        Log.e("BUDDY_ROTATE", "❌ Error al rotar 360: " + error);
                        // Aun con error, llamamos a onFinish para no quedar colgados
                        if (onFinish != null) runOnUiThread(onFinish);
                    }
                });
            }

            @Override
            public void onFailed(String error) {
                Log.e("BUDDY_WHEELS", "❌ No se pudieron habilitar las ruedas: " + error);
                if (onFinish != null) runOnUiThread(onFinish);
            }
        });
    }

    /**
     * Mueve a Buddy hacia adelante una distancia dada con una velocidad específica.
     *
     * @param speed Velocidad del movimiento
     * @param distance Distancia a recorrer
     * @param onFinish Acción a ejecutar al terminar
     */
    private void moveForward(float speed, float distance, Runnable onFinish) {
        // Primero habilita las ruedas
        BuddySDK.USB.enableWheels(1, 1, new IUsbCommadRsp.Stub() {
            @Override
            public void onSuccess(String s) {
                // Una vez habilitadas, realiza el movimiento
                BuddySDK.USB.moveBuddy(speed, distance, new IUsbCommadRsp.Stub() {
                    @Override
                    public void onSuccess(String s) {
                        // Si el movimiento finaliza correctamente, ejecuta callback
                        if ("WHEEL_MOVE_FINISHED".equals(s) && onFinish != null)
                            runOnUiThread(onFinish);
                    }

                    @Override
                    public void onFailed(String error) {
                        Log.e("BUDDY_WHEELS", "Error al mover adelante: " + error);
                        if (onFinish != null) runOnUiThread(onFinish);
                    }
                });
            }

            @Override
            public void onFailed(String error) {
                Log.e("BUDDY_WHEELS", "No se pudieron habilitar las ruedas: " + error);
                if (onFinish != null) runOnUiThread(onFinish);
            }
        });
    }

    /**
     * Gira a Buddy hacia la izquierda 90 grados.
     *
     * @param onFinish Acción a ejecutar al terminar el giro
     */
    private void turnLeft(Runnable onFinish) {
        // Habilita las ruedas
        BuddySDK.USB.enableWheels(1, 1, new IUsbCommadRsp.Stub() {
            @Override
            public void onSuccess(String s) {
                // Ejecuta el giro con velocidad 85 y ángulo +90 (izquierda)
                BuddySDK.USB.rotateBuddy(85f, 90f, new IUsbCommadRsp.Stub() {
                    @Override
                    public void onSuccess(String s) {
                        if ("WHEEL_MOVE_FINISHED".equals(s) && onFinish != null)
                            runOnUiThread(onFinish); // Ejecutar callback si terminó bien
                    }

                    @Override
                    public void onFailed(String error) {
                        Log.e("BUDDY_TURN", "Error al girar izquierda: " + error);
                        if (onFinish != null) runOnUiThread(onFinish);
                    }
                });
            }

            @Override
            public void onFailed(String error) {
                Log.e("BUDDY_WHEELS", "No se pudieron habilitar las ruedas: " + error);
                if (onFinish != null) runOnUiThread(onFinish);
            }
        });
    }


    /**
     * Gira a Buddy hacia la derecha 90 grados.
     *
     * @param onFinish Acción a ejecutar al terminar el giro
     */
    private void turnRight(Runnable onFinish) {
        // Habilita las ruedas
        BuddySDK.USB.enableWheels(1, 1, new IUsbCommadRsp.Stub() {
            @Override
            public void onSuccess(String s) {
                // Ejecuta el giro con velocidad 85 y ángulo -90 (derecha)
                BuddySDK.USB.rotateBuddy(85f, -90f, new IUsbCommadRsp.Stub() {
                    @Override
                    public void onSuccess(String s) {
                        if ("WHEEL_MOVE_FINISHED".equals(s) && onFinish != null)
                            runOnUiThread(onFinish);
                    }

                    @Override
                    public void onFailed(String error) {
                        Log.e("BUDDY_TURN", "Error al girar derecha: " + error);
                        if (onFinish != null) runOnUiThread(onFinish);
                    }
                });
            }

            @Override
            public void onFailed(String error) {
                Log.e("BUDDY_WHEELS", "No se pudieron habilitar las ruedas: " + error);
                if (onFinish != null) runOnUiThread(onFinish);
            }
        });
    }

    /**
     * Mueve a Buddy hacia atrás con una velocidad y distancia dadas.
     *
     * @param speed Velocidad del movimiento (limitada entre 0.05 y 0.7)
     * @param distance Distancia positiva (se convierte a negativa para ir hacia atrás)
     * @param onFinish Acción a ejecutar al finalizar el movimiento
     */
    private void moveBackward(float speed, float distance, Runnable onFinish) {
        // Asegura que la velocidad esté en el rango permitido
        float validSpeed = Math.max(0.05f, Math.min(speed, 0.7f));
        float reverseDistance = -Math.abs(distance); // Asegura que sea negativa

        // Habilita las ruedas
        BuddySDK.USB.enableWheels(1, 1, new IUsbCommadRsp.Stub() {
            @Override
            public void onSuccess(String s) {
                // Ejecuta el movimiento hacia atrás
                BuddySDK.USB.moveBuddy(validSpeed, reverseDistance, new IUsbCommadRsp.Stub() {
                    @Override
                    public void onSuccess(String s) {
                        if ("WHEEL_MOVE_FINISHED".equals(s) && onFinish != null)
                            runOnUiThread(onFinish);
                    }

                    @Override
                    public void onFailed(String error) {
                        Log.e("BUDDY_BACK", "Error al retroceder: " + error);
                        if (onFinish != null) runOnUiThread(onFinish);
                    }
                });
            }

            @Override
            public void onFailed(String error) {
                Log.e("BUDDY_WHEELS", "No se pudieron habilitar las ruedas: " + error);
                if (onFinish != null) runOnUiThread(onFinish);
            }
        });
    }

}

/**
 * Representa una respuesta individual de una pregunta del juego.
 */
class RespuestaStats {
    int pregunta;       // Índice o número de la pregunta
    long tiempo;        // Tiempo en segundos que tardó el jugador en responder
    boolean correcta;   // Indica si la respuesta fue correcta o no

    /**
     * Constructor de una respuesta.
     *
     * @param pregunta Índice de la pregunta
     * @param tiempo Tiempo de respuesta en segundos
     * @param correcta true si la respuesta fue correcta, false si no
     */
    public RespuestaStats(int pregunta, long tiempo, boolean correcta) {
        this.pregunta = pregunta;
        this.tiempo = tiempo;
        this.correcta = correcta;
    }
}


/**
 * Contiene un resumen general del rendimiento del jugador al finalizar la partida.
 */
class ResumenStats {
    int correctas;             // Número de respuestas correctas
    int incorrectas;           // Número de respuestas incorrectas
    int total;                 // Total de preguntas respondidas
    int porcentajeAciertos;    // Porcentaje de respuestas correctas
    int porcentajeFallos;      // Porcentaje de respuestas incorrectas
    int mediaTiempo;           // Tiempo medio por respuesta (en segundos)
    int tiempoTotal;           // Tiempo total de todas las respuestas (en segundos)

    /**
     * Constructor del resumen de estadísticas.
     *
     * @param correctas Número de aciertos
     * @param incorrectas Número de fallos
     * @param total Total de preguntas respondidas
     * @param porcentajeAciertos % de respuestas correctas
     * @param porcentajeFallos % de respuestas incorrectas
     * @param mediaTiempo Tiempo medio por pregunta
     * @param tiempoTotal Tiempo total de respuesta
     */
    public ResumenStats(int correctas, int incorrectas, int total, int porcentajeAciertos, int porcentajeFallos, int mediaTiempo, int tiempoTotal) {
        this.correctas = correctas;
        this.incorrectas = incorrectas;
        this.total = total;
        this.porcentajeAciertos = porcentajeAciertos;
        this.porcentajeFallos = porcentajeFallos;
        this.mediaTiempo = mediaTiempo;
        this.tiempoTotal = tiempoTotal;
    }

}

