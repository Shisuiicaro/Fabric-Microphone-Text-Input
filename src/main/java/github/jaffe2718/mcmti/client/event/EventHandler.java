package github.jaffe2718.mcmti.client.event;

import github.jaffe2718.mcmti.MicrophoneTextInputMain;
import github.jaffe2718.mcmti.client.MicrophoneTextInputClient;
import github.jaffe2718.mcmti.config.ConfigUI;
import github.jaffe2718.mcmti.unit.MicrophoneHandler;
import github.jaffe2718.mcmti.unit.SpeechRecognizer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.text.Text;
import org.vosk.Model;

import javax.sound.sampled.AudioFormat;
import java.nio.charset.StandardCharsets;

/**
 * This class is used to register response processing for game events.
 * This includes the function of initializing the speech recognizer when the game starts and the task of detecting that the user presses the key V to initiate speech recognition and send a message.
 * @author Jaffe2718*/
public class EventHandler {

    /** The following variables are used to store the speech recognizer*/
    private static MicrophoneHandler microphoneHandler;

    /** The following variables are used to store the microphone handler*/
    private static SpeechRecognizer speechRecognizer;

    /** The following variables are used to store the last recognized result*/
    private static String lastResult = "";

    /** The following variables are used to store the thread that listens to the microphone*/
    private static Thread listenThread;

    /** Variable to control voice recognition toggle */
    private static boolean voiceRecognitionEnabled = true; // Enabled by default

        private static boolean keyWasPressed = false;



