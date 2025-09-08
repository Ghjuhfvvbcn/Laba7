package data;

import java.io.Serializable;
import java.util.Objects;

public final class Coordinates implements Serializable {
    private static final long serialVersionUID = 1L;
    private Double x;
    private Integer y;

    public Coordinates(Double x, Integer y){
        setX(x);
        setY(y);
    }

    private void setX(Double x){
        if (x == null){
            throw new IllegalArgumentException("X coordinate value cannot be null");
        }else{
            this.x = x;
        }
    }

    public Double getX(){return x;}

    private void setY(Integer y){
        if (y == null){
            throw new IllegalArgumentException("Y coordinate value cannot be null");
        }else{
            this.y = y;
        }
    }

    public Integer getY(){return y;}

    @Override
    public String toString(){
        return String.format("Coordinates[x=%.2f, y=%d]", x, y);
    }

    @Override
    public boolean equals(Object other){
        if(this == other) return true;
        if(!(other instanceof Coordinates)) return false;
        Coordinates o = (Coordinates) other;
        return Double.compare(x, o.x) == 0 &&
                Objects.equals(y, o.y);
    }

    @Override
    public int hashCode(){
        return Objects.hash(x, y);
    }
}