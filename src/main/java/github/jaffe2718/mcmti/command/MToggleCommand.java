package github.jaffe2718.mcmti.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;


public class MToggleCommand {
    private static boolean isCapturingAudio = true;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> command = CommandManager.literal("mtoggle")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.argument("toggle", BoolArgumentType.bool())
                        .executes(MToggleCommand::toggleCapture));
        dispatcher.register(command);
    }

    private static int toggleCapture(CommandContext<ServerCommandSource> context) {
        boolean newValue = BoolArgumentType.getBool(context, "toggle");
        isCapturingAudio = newValue;

        String message = isCapturingAudio ? "Captura de áudio ativada." : "Captura de áudio desativada.";
        context.getSource().sendFeedback(() -> Text.of(message), false);
        return 1;
    }
// ZSMP E FODA
    public static boolean isCapturingAudio() {
        return isCapturingAudio;
    }
}