    /** This method is used to register the response processing for the game start event*/
    public static void register() {

        ClientLifecycleEvents.CLIENT_STARTED.register(EventHandler::handelClientStartEvent);

        ClientTickEvents.END_CLIENT_TICK.register(EventHandler::handleEndClientTickEvent);

        ClientTickEvents.START_CLIENT_TICK.register(EventHandler::handleStartClientTickEvent);

        ClientLifecycleEvents.CLIENT_STOPPING.register(EventHandler::handleClientStopEvent);


        // Register keybind to toggle voice recognition (using "M" key)
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                if (MicrophoneTextInputClient.vKeyBinding.isPressed()) {
                    if (!keyWasPressed) { // Verifica se a tecla foi pressionada neste tick
                        keyWasPressed = true;
                    }
                } else {
                    if (keyWasPressed) { // Verifica se a tecla estava sendo pressionada e foi solta neste tick
                        keyWasPressed = false;
                        voiceRecognitionEnabled = !voiceRecognitionEnabled; // Toggle voice recognition
                        client.player.sendMessage(Text.of("Voice recognition " + (voiceRecognitionEnabled ? "enabled" : "disabled")), false);
                    }
                }
            }
        });
    }

    private static void listenThreadTask() {
        while (true) {
            try {
                if (voiceRecognitionEnabled) { // Only listen when voice recognition is enabled
                    if (speechRecognizer == null) {         // wait 10 seconds and try to initialize the speech recognizer again
                        if (MinecraftClient.getInstance().player != null) {
                            MinecraftClient.getInstance().player.sendMessage(Text.of("§cAcoustic Model Load Failed"), false);
                        }
                        // listenThread.wait(10000);
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException ie) {
                            continue;
                        }
                        speechRecognizer = new SpeechRecognizer(new Model(ConfigUI.acousticModelPath), ConfigUI.sampleRate);
                    } else if (microphoneHandler == null) {  // wait 10 seconds and try to initialize the microphone handler again
                        listenThread.wait(10000);
                        microphoneHandler = new MicrophoneHandler(new AudioFormat(ConfigUI.sampleRate, 16, 1, true, false));
                        microphoneHandler.startListening();  // Try to restart microphone
                    } else {                                 // If the speech recognizer and the microphone handler are initialized successfully
                        String tmp = speechRecognizer.getStringMsg(microphoneHandler.readData());
                        if (!tmp.equals("") && !tmp.equals(lastResult)) {   // Read audio data from the microphone and send it to the speech recognizer for recognition
                            if (ConfigUI.encoding_repair) {
                                lastResult = SpeechRecognizer.repairEncoding(tmp, ConfigUI.srcEncoding, ConfigUI.dstEncoding);
                            } else {                                        // default configuration without encoding repair
                                lastResult = tmp;                           // restore the recognized text
                            }
                        }
                    }
                } else {
                    Thread.sleep(100); // Sleep for a short while to reduce CPU usage when voice recognition is disabled
                }
            } catch (Exception e) {
                MicrophoneTextInputMain.LOGGER.error(e.getMessage());
            }
        }
    }

    private static void handelClientStartEvent(MinecraftClient client) {     // when the client launch
        MicrophoneTextInputMain.LOGGER.info("Loading acoustic model from " + ConfigUI.acousticModelPath + "   ..."); // Log the path of the acoustic model
        try {                                  // Initialize the speech recognizer
            speechRecognizer = new SpeechRecognizer(new Model(ConfigUI.acousticModelPath), ConfigUI.sampleRate);
            MicrophoneTextInputMain.LOGGER.info("Acoustic model loaded successfully!");
        }catch (Exception e1) {
            MicrophoneTextInputMain.LOGGER.error(e1.getMessage());
        }
        try {                                   // Initialize the microphone handler, single channel, 16 bits per sample, signed, little endian
            microphoneHandler = new MicrophoneHandler(new AudioFormat(ConfigUI.sampleRate, 16, 1, true, false));
            microphoneHandler.startListening();
            MicrophoneTextInputMain.LOGGER.info("Microphone handler initialized successfully!");
        } catch (Exception e2) {
            MicrophoneTextInputMain.LOGGER.error(e2.getMessage());
        }
        if (ConfigUI.encoding_repair) {         // If the encoding repair function is enabled, log a warning
            MicrophoneTextInputMain.LOGGER.warn(
                    String.format("(test function) Trt to resolve error encoding from %s to %s...", ConfigUI.srcEncoding, ConfigUI.dstEncoding));
        }
        listenThread = new Thread(EventHandler::listenThreadTask);
        listenThread.start();
    }

    private static void handleClientStopEvent(MinecraftClient client) {
        listenThread.interrupt();                 // Stop the thread that listens to the microphone
        microphoneHandler.stopListening();        // Stop listening to the microphone
        speechRecognizer = null;
        microphoneHandler = null;
        listenThread = null;                      // Clear the thread
    }
    private static String processSpecialCharacters(String text) {
        // Substitua caracteres especiais conforme necessário
        String processedText = text.replace("ê", "e").replace("Ã", "ã");
        return processedText;
    }

    private static void handleEndClientTickEvent(MinecraftClient client) {
        if (client.player != null && microphoneHandler != null && !lastResult.equals("")) {
            // Processar texto reconhecido para tratar caracteres especiais
            String processedText = processSpecialCharacters(lastResult);

            // Converter para UTF-8 para garantir a correta interpretação de caracteres
            processedText = new String(processedText.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

            // Enviar o texto processado para o chat
            if (ConfigUI.autoSend) {
                client.player.networkHandler.sendChatMessage(ConfigUI.prefix + " " + processedText);
                client.player.sendMessage(Text.of("§aMessage Sent"), false);
            } else {
                client.setScreen(new ChatScreen(ConfigUI.prefix + " " + processedText));
                // Aumente o tempo que a mensagem fica na tela
                for (int i = 0; i < 100; i++) {
                    if (client.currentScreen != null) client.currentScreen.applyKeyPressNarratorDelay();
                }
            }
            lastResult = ""; // Limpar o texto reconhecido
        }
    }
    private static void handleStartClientTickEvent(MinecraftClient client) {  // handle another client tick event to notify the user that the speech recognition is in progress and the game is not frozen
        if (client.player!=null) {
            client.player.sendMessage(Text.of(""), true);
        } else if (lastResult.length() > 0) {
            lastResult = "";
        }
    }
}
