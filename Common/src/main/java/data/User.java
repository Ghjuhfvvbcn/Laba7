package data;

import java.io.Serializable;

public class User implements Serializable {
    private int id;
    private String login;
    // Пароль НЕ храним здесь. Он нужен только для аутентификации.

    public User(int id, String login) {
        this.id = id;
        this.login = login;
    }
    public void setId(int id){
        this.id = id;
    }
    public int getId(){
        return id;
    }

    public void setLogin(String login){
        this.login = login;
    }
    public String getLogin(){
        return login;
    }
}