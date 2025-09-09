package commands;

public interface Command {
    String execute();
    String getCommandName();
    default boolean requiresUser() {
        return false; // по умолчанию команда не требует пользователя
    }
}
