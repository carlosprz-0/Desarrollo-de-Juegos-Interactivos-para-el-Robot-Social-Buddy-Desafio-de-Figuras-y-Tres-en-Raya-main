// Paquete principal de la app
package com.bfr.helloworld;

// Importaciones necesarias para servidor, contexto Android, logs, JSON y manejo de archivos
import fi.iki.elonen.NanoHTTPD;
import android.content.Context;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

// Clase abstracta que extiende NanoHTTPD para crear un servidor local que permita subir puzzles vía web
public abstract class WebPuzzleUploader extends NanoHTTPD {

    private final Context context;
    private final File puzzlesRootDir;
    private File activeGameDir; // Directorio del juego activo actualmente

    // Constructor del servidor. Toma el contexto y el nombre del juego activo
    public WebPuzzleUploader(Context ctx, String selectedGame) throws IOException {
        super(8085); // Puerto del servidor
        this.context = ctx;

        this.puzzlesRootDir = new File(context.getFilesDir(), "puzzles");
        if (!puzzlesRootDir.exists()) puzzlesRootDir.mkdirs();

        File selected = new File(puzzlesRootDir, selectedGame);
        if (!selected.exists()) selected.mkdirs();
        activeGameDir = selected;
    }

    public File getActiveGameDir() {
        return activeGameDir;
    }

    // Inicia el servidor HTTP
    public void startServer() {
        try {
            start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            Log.i("WebUploader", "✅ Server started on http://localhost:8085");
        } catch (IOException e) {
            Log.e("WebUploader", "❌ Error starting server", e);
        }
    }

