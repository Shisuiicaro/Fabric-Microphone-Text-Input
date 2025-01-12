package github.jaffe2718.mcmti.client;

import github.jaffe2718.mcmti.client.event.EventHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class MicrophoneTextInputClient implements ClientModInitializer {
    public static KeyBinding vKeyBinding;
    /**
     * Runs the mod initializer on the client environment.
     */
    @Override
    public void onInitializeClient() {
        vKeyBinding = new KeyBinding("Toggle speech recognition", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_M, "GLOBAL TRANSLATIONS"   );
        KeyBindingHelper.registerKeyBinding(vKeyBinding);
        EventHandler.register();
    }
}