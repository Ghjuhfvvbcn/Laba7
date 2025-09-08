package data;

import java.io.Serializable;
import java.util.Objects;

public final class Studio implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;

    public Studio(String name){
        setName(name);
    }

    private void setName(String name){
        if(name == null || name.trim().isEmpty()){
            throw new IllegalArgumentException("Name of studio value cannot be empty or null");
        }else{
            this.name = name;
        }
    }

    public String getName(){return name;}

    @Override
    public String toString(){
        return String.format("Studio[name=%s]", name);
    }

    @Override
    public boolean equals(Object other){
        if(this == other) return true;
        if(!(other instanceof Studio)) return false;
        Studio o = (Studio) other;
        return Objects.equals(name, o.name);
    }

    public int hashCode(){
        return Objects.hash(name);
    }
}