    // Método principal que maneja todas las rutas HTTP entrantes
    @Override
    public Response serve(IHTTPSession session) {
        Map<String, String> files = new LinkedHashMap<>(); // Almacena los archivos subidos temporalmente
        String uri = session.getUri(); // Obtiene la URI de la petición

        try {
            // Si la petición es POST, parsea el cuerpo para obtener archivos
            if (Method.POST.equals(session.getMethod())) session.parseBody(files);

            // Obtiene los parámetros enviados en la petición
            Map<String, String> params = session.getParms();

            // Maneja la ruta de subida de nuevos puzzles en lote
            if (Method.POST.equals(session.getMethod()) && uri.startsWith("/upload")) {
                JSONArray array = loadJson(); // Carga puzzles existentes
                int count = Integer.parseInt(params.get("count")); // Cantidad de puzzles nuevos

                for (int i = 0; i < count; i++) {
                    String difficulty = params.get("difficulty_" + i);
                    String prompt = params.get("prompt_" + i);
                    int correctIndex = Integer.parseInt(params.get("correct_" + i));

                    List<String> keys = new ArrayList<>(files.keySet());

                    // Verifica que se hayan recibido todos los archivos esperados
                    if (keys.size() < 4 * count) {
                        return newFixedLengthResponse("Error: se esperaban 4 archivos por pregunta, y se recibieron menos.");
                    }

                    // Asocia cada archivo subido a su rol correspondiente
                    String mainPath = files.get(keys.get(i * 4));
                    String opt0 = files.get(keys.get(i * 4 + 1));
                    String opt1 = files.get(keys.get(i * 4 + 2));
                    String opt2 = files.get(keys.get(i * 4 + 3));

                    if (mainPath == null || opt0 == null || opt1 == null || opt2 == null) {
                        return newFixedLengthResponse("Error: faltan archivos.");
                    }

                    // Genera un ID único para el puzzle
                    String base = "puzzle_" + UUID.randomUUID().toString().replace("-", "");

                    // Copia la imagen principal al directorio del juego
                    File puzzleFile = new File(getActiveGameDir(), base + ".png");
                    copy(new File(mainPath), puzzleFile);

                    // Copia las imágenes de opciones y guarda sus nombres
                    List<String> optionNames = new ArrayList<>();
                    String[] optPaths = {opt0, opt1, opt2};
                    for (int j = 0; j < 3; j++) {
                        String name = base + "_op" + j;
                        File optTarget = new File(getActiveGameDir(), name + ".png");
                        copy(new File(optPaths[j]), optTarget);
                        optionNames.add(name);
                    }

                    // Crea el objeto JSON para este puzzle y lo añade al array
                    JSONObject obj = new JSONObject();
                    obj.put("puzzleImage", base);
                    obj.put("options", new JSONArray(optionNames));
                    obj.put("correctIndex", correctIndex);
                    obj.put("prompt", prompt);
                    obj.put("difficulty", difficulty);
                    array.put(obj);
                }

                // Guarda el JSON actualizado y redirige a /edit
                saveJson(array);
                return redirect("/edit");
            }

            // Maneja la actualización de una pregunta existente
            if (uri.startsWith("/updateQuestion/")) {
                int index = Integer.parseInt(uri.substring(uri.lastIndexOf("/") + 1));
                try {
                    JSONArray array = loadJson();
                    JSONObject obj = array.getJSONObject(index);

                    // Actualiza campos básicos
                    obj.put("prompt", params.get("prompt"));
                    obj.put("difficulty", params.get("difficulty"));
                    obj.put("correctIndex", Integer.parseInt(params.get("correctIndex")));

                    // Actualiza imagen principal si hay una nueva
                    String mainImage = obj.getString("puzzleImage") + ".png";
                    String newMainPath = files.get("newMain");
                    if (newMainPath != null) {
                        File newMain = new File(newMainPath);
                        if (newMain.exists()) {
                            copy(newMain, new File(getActiveGameDir(), mainImage));
                            Log.d("UPDATE", "Imagen principal actualizada: " + mainImage);
                        } else {
                            Log.w("UPDATE", "Archivo newMain no existe físicamente.");
                        }
                    }

                    // Actualiza imágenes de opciones si hay nuevas
                    JSONArray opts = obj.getJSONArray("options");
                    for (int i = 0; i < opts.length(); i++) {
                        String optKey = "newOpt" + i;
                        String optPath = files.get(optKey);
                        if (optPath != null) {
                            File optFile = new File(optPath);
                            if (optFile.exists()) {
                                File target = new File(getActiveGameDir(), opts.getString(i) + ".png");
                                copy(optFile, target);
                                Log.d("UPDATE", "Opción actualizada: " + target.getName());
                            } else {
                                Log.w("UPDATE", "Archivo " + optKey + " no existe físicamente.");
                            }
                        }
                    }

                    saveJson(array);
                    return redirect("/edit");
                } catch (Exception e) {
                    return newFixedLengthResponse("Error actualizando: " + e.getMessage());
                }
            }

            // Maneja la eliminación de una pregunta por índice
            if (Method.POST.equals(session.getMethod()) && uri.startsWith("/deleteQuestion/")) {
                Log.i("DELETE", "🟢 Entrando al bloque deleteQuestion con URI: " + uri);
                int index = Integer.parseInt(uri.substring("/deleteQuestion/".length()));
                try {
                    JSONArray array = loadJson();
                    if (index < 0 || index >= array.length()) {
                        return newFixedLengthResponse("Error: índice inválido.");
                    }

                    JSONObject obj = array.getJSONObject(index);

                    // Elimina archivos asociados a la pregunta
                    File mainImg = new File(getActiveGameDir(), obj.getString("puzzleImage") + ".png");
                    if (mainImg.exists()) mainImg.delete();

                    JSONArray options = obj.getJSONArray("options");
                    for (int i = 0; i < options.length(); i++) {
                        File opt = new File(getActiveGameDir(), options.getString(i) + ".png");
                        if (opt.exists()) opt.delete();
                    }

                    // Remueve la entrada del JSON
                    JSONArray newArray = new JSONArray();
                    for (int i = 0; i < array.length(); i++) {
                        if (i != index) {
                            newArray.put(array.getJSONObject(i));
                        }
                    }
                    saveJson(newArray);

                    return redirect("/edit");
                } catch (Exception e) {
                    return newFixedLengthResponse("Error eliminando: " + e.getMessage());
                }
            }

            // Sirve imágenes desde /puzzles/...
            if (uri.startsWith("/puzzles/")) {
                File requested = new File(getActiveGameDir(), uri.substring("/puzzles/".length()));
                if (requested.exists()) {
                    try {
                        FileInputStream fis = new FileInputStream(requested);
                        return newChunkedResponse(Response.Status.OK, "image/png", fis);
                    } catch (IOException e) {
                        return newFixedLengthResponse("Error al leer la imagen.");
                    }
                } else {
                    return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Imagen no encontrada: " + uri);
                }
            }

            // Maneja la eliminación completa de un juego
            if (uri.startsWith("/deleteGame/")) {
                String gameName = uri.substring("/deleteGame/".length());
                File gameDir = new File(puzzlesRootDir, gameName);
                if (!gameDir.exists()) {
                    return newFixedLengthResponse("Error: el juego no existe.");
                }

                for (File f : Objects.requireNonNull(gameDir.listFiles())) {
                    f.delete();
                }
                gameDir.delete();

                // Cambia el juego activo si es necesario
                if (activeGameDir.getName().equals(gameName)) {
                    File[] remaining = puzzlesRootDir.listFiles(File::isDirectory);
                    if (remaining != null && remaining.length > 0) {
                        activeGameDir = remaining[0];
                    }
                }

                return redirect("/selectGame");
            }

            // Crear un nuevo juego
            if (uri.equals("/createGame") && session.getMethod().equals(Method.POST)) {
                Map<String, String> parms = session.getParms();
                String newGameName = parms.get("newGameName");

                if (newGameName == null || newGameName.trim().isEmpty()) {
                    return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT,
                            "Error: El nombre del juego no puede estar vacío.");
                }

                File newGameDir = new File(puzzlesRootDir, newGameName);

                if (newGameDir.exists()) {
                    return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT,
                            "Error: Ya existe un juego con ese nombre.");
                }

                if (newGameDir.mkdirs()) {
                    activeGameDir = newGameDir;
                    return redirect("/selectGame");
                } else {
                    return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT,
                            "Error al crear la carpeta del juego.");
                }
            }

            if (uri.equals("/renameGame") && Method.POST.equals(session.getMethod())) {
                Map<String, String> parms = session.getParms();
                String oldName = parms.get("oldGame");
                String newName = parms.get("newGame");

                if (oldName == null || newName == null || oldName.equals(newName)) {
                    return newFixedLengthResponse("Error: nombre inválido.");
                }

                File oldDir = new File(puzzlesRootDir, oldName);
                File newDir = new File(puzzlesRootDir, newName);

                if (!oldDir.exists()) {
                    return newFixedLengthResponse("Error: juego original no encontrado.");
                }

                if (newDir.exists()) {
                    return newFixedLengthResponse("Error: ya existe un juego con ese nombre.");
                }

                // Renombrar la carpeta
                boolean success = oldDir.renameTo(newDir);
                if (success) {
                    if (activeGameDir.getName().equals(oldName)) {
                        activeGameDir = newDir; // Actualizar el juego activo si era el renombrado
                    }
                    return redirect("/selectGame");
                } else {
                    return newFixedLengthResponse("Error al renombrar el juego.");
                }
            }

            // Maneja navegación básica a las páginas del panel web
            if (uri.equals("/")) return newFixedLengthResponse(getMainMenu());
            if (uri.equals("/edit")) return newFixedLengthResponse(getEditHtml(params.get("filter")));
            if (uri.equals("/upload")) return newFixedLengthResponse(getUploadFormHtml());
            if (uri.equals("/selectGame")) return getGameSelector();
            if (uri.equals("/stats")) return newFixedLengthResponse(getStatsHtml());

            // Descargar archivos de estadísticas
            if (uri.startsWith("/statsDownload/")) {
                String filename = uri.substring("/statsDownload/".length());
                File statsFile = new File(context.getFilesDir(), filename);
                if (statsFile.exists()) {
                    return newChunkedResponse(Response.Status.OK, "application/json", new FileInputStream(statsFile));
                } else {
                    return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "No encontrado.");
                }
            }

            // Descargar juego como ZIP
            if (uri.startsWith("/zipDownload/")) {
                String gameName = uri.substring("/zipDownload/".length());
                File dir = new File(puzzlesRootDir, gameName);
                if (!dir.exists() || !dir.isDirectory()) {
                    return newFixedLengthResponse("Error: ese juego no existe.");
                }

                try {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    ZipOutputStream zos = new ZipOutputStream(bos);

                    File[] gameFiles = dir.listFiles();
                    if (gameFiles == null || gameFiles.length == 0) {
                        return newFixedLengthResponse("Este juego no tiene archivos para exportar.");
                    }

                    for (File f : gameFiles) {
                        if (f.isFile()) {
                            zos.putNextEntry(new ZipEntry(f.getName()));
                            Files.copy(f.toPath(), zos);
                            zos.closeEntry();
                        }
                    }

                    zos.finish();
                    return newFixedLengthResponse(Response.Status.OK, "application/zip", new ByteArrayInputStream(bos.toByteArray()), bos.size());

                } catch (Exception e) {
                    return newFixedLengthResponse("Error al crear ZIP: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Error: " + e.getMessage());
        }
        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Ruta no encontrada: " + uri);
    }


    // Carga el archivo puzzles_detailed.json desde el directorio del juego activo
    private JSONArray loadJson() throws Exception {
        File file = new File(getActiveGameDir(), "puzzles_detailed.json"); // Ruta al archivo JSON
        if (!file.exists()) return new JSONArray(); // Si no existe, devuelve un array vacío

        // Lectura del contenido del archivo línea por línea
        BufferedReader reader = new BufferedReader(new FileReader(file));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);

        // Convierte el texto leido en un JSONArray y lo devuelve
        return new JSONArray(sb.toString());
    }

    // Crea una redirección HTTP hacia una ruta específica
    private Response redirect(String path) {
        Response r = newFixedLengthResponse(Response.Status.REDIRECT, "text/html", "Redirigiendo...");
        r.addHeader("Location", path);
        return r;
    }

    // Copia un archivo binario desde origen a destino
    private void copy(File src, File dst) throws IOException {
        try (InputStream in = new FileInputStream(src); OutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[4096];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
        }
    }

    // Guarda el JSON de puzzles en disco
    private void saveJson(JSONArray array) throws IOException, JSONException {
        File target = new File(getActiveGameDir(), "puzzles_detailed.json");
        Log.i("SAVE_JSON", "✍️ Guardando en: " + target.getAbsolutePath());
        FileWriter writer = new FileWriter(target);
        writer.write(array.toString(2));
        writer.close();
    }


    // Genera una página HTML que lista las estadísticas de los jugadores
    private String getStatsHtml() {
        // Busca archivos que empiecen con "estadisticas_" y terminen con ".json" en el directorio interno
        File[] statFiles = context.getFilesDir().listFiles((dir, name) -> name.startsWith("estadisticas_") && name.endsWith(".json"));

        // Comienza la estructura HTML con TailwindCSS para estilos
        StringBuilder sb = new StringBuilder("<html><head><title>Estadísticas</title><script src='https://cdn.tailwindcss.com'></script></head><body class='p-6 bg-gray-100'>");
        sb.append("<h2 class='text-2xl font-bold mb-4'>Estadísticas de Jugadores</h2>");

        // Si no hay archivos, se muestra un mensaje indicándolo
        if (statFiles == null || statFiles.length == 0) {
            sb.append("<p>No hay estadísticas disponibles.</p>");
        } else {
            // Si hay estadísticas, se listan con enlaces de descarga
            sb.append("<ul class='space-y-2'>");
            for (File f : statFiles) {
                sb.append("<li class='bg-white p-4 rounded shadow'><span>")
                        .append(f.getName())
                        .append("</span> - <a class='text-blue-600' href='/statsDownload/")
                        .append(f.getName())
                        .append("'>Descargar</a></li>");
            }
            sb.append("</ul>");
        }

        // Enlace para volver al menú principal
        sb.append("<div class='mt-6'><a href='/' class='text-blue-500'>&larr; Volver al menú</a></div></body></html>");
        return sb.toString();
    }


    // Genera la página HTML para seleccionar, renombrar o eliminar juegos existentes
    private Response getGameSelector() throws UnsupportedEncodingException {
        // Comienza el HTML con estilos Tailwind
        StringBuilder sb = new StringBuilder("<html><head><title>Juegos</title><script src='https://cdn.tailwindcss.com'></script></head><body class='p-6 bg-gray-100'>");
        sb.append("<h2 class='text-2xl font-bold mb-4'>Juegos Disponibles</h2>");

        // Lista todos los directorios que representan juegos
        File[] dirs = puzzlesRootDir.listFiles(File::isDirectory);
        if (dirs != null) {
            for (File dir : dirs) {
                boolean activo = dir.getName().equals(getActiveGameDir().getName()); // Verifica si es el juego activo

                // Muestra nombre del juego con opciones para descargar o eliminar
                sb.append("<div class='bg-white p-4 rounded shadow mb-2 flex justify-between items-center'>")
                        .append("<span>").append(dir.getName()).append(activo ? " (activo)" : "").append("</span>")
                        .append("<div>")
                        .append("<a class='text-green-600 mr-4' href='/zipDownload/")
                        .append(URLEncoder.encode(dir.getName(), "UTF-8"))
                        .append("'>Descargar ZIP</a>")
                        .append("<a class='text-red-600' href='/deleteGame/")
                        .append(URLEncoder.encode(dir.getName(), "UTF-8")).append("' onclick='return confirm(\"¿Eliminar este juego?\")'>Eliminar</a>")
                        .append("</div></div>");
            }
        }

        // Formulario para crear un nuevo juego
        sb.append("<form method='POST' enctype='multipart/form-data' class='mt-6 bg-white p-4 rounded shadow' action='/createGame'>")
                .append("<label class='block mb-1' for='newGameName'>Nombre del nuevo juego:</label>")
                .append("<input type='text' id='newGameName' name='newGameName' class='border px-2 py-1 rounded w-full mb-2' required />")
                .append("<button type='submit' class='bg-green-600 text-white px-4 py-2 rounded'>Crear carpeta</button>")
                .append("</form>");

        // Formulario para renombrar el juego activo
        sb.append("<form method='POST' class='mt-6 bg-white p-4 rounded shadow' action='/renameGame'>")
                .append("<label class='block mb-1'>Renombrar juego activo:</label>")
                .append("<input type='hidden' name='oldGame' value='").append(getActiveGameDir().getName()).append("' />")
                .append("<input name='newGame' class='border px-2 py-1 rounded w-full mb-2' required />")
                .append("<button type='submit' class='bg-yellow-600 text-white px-4 py-2 rounded'>Renombrar</button>")
                .append("</form>");

        // Enlace para volver al menú principal
        sb.append("<div class='mt-6'><a href='/' class='text-blue-500'>&larr; Volver al menú</a></div></body></html>");
        return newFixedLengthResponse(Response.Status.OK, "text/html", sb.toString());
    }

    // Genera una página HTML para editar los puzzles existentes con opción de filtrar por dificultad
    private String getEditHtml(String filter) throws Exception {
        JSONArray array = loadJson(); // Carga los puzzles desde el JSON

        // Comienza la estructura HTML con estilos y encabezado
        StringBuilder sb = new StringBuilder("<html><head><meta charset='UTF-8'><script src='https://cdn.tailwindcss.com'></script><title>Editar</title></head><body class='p-6 bg-gray-100'>");
        sb.append("<h2 class='text-2xl font-bold mb-4'>Editando: ").append(getActiveGameDir().getName()).append("</h2>");

        // Filtro por dificultad (fácil, normal, difícil)
        sb.append("<form method='GET' class='mb-4'>")
                .append("Filtrar por dificultad: ")
                .append("<select name='filter'><option value=''>Todas</option><option value='facil'>Fácil</option><option value='normal'>Normal</option><option value='dificil'>Difícil</option></select>")
                .append("<button class='ml-2 bg-blue-500 text-white px-3 py-1 rounded' type='submit'>Filtrar</button>")
                .append("</form>");

        boolean anyVisible = false; // Verifica si se mostrará al menos una pregunta

        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);
            if (filter != null && !filter.isEmpty() && !filter.equals(obj.optString("difficulty"))) continue;
            JSONArray opts = obj.getJSONArray("options");

            anyVisible = true;

            // Formulario para editar una pregunta
            sb.append("<form method='POST' enctype='multipart/form-data' action='/updateQuestion/").append(i).append("' class='bg-white p-4 rounded shadow mb-4'>")
                    .append("<p><strong>Imagen:</strong><br><img src='/puzzles/").append(obj.getString("puzzleImage")).append(".png' style='max-width:150px;'/></p>")
                    .append("<label class='block mb-2'>Reemplazar imagen principal:")
                    .append("<input type='file' name='newMain' accept='image/*' class='block mt-1' /></label><br>");

            // Campos para editar cada una de las opciones de respuesta
            for (int j = 0; j < opts.length(); j++) {
                sb.append("<p>Opción ").append(j).append(": <img src='/puzzles/")
                        .append(opts.getString(j)).append(".png' style='max-width:100px;' /></p>");
                sb.append("<label class='block mb-2'>Reemplazar opción ").append(j).append(":")
                        .append("<input type='file' name='newOpt").append(j).append("' accept='image/*' class='block mt-1' /></label><br>");
            }

            // Campos de edición de texto: descripción, dificultad y respuesta correcta
            sb.append("<label>Descripción: <input class='border w-full mb-2' name='prompt' value='").append(obj.getString("prompt")).append("' /></label><br>")
                    .append("<label>Dificultad: <select name='difficulty'>")
                    .append("<option value='facil'").append(obj.getString("difficulty").equals("facil") ? " selected" : "").append(">Fácil</option>")
                    .append("<option value='normal'").append(obj.getString("difficulty").equals("normal") ? " selected" : "").append(">Normal</option>")
                    .append("<option value='dificil'").append(obj.getString("difficulty").equals("dificil") ? " selected" : "").append(">Difícil</option>")
                    .append("</select></label><br><br>")
                    .append("<label>Respuesta correcta: <select name='correctIndex'>");

            // Selector de respuesta correcta
            for (int j = 0; j < opts.length(); j++) {
                sb.append("<option value='").append(j).append("'")
                        .append(obj.getInt("correctIndex") == j ? " selected" : "").append(">Opción ").append(j).append("</option>");
            }

            // Botón para guardar cambios
            sb.append("</select></label><br><br>")
                    .append("<button class='bg-blue-600 text-white px-4 py-2 rounded' type='submit'>Guardar</button>")
                    .append("</form>")
                    // Botón para eliminar pregunta
                    .append("<form method='POST' action='/deleteQuestion/").append(i).append("' ")
                    .append("onsubmit=\"return confirm('¿Eliminar esta pregunta?')\" style='display:inline;'>")
                    .append("<button type='submit' class='ml-4 text-red-600 hover:underline'>Eliminar</button>")
                    .append("</form>");
        }

        // Mensaje si no hay preguntas para mostrar
        if (!anyVisible) {
            sb.append("<p class='text-gray-600'>No hay preguntas disponibles para editar.</p>");
        }

        // Enlace para volver al menú principal
        sb.append("<div class='mt-6'><a href='/' class='text-blue-500'>&larr; Volver al menú principal</a></div></body></html>");
        return sb.toString();
    }

    // Genera la interfaz HTML para subir nuevas preguntas de manera dinámica
    private String getUploadFormHtml() {
        return "<html><head><meta charset='UTF-8'>" +
                "<script src='https://cdn.tailwindcss.com'></script>" +
                "<title>Subir (orden aleatorio)</title></head>" +
                "<body class='p-6 bg-gray-100'>" +
                "<h2 class='text-2xl font-bold mb-4'>Subir preguntas al juego: " + getActiveGameDir().getName() + "</h2>" +

                // Formulario principal con soporte para múltiples archivos
                "<form method='POST' enctype='multipart/form-data' action='/upload?nocache=" + System.currentTimeMillis() + "'>" +
                "<input type='hidden' name='count' id='countField' value='1' />" +
                "<div id='questionContainer'></div>" +
                "<button type='button' onclick='addQuestion()' class='bg-green-600 text-white px-4 py-2 rounded'>Añadir otra</button><br><br>" +
                "<button type='submit' class='bg-blue-600 text-white px-6 py-2 rounded'>Subir</button>" +
                "</form>" +

                // Script para añadir dinámicamente nuevas preguntas al formulario
                "<script>" +
                "let questionIndex = 0; addQuestion();" +
                "function addQuestion() {" +
                "  const q = document.createElement('div');" +
                "  q.className = 'mb-4 bg-white p-4 rounded shadow';" +
                "  const i = questionIndex;" +
                "  q.innerHTML = " +
                "    '<label>Imagen principal:<input type=\"file\" name=\"file\" required></label><br>' +" +
                "    '<label>Opción 1:<input type=\"file\" name=\"file\" required></label><br>' +" +
                "    '<label>Opción 2:<input type=\"file\" name=\"file\" required></label><br>' +" +
                "    '<label>Opción 3:<input type=\"file\" name=\"file\" required></label><br>' +" +
                "    '<label>Respuesta correcta:<select name=\"correct_' + i + '\">' +" +
                "    '<option value=\"0\">0</option><option value=\"1\">1</option><option value=\"2\">2</option></select></label><br>' +" +
                "    '<label>Descripción:<input name=\"prompt_' + i + '\" required></label><br>' +" +
                "    '<label>Dificultad:<select name=\"difficulty_' + i + '\">' +" +
                "    '<option value=\"facil\">Fácil</option><option value=\"normal\">Normal</option><option value=\"dificil\">Difícil</option></select></label><br>';" +
                "  document.getElementById('questionContainer').appendChild(q);" +
                "  questionIndex++;" +
                "  document.getElementById('countField').value = questionIndex;" +
                "}" +
                "</script>" +

                // Enlace de retorno al menú principal
                "<div class='mt-6'><a href='/' class='text-blue-500'>&larr; Volver al menú</a></div>" +
                "</body></html>";
    }

    // Genera el menú principal de administración
    private String getMainMenu() {
        return "<html><head><title>Panel Principal</title><script src='https://cdn.tailwindcss.com'></script></head>" +
                "<body class='p-6 bg-gray-100'><h1 class='text-2xl font-bold mb-4'>Panel de Administración</h1>" +
                "<ul class='space-y-2'><li><a href='/selectGame' class='text-blue-600'>Ver Juegos</a></li>" +
                "<li><a href='/stats' class='text-blue-600'>Ver Estadísticas</a></li>" +
                "<li><a href='/edit' class='text-blue-600'>Editar Preguntas</a></li>" +
                "<li><a href='/upload' class='text-blue-600'>Subir Preguntas</a></li></ul></body></html>";
    }

    // Método abstracto a implementar para cuando se cierre el servidor
    public abstract void onServerClosed();
}


