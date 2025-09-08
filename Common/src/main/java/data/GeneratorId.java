package data;

import java.io.Serializable;

class GeneratorId implements Serializable {
    private static final long serialVersionUID = 1L;
    private static Long id = 1L;

    public static Long generateId(){
        return id++;
    }

    public static void setId(Long newId){
        if (id < newId){
            id = newId;
        }
    }
}