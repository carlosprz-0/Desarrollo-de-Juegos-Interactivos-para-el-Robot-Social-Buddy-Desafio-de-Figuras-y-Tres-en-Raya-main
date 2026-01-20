package com.bfr.helloworld;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bfr.buddy.usb.shared.IUsbCommadRsp;
import com.bfr.buddysdk.BuddyActivity;
import com.bfr.buddysdk.BuddySDK;
import com.bfr.buddy.ui.shared.FacialExpression;
import com.bfr.buddy.speech.shared.ITTSCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import android.view.animation.AnimationUtils;
import android.media.MediaPlayer;
import android.util.Log;




public class MainActivity extends BuddyActivity {

    // Indica si el usuario puede hacer clic en los botones del tablero
    private boolean inputEnabled = true;

    // Contenedor principal con el menú lateral (si se usa)
    private DrawerLayout drawerLayout;

    // Matriz 3x3 de botones para representar el tablero de tres en raya
    private Button[][] buttons = new Button[3][3];

    // Bandera para saber si es el turno del jugador X (jugador 1)
    private boolean playerXTurn = true;

    // Indica si se juega contra la IA (true) o contra otro jugador (false)
    private boolean playAgainstAI;

    // Puntuación del jugador X
    private int scorePlayerX = 0;

    // Puntuación del jugador O
    private int scorePlayerO = 0;

    // Lista de botones que forman una combinación ganadora
    private List<Button> winningButtons = new ArrayList<>();

    // Nombres de los jugadores (por defecto)
    private String namePlayerX = "Jugador 1";
    private String namePlayerO = "Jugador 2";

    // Elementos de la interfaz relacionados con la introducción del nombre del jugador
    private EditText nameInput;
    private Button nameConfirmButton;
    private View replayView; // Vista del botón o panel para repetir partida

    // Utilidades para tareas diferidas o repetidas
    private final Handler handler = new Handler();

    // Generador de números aleatorios (para elegir frases o movimientos aleatorios)
    private final Random random = new Random();

    // Vista para mostrar la tarjeta de resultado al final de una partida
    private View resultCardView;

    // Vista donde se introduce el nombre del jugador
    private View nameInputLayout;

    // Cola de animaciones (movimientos o gestos del robot)
    private final Queue<Runnable> animationQueue = new LinkedList<>();

    // Indica si una animación está actualmente en ejecución
    private boolean isPlayingAnimation = false;

    //Frases de interaccion de Buddy
    private final String[] buddyTurnPhrases = {
            "Ahora juego yo.",
            "Mi turno.",
            "Déjame pensar... ya sé.",
            "Me toca mover.",
            "Voy yo ahora.",
            "Atento, que muevo yo.",
            "Veamos qué hago ahora.",
            "Hora de mi jugada.",
            "Prepárate... ¡voy!",
            "Mmm... ¿cuál será mi mejor jugada?",
            "Esta te va a sorprender.",
            "Voy a intentar esta jugada...",
            "A ver si esta funciona...",
            "¿Listo para esto?",
            "Espero que estés preparado.",
            "¡Aquí voy!",
            "No te confíes...",
            "¡Esta será buena!",
            "Con cuidado... ahora voy yo.",
            "¡Turno de Buddy!"
    };


    private final String[] buddyRematchPhrases = {
            "¡Vamos otra vez!",
            "Buena suerte en esta ronda.",
            "A ver si me ganas esta vez.",
            "¿Listo para otra?",
            "¡Prepara tu estrategia!"
    };

    private final String[] buddyLosePhrases = {
            "¡Qué buena jugada, %s! Has ganado.",
            "Vaya, %s, me has vencido. ¡Felicidades!",
            "Me ganaste esta vez, %s. ¡Bien hecho!",
            "¡Eso fue inesperado, %s! Buena victoria."
    };

    private final String[] buddyWinPhrases = {
            "¡He ganado esta vez! Pero estoy seguro de que la próxima puedes vencerme.",
            "¡Eso fue divertido! ¿Te animas a intentarlo otra vez?",
            "¡Victoria para mí! Pero no te rindas, jugaste muy bien.",
            "¡Lo logré! Pero tú también estuviste cerca, ¿jugamos otra?",
            "¡Gané! Pero esto apenas empieza, ¡vamos por otra ronda!",
            "¡Uy! Me llevé la victoria, pero seguro que me ganas en la siguiente.",
            "¡Esa estuvo buena! Pero quiero ver cómo me ganas la próxima.",
            "¡Qué partida! Esta vez gané, pero estoy seguro de que tienes una sorpresa para mí la siguiente vez."
    };

    private final String[] buddyDrawPhrases = {
            "¡Empate! Estuvo muy reñido.",
            "Hemos empatado, eso fue intenso.",
            "¡Qué buen duelo! Nadie gana esta vez.",
            "Parece que pensamos igual, ¿jugamos otra?",
            "Un empate... interesante. Vamos de nuevo.",
            "Igualados. La próxima será decisiva."
    };

    private final String[] playerWinPhrases = {
            "¡Felicidades %s! Has ganado esta partida.",
            "¡Qué gran jugada %s! Bien merecido.",
            "¡Victoria lograda por %s! Enhorabuena.",
            "¡Impresionante %s! Estuviste genial.",
            "¡Eso fue emocionante, %s! Muy bien jugado.",
            "¡Bien hecho %s! ¿Listos para otra ronda?",
            "¡Qué partida más reñida! Felicidades, %s.",
            "¡%s es el ganador! Fue un juego genial."
    };

    // Bandera que indica si una partida ya ha comenzado
    private boolean gameStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Obtiene las preferencias del juego guardadas previamente
        SharedPreferences prefs = getSharedPreferences("GamePrefs", MODE_PRIVATE);

        // Recupera si el modo actual es contra la IA o PvP
        playAgainstAI = prefs.getBoolean("playAgainstAI", false);

        // Recupera la puntuación del jugador X guardada previamente
        scorePlayerX = prefs.getInt("scorePlayerX", 0);

        // Recupera la puntuación del jugador O guardada previamente
        scorePlayerO = prefs.getInt("scorePlayerO", 0);

        // Establece el layout de la actividad a la pantalla de entrada de nombre
        setContentView(R.layout.name_input_layout);

        // Obtiene referencias a los elementos de entrada de nombre en la interfaz
        nameInput = findViewById(R.id.nameInput);
        nameConfirmButton = findViewById(R.id.nameConfirmButton);
        nameInputLayout = findViewById(R.id.nameInputLayout);

