import java.util.List;

public class Hand extends Deck {
    public Hand play(List<Card> set) {
        if (!hasAll(set)) return this;
        drawAll(set);
        return this;
    }
}
