package proxyBanSystem.sponge;

import net.kyori.adventure.text.Component;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.CommandCompletion;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.ArgumentReader;
import proxyBanSystem.platform.ProxyCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SpongeCommandWrapper implements Command.Raw {

    private final ProxyCommand command;

    public SpongeCommandWrapper(ProxyCommand command) {
        this.command = command;
    }

    @Override
    public CommandResult process(CommandCause cause, ArgumentReader.Mutable arguments) throws CommandException {
        command.execute(new SpongeCommandSource(cause), splitArguments(arguments.remaining(), false));
        return CommandResult.success();
    }

    @Override
    public List<CommandCompletion> complete(CommandCause cause, ArgumentReader.Mutable arguments) throws CommandException {
        return command.suggest(new SpongeCommandSource(cause), splitArguments(arguments.remaining(), true))
                .stream()
                .map(CommandCompletion::of)
                .toList();
    }

    @Override
    public boolean canExecute(CommandCause cause) {
        return true;
    }

    @Override
    public Optional<Component> shortDescription(CommandCause cause) {
        return Optional.empty();
    }

    @Override
    public Optional<Component> extendedDescription(CommandCause cause) {
        return Optional.empty();
    }

    @Override
    public Component usage(CommandCause cause) {
        return Component.text("");
    }

    private String[] splitArguments(String input, boolean complete) {
        if (input == null || input.isEmpty()) {
            return complete ? new String[]{""} : new String[0];
        }

        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return complete ? new String[]{""} : new String[0];
        }

        List<String> parts = new ArrayList<>(List.of(trimmed.split("\\s+")));
        if (complete && Character.isWhitespace(input.charAt(input.length() - 1))) {
            parts.add("");
        }

        return parts.toArray(String[]::new);
    }
}