        // Oculta todos los elementos de entrada de nombre al iniciar
        nameInputLayout.setVisibility(View.GONE);
        nameInput.setVisibility(View.GONE);
        nameConfirmButton.setVisibility(View.GONE);
    }

    @Override
    public void onSDKReady() {
        // Establece la expresión facial neutral al inicio
        BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL);

        // Centra completamente la cabeza del robot (horizontal y verticalmente)
        centerBuddyHeadFully();

        // Verifica si el juego es contra la inteligencia artificial
        if (playAgainstAI) {
            // Si se juega contra Buddy (IA), pide el nombre del jugador
            BuddySDK.Speech.startSpeaking("Hola, ¿cómo te llamas?", new ITTSCallback.Stub() {
                @Override
                public void onSuccess(String s) {
                    // Al terminar de hablar, muestra los campos de entrada de nombre
                    runOnUiThread(() -> {
                        animateShow(nameInputLayout);
                        animateShow(nameInput);
                        animateShow(nameConfirmButton);

                        // Acción al presionar el botón de confirmar nombre
                        nameConfirmButton.setOnClickListener(v -> {
                            String name = nameInput.getText().toString().trim();
                            if (name.isEmpty()) name = "Jugador";

                            // Se guarda el nombre del jugador y se asigna "Buddy" como oponente
                            namePlayerX = name;
                            namePlayerO = "Buddy";

                            // Oculta los elementos de entrada con animación inversa
                            animateShow2(nameInputLayout);
                            animateShow2(nameInput);
                            animateShow2(nameConfirmButton);

                            // Confirma con voz el nombre ingresado y comienza el juego
                            BuddySDK.Speech.startSpeaking("Perfecto, un placer conocerte " + namePlayerX + ". ¡Empecemos!", new ITTSCallback.Stub() {
                                @Override
                                public void onSuccess(String s) {
                                    // Muestra una expresión feliz y reproduce sonido de inicio
                                    BuddySDK.UI.setFacialExpression(FacialExpression.HAPPY);
                                    BuddySDK.UI.setMood(FacialExpression.HAPPY, 1.0, null);
                                    playSound(R.raw.go_soundbit);

                                    // Hace una animación de giro y comienza el juego
                                    rotate360(MainActivity.this::startGame);
                                }

                                @Override public void onError(String s) {}
                                @Override public void onPause() {}
                                @Override public void onResume() {}
                                @Override public IBinder asBinder() { return null; }
                            });
                        });
                    });
                }

                @Override public void onError(String s) {}
                @Override public void onPause() {}
                @Override public void onResume() {}
                @Override public IBinder asBinder() { return null; }
            });
        } else {
            // Si el modo es jugador contra jugador (PvP), se piden dos nombres
            BuddySDK.Speech.startSpeaking("Jugador uno, ¿cómo te llamas?", new ITTSCallback.Stub() {
                @Override
                public void onSuccess(String s) {
                    runOnUiThread(() -> {
                        animateShow(nameInputLayout);
                        animateShow(nameInput);
                        animateShow(nameConfirmButton);

                        nameConfirmButton.setOnClickListener(v -> {
                            String name1 = nameInput.getText().toString().trim();
                            if (name1.isEmpty()) name1 = "Jugador 1";
                            namePlayerX = name1;

                            // Limpia el campo y cambia el hint para el segundo jugador
                            nameInput.setText("");
                            nameInput.setHint("Jugador 2");

                            // Pregunta por el segundo nombre
                            BuddySDK.Speech.startSpeaking("Jugador dos, ¿cómo te llamas?", new ITTSCallback.Stub() {
                                @Override
                                public void onSuccess(String s) {
                                    runOnUiThread(() -> {
                                        nameConfirmButton.setOnClickListener(v2 -> {
                                            String name2 = nameInput.getText().toString().trim();
                                            if (name2.isEmpty()) name2 = "Jugador 2";
                                            namePlayerO = name2;

                                            // Oculta el formulario y confirma con voz los nombres
                                            animateShow2(nameInputLayout);
                                            animateShow2(nameInput);
                                            animateShow2(nameConfirmButton);
                                            BuddySDK.Speech.startSpeaking("Perfecto, " + namePlayerX + " y " + namePlayerO + ". ¡Empecemos!", new ITTSCallback.Stub() {
                                                @Override
                                                public void onSuccess(String s) {
                                                    BuddySDK.UI.setFacialExpression(FacialExpression.HAPPY);
                                                    BuddySDK.UI.setMood(FacialExpression.HAPPY, 1.0, null);
                                                    playSound(R.raw.go_soundbit);
                                                    rotate360(MainActivity.this::startGame);
                                                }

                                                @Override public void onError(String s) {}
                                                @Override public void onPause() {}
                                                @Override public void onResume() {}
                                                @Override public IBinder asBinder() { return null; }
                                            });
                                        });
                                    });
                                }

                                @Override public void onError(String s) {}
                                @Override public void onPause() {}
                                @Override public void onResume() {}
                                @Override public IBinder asBinder() { return null; }
                            });
                        });
                    });
                }

                @Override public void onError(String s) {}
                @Override public void onPause() {}
                @Override public void onResume() {}
                @Override public IBinder asBinder() { return null; }
            });
        }
    }

    private void startGame() {
        // Previene que el juego se inicie más de una vez
        if (gameStarted) return;
        gameStarted = true;

        // Mensaje de depuración que se muestra en Logcat
        Log.d("DEBUG", "startGame ejecutado");

        // Establece la expresión facial y el estado de ánimo neutral en Buddy
        BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL);
        BuddySDK.UI.setMood(FacialExpression.NEUTRAL, 1.0, null);

        // Carga el layout principal del juego (donde está el tablero)
        setContentView(R.layout.activity_main);

        // Obtiene una referencia al layout principal (puede ser un contenedor del tablero)
        drawerLayout = findViewById(R.id.drawerLayout);

        // Inicializa el tablero de juego y los botones
        initializeBoard();

        // Actualiza en pantalla los puntajes actuales de los jugadores
        updateScores();

        // Aplica una animación para mostrar el layout del juego
        animateShow(drawerLayout);
    }



    private void initializeBoard() {
        // Mensaje de depuración que se muestra en Logcat
        Log.d("DEBUG", "initializeBoard ejecutado");

        // Matriz de IDs que corresponden a los botones del tablero 3x3
        int[][] ids = {
                {R.id.button_00, R.id.button_01, R.id.button_02},
                {R.id.button_10, R.id.button_11, R.id.button_12},
                {R.id.button_20, R.id.button_21, R.id.button_22}
        };

        // Recorre la matriz para inicializar cada botón
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                int row = i, col = j;
                buttons[i][j] = findViewById(ids[i][j]); // Asocia el botón del layout
                buttons[i][j].setOnClickListener(null);   // Elimina cualquier listener anterior
                buttons[i][j].setText("");               // Limpia el texto del botón
                buttons[i][j].setEnabled(true);          // Asegura que el botón esté activo
                buttons[i][j].setOnClickListener(v -> playerMove(row, col)); // Asigna nueva acción al pulsar
            }
        }

        // Limpia la lista de botones ganadores (resaltados en una victoria)
        winningButtons.clear();
    }

    private void updateScores() {
        // Referencias a los elementos de texto donde se muestran los puntajes
        TextView scoreX = findViewById(R.id.playerXScore);
        TextView scoreO = findViewById(R.id.playerOScore);

        // Actualiza el texto con los nombres y puntajes si los TextViews existen
        if (scoreX != null && scoreO != null) {
            scoreX.setText(namePlayerX + " (X): " + scorePlayerX);
            scoreO.setText(namePlayerO + " (O): " + scorePlayerO);
        }
    }


    // Verifica si existe una línea ganadora en el tablero
    private boolean checkWin() {
        // Define todas las posibles combinaciones ganadoras (filas, columnas, diagonales)
        int[][][] lines = {
                {{0,0},{0,1},{0,2}}, // Fila 1
                {{1,0},{1,1},{1,2}}, // Fila 2
                {{2,0},{2,1},{2,2}}, // Fila 3
                {{0,0},{1,0},{2,0}}, // Columna 1
                {{0,1},{1,1},{2,1}}, // Columna 2
                {{0,2},{1,2},{2,2}}, // Columna 3
                {{0,0},{1,1},{2,2}}, // Diagonal principal
                {{0,2},{1,1},{2,0}}  // Diagonal secundaria
        };

        // Recorre cada combinación y verifica si hay coincidencia en los tres botones
        for (int[][] line : lines) {
            Button b1 = buttons[line[0][0]][line[0][1]];
            Button b2 = buttons[line[1][0]][line[1][1]];
            Button b3 = buttons[line[2][0]][line[2][1]];

            // Si los tres botones tienen el mismo símbolo (X u O)
            if (equals(b1, b2, b3)) {
                // Guarda los botones ganadores para resaltarlos visualmente luego
                winningButtons = new ArrayList<>(Arrays.asList(b1, b2, b3));
                return true; // Se detectó una victoria
            }
        }
        return false; // No se detectó ninguna victoria
    }

    // Verifica si hay empate: todos los botones están ocupados y no hay ganador
    private boolean checkDraw() {
        for (Button[] row : buttons) {
            for (Button b : row) {
                if (b.getText().toString().isEmpty()) return false; // Hay al menos un botón libre
            }
        }
        return true; // Todos los botones están ocupados
    }

    // Comprueba si tres botones tienen el mismo texto no vacío (usado en checkWin)
    private boolean equals(Button b1, Button b2, Button b3) {
        String s = b1.getText().toString();
        return !s.isEmpty() &&
                s.equals(b2.getText().toString()) &&
                s.equals(b3.getText().toString());
    }

    private void playerMove(int row, int col) {
        // Si la entrada está deshabilitada, no se permite jugar
        if (!inputEnabled) return;

        // Si el botón ya tiene una marca, se ignora el toque
        if (!buttons[row][col].getText().toString().isEmpty()) return;

        // Coloca "X" o "O" según el turno actual
        buttons[row][col].setText(playerXTurn ? "X" : "O");

        // Asigna un color según el jugador
        buttons[row][col].setTextColor(
                ContextCompat.getColor(this, playerXTurn
                        ? android.R.color.holo_blue_dark // Jugador X: azul
                        : android.R.color.holo_red_dark) // Jugador O: rojo
        );

        // Desactiva el botón para que no se pueda volver a pulsar
        buttons[row][col].setEnabled(false);

        // Reproduce un sonido al pulsar el botón
        playSound(R.raw.click_button);

        // Aplica una animación al presionar el botón
        buttons[row][col].startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_up));

        // Verifica si alguien ha ganado tras esta jugada
        if (checkWin()) {
            // Suma un punto al jugador correspondiente
            if (playerXTurn) scorePlayerX++; else scorePlayerO++;
            updateScores();

            // Anuncia el resultado según el modo de juego (vs Buddy o PvP)
            if (playAgainstAI && !playerXTurn) {
                announceResult("Buddy ha ganado.");
            } else if (playAgainstAI && playerXTurn) {
                announceResult(namePlayerX + " ha ganado.");
            } else {
                announceResult(playerXTurn ? namePlayerX + " ha ganado." : namePlayerO + " ha ganado.");
            }
            return; // Sale del método si alguien ha ganado
        }

        // Verifica si hay un empate
        if (checkDraw()) {
            announceResult("Empate");
            return;
        }

        // Cambia el turno al otro jugador
        playerXTurn = !playerXTurn;

        // Si el modo es contra la IA y ahora le toca a Buddy
        if (playAgainstAI && !playerXTurn) {
            inputEnabled = false; // Se desactiva la entrada para evitar que el jugador pulse durante el turno de Buddy

            // Selecciona una frase aleatoria para que Buddy la diga antes de mover
            String phrase = buddyTurnPhrases[random.nextInt(buddyTurnPhrases.length)];
            BuddySDK.Speech.startSpeaking(phrase, new ITTSCallback.Stub() {
                @Override
                public void onSuccess(String s) {
                    // Ejecuta el movimiento de la IA en el hilo principal
                    runOnUiThread(MainActivity.this::move);
                }

                // Métodos necesarios pero no utilizados
                @Override public void onPause() {}
                @Override public void onResume() {}
                @Override public void onError(String s) throws RemoteException {}
                @Override public IBinder asBinder() { return null; }
            });
        }
    }

    private void move() {
        // Verificación extra: si por alguna razón es el turno del jugador, no ejecuta la IA
        if (playerXTurn) return;

        // Obtiene la dificultad almacenada en las preferencias compartidas (por defecto: "normal")
        String difficulty = getSharedPreferences("GamePrefs", MODE_PRIVATE).getString("difficulty", "normal");

        // Según la dificultad, elige el tipo de movimiento:
        // - "dificil": usa un algoritmo más inteligente
        // - cualquier otra (incluye "normal"): movimiento básico
        int[] move = difficulty.equals("dificil") ? findBestMoveSmart() : findBestMove();

        // Si se ha encontrado una jugada válida
        if (move != null) {
            int row = move[0];
            int col = move[1];

            // Asegura que la casilla esté vacía antes de marcar
            if (buttons[row][col].getText().toString().isEmpty()) {
                // Marca con "O" porque Buddy siempre juega como O
                buttons[row][col].setText("O");
                buttons[row][col].setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
                buttons[row][col].setEnabled(false); // desactiva la casilla
                playSound(R.raw.click_button); // reproduce sonido
                buttons[row][col].startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_up)); // animación
            }

            // Verifica si Buddy ha ganado
            if (checkWin()) {
                scorePlayerO++;      // actualiza su puntaje
                updateScores();      // refleja en UI
                announceResult("Buddy ha ganado."); // muestra el resultado
                return;              // termina el turno
            }

            // Verifica si hay empate
            if (checkDraw()) {
                announceResult("Empate");
                return;
            }

            // Si no ganó ni empató, vuelve el turno al jugador
            playerXTurn = true;
            inputEnabled = true;
        }
    }

    private int[] findBestMove() {
        List<int[]> available = new ArrayList<>();

        // Recorre todas las celdas del tablero
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                // Si la celda está vacía, la añade a la lista de movimientos posibles
                if (buttons[i][j].getText().toString().isEmpty())
                    available.add(new int[]{i, j});

        // Si no hay movimientos disponibles, devuelve null (empate)
        // Si hay, elige uno aleatorio de la lista
        return available.isEmpty() ? null : available.get(random.nextInt(available.size()));
    }

    private int[] findBestMoveSmart() {
        // 1. Intentar ganar en el siguiente movimiento
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                if (buttons[i][j].getText().toString().isEmpty()) {
                    buttons[i][j].setText("O");        // Simula que Buddy juega ahí
                    if (checkWin()) {                 // Si ganaría con esa jugada
                        buttons[i][j].setText("");    // Limpia la simulación
                        return new int[]{i, j};       // Devuelve esa jugada
                    }
                    buttons[i][j].setText("");        // Limpia la simulación
                }

        // 2. Intentar bloquear a X (jugador) si puede ganar en la próxima
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                if (buttons[i][j].getText().toString().isEmpty()) {
                    buttons[i][j].setText("X");        // Simula que el jugador juega ahí
                    if (checkWin()) {                 // Si ganaría el jugador
                        buttons[i][j].setText("");    // Limpia la simulación
                        return new int[]{i, j};       // Buddy se anticipa y bloquea
                    }
                    buttons[i][j].setText("");        // Limpia la simulación
                }

        // 3. Si no hay forma de ganar ni bloquear, mueve aleatoriamente
        return findBestMove();
    }

    private void announceResult(String resultText) {
        // Obtener referencias a las vistas principales del juego
        View gameGrid = findViewById(R.id.gameGrid);
        View gameLayout = findViewById(R.id.gameLayout);

        // Mostrar visualmente las vistas si están ocultas
        if (gameGrid != null) animateShow2(gameGrid);
        if (gameLayout != null) animateShow2(gameLayout);

        // Por defecto, la frase final será el resultado recibido
        String finalPhrase = resultText;

        // Lógica según el resultado del juego
        if (resultText.contains("Buddy ha ganado")) {
            // Si ganó Buddy: animación alegre, sonido de victoria y frase correspondiente
            playSound(R.raw.winbanjo);
            playHappyAnimation(null);
            finalPhrase = buddyWinPhrases[random.nextInt(buddyWinPhrases.length)];

        } else if (playAgainstAI && resultText.contains(namePlayerX)) {
            // Si el jugador humano gana contra Buddy: Buddy muestra alegría pero se reconoce la derrota
            playSound(R.raw.winbanjo);
            playHappyAnimation(null);
            String phraseTemplate = buddyLosePhrases[random.nextInt(buddyLosePhrases.length)];
            finalPhrase = String.format(phraseTemplate, namePlayerX);

        } else if (resultText.contains("Empate")) {
            // Si hay empate: sonido de empate, animación neutral, frase de empate
            playSound(R.raw.draw_game);
            playEmptyAnimation(null);
            finalPhrase = buddyDrawPhrases[random.nextInt(buddyDrawPhrases.length)];

        } else if (!playAgainstAI && (resultText.contains(namePlayerX) || resultText.contains(namePlayerO))) {
            // Si es un juego entre dos personas, se identifica al ganador y se asigna una frase correspondiente
            playSound(R.raw.winbanjo);
            playHappyAnimation(null);
            String winnerName = resultText.contains(namePlayerX) ? namePlayerX : namePlayerO;
            String phraseTemplate = playerWinPhrases[random.nextInt(playerWinPhrases.length)];
            finalPhrase = String.format(phraseTemplate, winnerName);
        }

        // Buddy habla la frase final seleccionada
        BuddySDK.Speech.startSpeaking(finalPhrase, new ITTSCallback.Stub() {
            @Override
            public void onSuccess(String s) {
                // Una vez termina de hablar el resultado, ejecuta lo siguiente en el hilo principal
                runOnUiThread(() -> {
                    // Centra la cabeza de Buddy antes de continuar
                    centerBuddyHeadFully();

                    // Genera una nueva pregunta para jugar otra partida
                    String pregunta = playAgainstAI
                            ? "¿Quieres jugar otra vez?"
                            : "¿Queréis jugar otra vez?";

                    // Buddy hace la pregunta con voz
                    BuddySDK.Speech.startSpeaking(pregunta, new ITTSCallback.Stub() {
                        @Override
                        public void onSuccess(String s) {
                            // Al finalizar la pregunta, vuelve a estado neutral y muestra la tarjeta de resultado
                            BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL);
                            BuddySDK.UI.setMood(FacialExpression.NEUTRAL, 1.0, null);
                            runOnUiThread(() -> showResultCard(resultText));
                        }

                        // Métodos vacíos requeridos por la interfaz
                        @Override public void onError(String s) {}
                        @Override public void onPause() {}
                        @Override public void onResume() {}
                        @Override public IBinder asBinder() { return null; }
                    });
                });
            }

            // Métodos vacíos requeridos por la interfaz
            @Override public void onError(String s) {}
            @Override public void onPause() {}
            @Override public void onResume() {}
            @Override public IBinder asBinder() { return null; }
        });
    }


    // Reproduce un sonido desde un recurso de audio proporcionado (resId)
    private void playSound(int resId) {
        MediaPlayer mediaPlayer = MediaPlayer.create(this, resId); // Crea un reproductor de medios con el recurso
        if (mediaPlayer != null) {
            // Libera recursos cuando el audio termina de reproducirse
            mediaPlayer.setOnCompletionListener(MediaPlayer::release);
            mediaPlayer.start(); // Inicia la reproducción
        }
    }

    // Anima la aparición de una vista: la muestra con escala y opacidad desde valores bajos a normales
    public void animateShow(View view) {
        view.setVisibility(View.VISIBLE);    // Asegura que la vista esté visible
        view.setScaleX(0.8f);                // Escala inicial en X (80%)
        view.setScaleY(0.8f);                // Escala inicial en Y
        view.setAlpha(0f);                   // Comienza totalmente transparente
        view.animate()                       // Inicia la animación
                .alpha(1f)                       // Termina completamente opaca
                .scaleX(1f)                      // Escala final completa en X
                .scaleY(1f)                      // Escala final completa en Y
                .setDuration(400)               // Duración de 400 milisegundos
                .start();                        // Comienza la animación
    }

    // Anima la desaparición de una vista: la reduce en tamaño y la desvanece
    public void animateShow2(View view) {
        view.animate()
                .alpha(0f)                        // Desaparece (transparencia)
                .scaleX(0.8f)                     // Reduce el tamaño en X
                .scaleY(0.8f)                     // Reduce el tamaño en Y
                .setDuration(300)                // Duración de la animación (300 ms)
                .withEndAction(() -> view.setVisibility(View.GONE)) // Al finalizar, oculta la vista completamente
                .start();                         // Ejecuta la animación
    }

    // Hace que Buddy diga una frase aleatoria de rematch (otra ronda) y luego ejecuta una acción al terminar
    private void sayRandomRematchPhrase(Runnable onFinish) {
        // Elige una frase al azar del array buddyRematchPhrases
        String phrase = buddyRematchPhrases[random.nextInt(buddyRematchPhrases.length)];

        // Inicia la reproducción de voz
        BuddySDK.Speech.startSpeaking(phrase, new ITTSCallback.Stub() {
            @Override
            public void onSuccess(String s) {
                // Ejecuta la acción final en el hilo de la interfaz cuando termine de hablar
                runOnUiThread(onFinish);
            }

            // Métodos requeridos por la interfaz, pero no utilizados aquí
            @Override public void onError(String s) {}
            @Override public void onPause() {}
            @Override public void onResume() {}
            @Override public IBinder asBinder() { return null; }
        });
    }

    // Muestra la tarjeta de resultado final del juego, con opciones para repetir o volver al menú
    private void showResultCard(String ganadorTexto) {
        // Reproduce sonido de fin de juego
        playSound(R.raw.game_level_complete);

        // Solo infla la vista de resultado si aún no ha sido creada
        if (resultCardView == null) {
            resultCardView = getLayoutInflater().inflate(R.layout.result_card_layout, null);
            // Añade la vista inflada al contenedor principal del layout del juego
            ((android.widget.FrameLayout) findViewById(R.id.all_gameLayout)).addView(resultCardView);
            // Muestra la vista con una animación de entrada
            animateShow(resultCardView);
        }

        // Obtiene referencias a los elementos dentro de la tarjeta de resultado
        TextView textWinner = resultCardView.findViewById(R.id.textWinner);
        TextView textScores = resultCardView.findViewById(R.id.textScores);
        Button buttonReplay = resultCardView.findViewById(R.id.buttonResultReplay);
        Button buttonMenu = resultCardView.findViewById(R.id.buttonResultMenu);

        // Muestra el texto del ganador
        textWinner.setText(ganadorTexto);

        // Muestra los puntajes acumulados de ambos jugadores
        textScores.setText(namePlayerX + ": " + scorePlayerX + " puntos\n" +
                namePlayerO + ": " + scorePlayerO + " puntos");

        // Acción del botón de "Volver a jugar"
        buttonReplay.setOnClickListener(v -> {
            // Elimina la tarjeta de resultado de la vista
            ((ViewGroup) resultCardView.getParent()).removeView(resultCardView);
            resultCardView = null;

            // Oculta el tablero y el layout principal
            View gameGrid = findViewById(R.id.gameGrid);
            View gameLayout = findViewById(R.id.gameLayout);
            if (gameGrid != null) animateShow2(gameGrid);
            if (gameLayout != null) animateShow(gameLayout);

            // Hace que Buddy diga una frase aleatoria de rematch y reinicia el tablero
            sayRandomRematchPhrase(() -> {
                if (gameGrid != null) animateShow(gameGrid);
                playerXTurn = true;        // Reinicia el turno
                gameStarted = false;      // Marca el juego como no iniciado
                inputEnabled = true;      // Habilita los toques
                initializeBoard();        // Reinicia las celdas
                updateScores();           // Actualiza los puntajes mostrados
            });
        });

        // Acción del botón de "Menú"
        buttonMenu.setOnClickListener(v -> {
            // Limpia preferencias guardadas y vuelve al menú principal
            getSharedPreferences("GamePrefs", MODE_PRIVATE).edit().clear().apply();
            startActivity(new Intent(MainActivity.this, SelectionActivity.class));
            finish(); // Finaliza la actividad actual
        });
    }

    // Ejecuta una animación "feliz" aleatoria cuando Buddy está contento
    private void playHappyAnimation(Runnable onFinish) {
        // Cambia la expresión facial y el estado de ánimo a "feliz"
        BuddySDK.UI.setFacialExpression(FacialExpression.HAPPY);
        BuddySDK.UI.setMood(FacialExpression.HAPPY, 1.0, null);

        // Reinicia la cola de animaciones y su estado
        animationQueue.clear();
        isPlayingAnimation = false;

        // Elige aleatoriamente una de las 4 secuencias felices
        int variant = new Random().nextInt(4);

        switch (variant) {
            case 0: playHappySequence1(onFinish); break;
            case 1: playHappySequence2(onFinish); break;
            case 2: playHappySequence3(onFinish); break;
            case 3: playHappySequence4(onFinish); break;
        }
    }

    // Secuencia 1: Movimiento de cabeza "sí" + giro completo
    private void playHappySequence1(Runnable onFinish) {
        enqueueAnimation(() -> moveHeadYes(25f, 40f, () -> playNextAnimation()));
        enqueueAnimation(() -> rotate360(() -> finishAnimation(onFinish)));
    }

    // Secuencia 2: Movimiento de cabeza "sí" + giro a la izquierda + giro a la derecha
    private void playHappySequence2(Runnable onFinish) {
        enqueueAnimation(() -> moveHeadYes(25f, 40f, () -> playNextAnimation()));
        enqueueAnimation(() -> turnLeft(() -> playNextAnimation()));
        enqueueAnimation(() -> turnRight(() -> finishAnimation(onFinish)));
    }

    // Secuencia 3: Solo movimiento de cabeza "sí" más fuerte
    private void playHappySequence3(Runnable onFinish) {
        enqueueAnimation(() -> moveHeadYes(30f, 40f, () -> finishAnimation(onFinish)));
    }

    // Secuencia 4: Movimiento hacia atrás + giro completo + regreso hacia adelante
    private void playHappySequence4(Runnable onFinish) {
        enqueueAnimation(() -> moveBackward(20f, 0.2f, () -> playNextAnimation()));
        enqueueAnimation(() -> rotate360(() -> playNextAnimation()));
        enqueueAnimation(() -> moveForward(20f, 0.2f, () -> finishAnimation(onFinish)));
    }

    // Ejecuta una animación neutra (vacía) asociada a una expresión pensativa, útil por ejemplo en caso de empate
    private void playEmptyAnimation(Runnable onFinish) {
        // Establece la expresión facial y el estado de ánimo como "pensativo"
        BuddySDK.UI.setFacialExpression(FacialExpression.THINKING);
        BuddySDK.UI.setMood(FacialExpression.THINKING, 1.0, null);

        // Limpia la cola de animaciones y reinicia el estado
        animationQueue.clear();
        isPlayingAnimation = false;

        // Selecciona aleatoriamente una de las tres secuencias
        int variant = new Random().nextInt(3);

        switch (variant) {
            case 0: playEmptySequence1(onFinish); break;
            case 1: playEmptySequence2(onFinish); break;
            case 2: playEmptySequence3(onFinish); break;
        }
    }

    // Secuencia 1: retrocede, mueve cabeza en gesto de negación, avanza
    private void playEmptySequence1(Runnable onFinish) {
        enqueueAnimation(() -> moveBackward(15f, 0.2f, () -> playNextAnimation()));
        enqueueAnimation(() -> moveHeadNo(35f, 40f, () -> playNextAnimation()));
        enqueueAnimation(() -> moveForward(15f, 0.2f, () -> finishAnimation(onFinish)));
    }

    // Secuencia 2: gira a la derecha, mueve cabeza en negación, gira a la izquierda
    private void playEmptySequence2(Runnable onFinish) {
        enqueueAnimation(() -> turnRight(() -> playNextAnimation()));
        enqueueAnimation(() -> moveHeadNo(35f, 40f, () -> playNextAnimation()));
        enqueueAnimation(() -> turnLeft(() -> finishAnimation(onFinish)));
    }

    // Secuencia 3: solo mueve la cabeza como diciendo "no"
    private void playEmptySequence3(Runnable onFinish) {
        enqueueAnimation(() -> moveHeadNo(25f, 30f, () -> finishAnimation(onFinish)));
    }

    // Añade una animación a la cola y la reproduce si no hay ninguna en ejecución
    private void enqueueAnimation(Runnable animation) {
        animationQueue.add(animation);
        if (!isPlayingAnimation) {
            playNextAnimation();
        }
    }

    // Reproduce la siguiente animación en la cola, si existe
    private void playNextAnimation() {
        Runnable next = animationQueue.poll(); // Extrae la siguiente animación de la cola
        if (next != null) {
            isPlayingAnimation = true; // Marca que una animación está en curso
            next.run(); // Ejecuta la animación
        } else {
            Log.w("ANIM", "Cola vacía o animación no terminó, limpiando estado");
            isPlayingAnimation = false; // Libera el estado para permitir futuras animaciones
        }
    }

    // Marca el fin de una secuencia de animación y ejecuta un callback si se proporciona
    private void finishAnimation(Runnable onFinish) {
        Log.d("ANIMACIÓN", "Animación finalizada");
        if (onFinish != null) onFinish.run(); // Ejecuta el código proporcionado para después de la animación
    }

    private void centerBuddyHeadFully() {
        // Paso 1: Habilita el motor vertical (YES)
        BuddySDK.USB.enableYesMove(1, new IUsbCommadRsp.Stub() {
            @Override
            public void onSuccess(String s1) {
                // Paso 2: Si YES se habilitó correctamente, habilita el motor horizontal (NO)
                BuddySDK.USB.enableNoMove(1, new IUsbCommadRsp.Stub() {
                    @Override
                    public void onSuccess(String s2) {
                        Log.d("BUDDY_HEAD", "YES and NO motors enabled");

                        // Paso 3: Centrar cabeza verticalmente (movimiento YES hacia 0 grados)
                        BuddySDK.USB.buddySayYes(20f, 0f, new IUsbCommadRsp.Stub() {
                            @Override
                            public void onSuccess(String s3) {
                                Log.d("BUDDY_HEAD", "Vertical head centered");

                                // Paso 4: Luego, centrar cabeza horizontalmente (movimiento NO hacia 0 grados)
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

    private void rotate360(Runnable onFinish) {
        // Habilita las ruedas del robot (1,1) para permitir el movimiento
        BuddySDK.USB.enableWheels(1, 1, new IUsbCommadRsp.Stub() {
            @Override
            public void onSuccess(String s) {
                // Si las ruedas fueron habilitadas correctamente, inicia la rotación completa (360 grados)
                BuddySDK.USB.rotateBuddy(90f, 360f, new IUsbCommadRsp.Stub() {
                    @Override
                    public void onSuccess(String s) {
                        // Si se recibe la señal de que el movimiento terminó correctamente, ejecuta la acción final
                        if ("WHEEL_MOVE_FINISHED".equals(s) && onFinish != null)
                            runOnUiThread(onFinish);
                    }

                    @Override
                    public void onFailed(String error) {
                        // Si falla la rotación, se registra el error en los logs
                        Log.e("BUDDY_ROTATE", "Error en rotación 360°: " + error);
                    }
                });
            }

            @Override
            public void onFailed(String error) {
                // Si no se pueden habilitar las ruedas, se registra el error
                Log.e("BUDDY_WHEELS", "No se pudieron habilitar las ruedas: " + error);
            }
        });
    }

    private void turnLeft(Runnable onFinish) {
        // Solicita la activación de las ruedas para permitir el movimiento
        BuddySDK.USB.enableWheels(1, 1, new IUsbCommadRsp.Stub() {

            @Override
            public void onSuccess(String s) {
                // Si las ruedas se habilitan correctamente, se inicia la rotación hacia la izquierda
                // Parámetros: velocidad angular 85 grados/segundo, ángulo 90 grados
                BuddySDK.USB.rotateBuddy(85f, 90f, new IUsbCommadRsp.Stub() {

                    @Override
                    public void onSuccess(String s) {
                        // Si el movimiento se completa correctamente, y hay una acción a ejecutar, la lanza en el hilo principal
                        if ("WHEEL_MOVE_FINISHED".equals(s) && onFinish != null)
                            runOnUiThread(onFinish);
                    }

                    @Override
                    public void onFailed(String error) {
                        // Si ocurre un error durante la rotación, lo registra y ejecuta el callback de finalización igualmente
                        Log.e("BUDDY_TURN", "Error al girar izquierda: " + error);
                        if (onFinish != null) runOnUiThread(onFinish);
                    }
                });
            }

            @Override
            public void onFailed(String error) {
                // Si falla al habilitar las ruedas, también lo registra y lanza el callback si existe
                Log.e("BUDDY_WHEELS", "No se pudieron habilitar las ruedas: " + error);
                if (onFinish != null) runOnUiThread(onFinish);
            }
        });
    }

    private void turnRight(Runnable onFinish) {
        // Habilita los motores de las ruedas del robot para permitir movimiento
        BuddySDK.USB.enableWheels(1, 1, new IUsbCommadRsp.Stub() {

            @Override
            public void onSuccess(String s) {
                // Una vez habilitadas las ruedas, se ordena al robot girar 90 grados hacia la derecha
                // Se usa una velocidad de rotación de 85 grados/segundo y un ángulo negativo (-90) para indicar giro a la derecha
                BuddySDK.USB.rotateBuddy(85f, -90f, new IUsbCommadRsp.Stub() {

                    @Override
                    public void onSuccess(String s) {
                        // Si el giro se completa correctamente, y hay una acción a ejecutar, se llama en el hilo principal
                        if ("WHEEL_MOVE_FINISHED".equals(s) && onFinish != null)
                            runOnUiThread(onFinish);
                    }

                    @Override
                    public void onFailed(String error) {
                        // En caso de fallo en la rotación, registra el error y llama igualmente al callback si existe
                        Log.e("BUDDY_TURN", "Error al girar derecha: " + error);
                        if (onFinish != null) runOnUiThread(onFinish);
                    }
                });
            }

            @Override
            public void onFailed(String error) {
                // Si no se pudieron habilitar las ruedas, se registra el error y se llama al callback si existe
                Log.e("BUDDY_WHEELS", "No se pudieron habilitar las ruedas: " + error);
                if (onFinish != null) runOnUiThread(onFinish);
            }
        });
    }

    private void moveBackward(float speed, float distance, Runnable onFinish) {
        // Limita la velocidad a un rango seguro entre 0.05 y 0.7
        float validSpeed = Math.max(0.05f, Math.min(speed, 0.7f));

        // Asegura que la distancia sea negativa para moverse hacia atrás
        float reverseDistance = -Math.abs(distance);

        // Habilita las ruedas del robot antes de moverlo
        BuddySDK.USB.enableWheels(1, 1, new IUsbCommadRsp.Stub() {
            @Override
            public void onSuccess(String s) {
                // Una vez habilitadas las ruedas, ejecuta el movimiento hacia atrás
                BuddySDK.USB.moveBuddy(validSpeed, reverseDistance, new IUsbCommadRsp.Stub() {

                    @Override
                    public void onSuccess(String s) {
                        // Si el movimiento termina correctamente, ejecuta el callback en el hilo principal
                        if ("WHEEL_MOVE_FINISHED".equals(s) && onFinish != null)
                            runOnUiThread(onFinish);
                    }

                    @Override
                    public void onFailed(String error) {
                        // Si ocurre un error durante el movimiento, se registra el error
                        Log.e("BUDDY_BACK", "Error al retroceder: " + error);

                        // Aún en caso de error, se ejecuta el callback si fue proporcionado
                        if (onFinish != null) runOnUiThread(onFinish);
                    }
                });
            }

            @Override
            public void onFailed(String error) {
                // Si falla la activación de las ruedas, registra el error
                Log.e("BUDDY_WHEELS", "No se pudieron habilitar las ruedas: " + error);

                // Ejecuta el callback aunque haya fallado la activación de las ruedas
                if (onFinish != null) runOnUiThread(onFinish);
            }
        });
    }

    private void moveHeadYes(float angle, float speed, Runnable onFinish) {
        // Asegura que la velocidad y el ángulo estén dentro de un rango seguro
        float safeSpeed = Math.min(Math.max(speed, 0.1f), 49.2f); // velocidad entre 0.1 y 49.2
        float safeAngle = Math.min(Math.max(angle, 1f), 45f);     // ángulo entre 1° y 45°

        Log.e("Dentro de moveHeadYes", "angle: " + safeAngle + ", speed: " + safeSpeed);
        Log.d("BUDDY_HEAD", "Estado del motor YES antes de mover: " + BuddySDK.Actuators.getYesStatus());

        // Se asegura de que el callback final solo se ejecute una vez
        AtomicBoolean terminado = new AtomicBoolean(false);
        Runnable seguroFinish = () -> {
            if (!terminado.getAndSet(true)) {
                Log.d("ANIMACIÓN", "Animación finalizada");
                if (onFinish != null) runOnUiThread(onFinish);
            }
        };

        // Si el motor YES ya está ocupado (estado "SET"), reintenta después de 100ms
        if ("SET".equals(BuddySDK.Actuators.getYesStatus())) {
            Log.w("BUDDY_HEAD", "Motor YES ocupado, esperando para reintentar...");
            new Handler(Looper.getMainLooper()).postDelayed(() -> moveHeadYes(angle, speed, onFinish), 100);
            return;
        }

        // Paso 1: Deshabilita el motor YES
        BuddySDK.USB.enableYesMove(0, new IUsbCommadRsp.Stub() {
            @Override
            public void onSuccess(String s1) {
                // Paso 2: Lo vuelve a habilitar para reiniciar el estado
                BuddySDK.USB.enableYesMove(1, new IUsbCommadRsp.Stub() {
                    @Override
                    public void onSuccess(String s2) {
                        Log.d("BUDDY_HEAD", "Motor YES reiniciado y habilitado");

                        // Paso 3: Realiza el movimiento de inclinación de cabeza
                        BuddySDK.USB.buddySayYes(safeSpeed, safeAngle, new IUsbCommadRsp.Stub() {
                            @Override
                            public void onSuccess(String result) {
                                if ("YES_MOVE_FINISHED".equals(result)) {
                                    Log.d("BUDDY_HEAD", "Movimiento YES completado, volviendo al centro");

                                    // Paso 4: Vuelve la cabeza a la posición neutral (ángulo 0°)
                                    BuddySDK.USB.buddySayYes(safeSpeed, 0f, new IUsbCommadRsp.Stub() {
                                        @Override
                                        public void onSuccess(String s) {
                                            seguroFinish.run();
                                        }

                                        @Override
                                        public void onFailed(String error) {
                                            Log.e("BUDDY_HEAD", "Error al centrar cabeza YES: " + error);
                                            seguroFinish.run();
                                        }
                                    });
                                } else {
                                    // Si el resultado no es el esperado, aún así se finaliza
                                    seguroFinish.run();
                                }
                            }

                            @Override
                            public void onFailed(String error) {
                                Log.e("BUDDY_HEAD", "Fallo en movimiento YES: " + error);
                                seguroFinish.run();
                            }
                        });
                    }

                    @Override
                    public void onFailed(String e) {
                        Log.e("BUDDY_HEAD", "Error al re-habilitar motor YES: " + e);
                        seguroFinish.run();
                    }
                });
            }

            @Override
            public void onFailed(String e) {
                Log.e("BUDDY_HEAD", "Error al deshabilitar motor YES: " + e);
                seguroFinish.run();
            }
        });
    }

    private void moveHeadNo(float angle, float speed, Runnable onFinish) {
        // Asegura que los parámetros estén dentro de los límites seguros
        float safeSpeed = Math.max(0.1f, Math.min(speed, 140f));        // velocidad entre 0.1 y 140
        float safeAngle = Math.max(1f, Math.min(Math.abs(angle), 90f)); // ángulo entre 1° y 90°

        // Log para depuración
        Log.e("Dentro de moveHeadNo", "angle: " + safeAngle + ", speed: " + safeSpeed);
        Log.d("BUDDY_HEAD", "Estado del motor NO antes de mover: " + BuddySDK.Actuators.getNoStatus());

        // Asegura que el callback final se ejecute solo una vez
        AtomicBoolean terminado = new AtomicBoolean(false);
        Runnable seguroFinish = () -> {
            if (!terminado.getAndSet(true)) {
                Log.d("ANIMACIÓN", "Animación finalizada");
                if (onFinish != null) runOnUiThread(onFinish);
            }
        };

        // Si el motor está ocupado, espera 100ms y vuelve a intentar
        if ("SET".equals(BuddySDK.Actuators.getNoStatus())) {
            Log.w("BUDDY_HEAD", "Motor NO ocupado, esperando para reintentar...");
            new Handler(Looper.getMainLooper()).postDelayed(() -> moveHeadNo(angle, speed, onFinish), 100);
            return;
        }

        // Paso 1: Deshabilita temporalmente el motor NO para reiniciarlo
        BuddySDK.USB.enableNoMove(0, new IUsbCommadRsp.Stub() {
            @Override
            public void onSuccess(String s1) {
                // Paso 2: Habilita el motor nuevamente
                BuddySDK.USB.enableNoMove(1, new IUsbCommadRsp.Stub() {
                    @Override
                    public void onSuccess(String s2) {
                        Log.d("BUDDY_HEAD", "Motor NO reiniciado y habilitado");

                        // Paso 3: Ejecuta el movimiento de negación (giro de cabeza)
                        BuddySDK.USB.buddySayNo(safeSpeed, safeAngle, new IUsbCommadRsp.Stub() {
                            @Override
                            public void onSuccess(String result) {
                                if ("NO_MOVE_FINISHED".equals(result)) {
                                    Log.d("BUDDY_HEAD", "Movimiento NO completado, volviendo al centro");

                                    // Paso 4: Devuelve la cabeza a la posición central (ángulo 0°)
                                    BuddySDK.USB.buddySayNo(safeSpeed, 0f, new IUsbCommadRsp.Stub() {
                                        @Override
                                        public void onSuccess(String s) {
                                            seguroFinish.run();
                                        }

                                        @Override
                                        public void onFailed(String error) {
                                            Log.e("BUDDY_HEAD", "Error al centrar cabeza NO: " + error);
                                            seguroFinish.run();
                                        }
                                    });
                                } else {
                                    // Finaliza aunque no se reciba confirmación esperada
                                    seguroFinish.run();
                                }
                            }

                            @Override
                            public void onFailed(String error) {
                                Log.e("BUDDY_HEAD", "Fallo en movimiento NO: " + error);
                                seguroFinish.run();
                            }
                        });
                    }

                    @Override
                    public void onFailed(String e) {
                        Log.e("BUDDY_HEAD", "Error al re-habilitar motor NO: " + e);
                        seguroFinish.run();
                    }
                });
            }

            @Override
            public void onFailed(String e) {
                Log.e("BUDDY_HEAD", "Error al deshabilitar motor NO: " + e);
                seguroFinish.run();
            }
        });
    }

    private void moveForward(float speed, float distance, Runnable onFinish) {
        // Habilita las ruedas antes de mover
        BuddySDK.USB.enableWheels(1, 1, new IUsbCommadRsp.Stub() {
            @Override
            public void onSuccess(String s) {
                // Si las ruedas se habilitan correctamente, inicia el movimiento hacia adelante
                BuddySDK.USB.moveBuddy(speed, distance, new IUsbCommadRsp.Stub() {
                    @Override
                    public void onSuccess(String s) {
                        // Verifica si el movimiento ha finalizado correctamente
                        if ("WHEEL_MOVE_FINISHED".equals(s) && onFinish != null)
                            runOnUiThread(onFinish); // Ejecuta el callback en el hilo principal
                    }

                    @Override
                    public void onFailed(String error) {
                        // Loguea el error si el movimiento falla
                        Log.e("BUDDY_WHEELS", "Error al mover adelante: " + error);
                        if (onFinish != null) runOnUiThread(onFinish); // Aún así llama al callback para no quedar colgado
                    }
                });
            }

            @Override
            public void onFailed(String error) {
                // Si no se pueden habilitar las ruedas, se registra el error
                Log.e("BUDDY_WHEELS", "No se pudieron habilitar las ruedas: " + error);
                if (onFinish != null) runOnUiThread(onFinish); // Asegura que el flujo continúe
            }
        });
    }
}
