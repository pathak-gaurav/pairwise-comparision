package laurentian.pairwise.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * This class will hold the triad, A list will be created of Triad so for a PC matrix list of all
 * Triad can be stored.
 * */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Triad implements Comparable<Triad> {
    private double i;
    private double j;
    private double k;
    private double X;
    private double Y;
    private double Z;
    private double kii;

    @Override
    public int compareTo(Triad t) {
        return Double.compare(this.getKii(), t.kii);
    }
}
