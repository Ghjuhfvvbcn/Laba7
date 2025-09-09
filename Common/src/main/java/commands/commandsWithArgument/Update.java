package commands.commandsWithArgument;

import commands.CommandWithUser;
import commands.Executor;
import data.MusicBand;
import data.User;

public class Update implements CommandWithArgument<Long>, CommandWithUser {
    private String commandName = "update";
    private Long argument;
    private Executor executor;

    public Update(Executor executor){
        this.executor = executor;
    }

    @Override
    public String execute(){
        throw new UnsupportedOperationException("This command requires user authentication");
    }
    @Override
    public String execute(User user) {
        if (user == null) {
            return "Error: Authentication required";
        }
        return executor.update(argument, null, user);
    }

    public String executeWithMusicBand(MusicBand band, User user) {
        if (user == null) {
            return "Error: Authentication required";
        }
        return executor.update(argument, band, user);
    }

    @Override
    public void setArgument(String argument){
        Long arg;
        try{
            arg = Long.parseLong(argument.trim());
            if(arg <= 0) throw new NumberFormatException();
        }catch(NumberFormatException | NullPointerException e){
            throw new IllegalArgumentException("Command '" + commandName + "' failed: '" + argument + "' is not a valid Long number.");
        }
        this.argument = arg;
    }

    @Override
    public Long getArgument(){return argument;}

    @Override
    public String getCommandName(){return commandName;}

    @Override
    public boolean requiresUser() {
        return true; // требует аутентификации
    }
}
