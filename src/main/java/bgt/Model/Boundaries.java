package bgt.Model;

/**
 * Created by UDU on 7/28/2017.
 */

public class Boundaries{

    private final double minlat;
    private final double maxlat;
    private final double maxlon;
    private final double minlon;
    private String origin="osmconvert 0.8.5";

    public Boundaries(double minlat, double maxlat, double maxlon, double minlon, String origin ) {
        this.minlat = minlat;
        this.maxlat = maxlat;
        this.maxlon = maxlon;
        this.minlon = minlon;
    }

    public double getMaxlat() {
        return maxlat;
    }

    public double getMaxlon() {
        return maxlon;
    }

    public double getMinlat() {
        return minlat;
    }

    public double getMinlon() {
        return minlon;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }
}
