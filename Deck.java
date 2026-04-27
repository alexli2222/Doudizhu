import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Deck {
    private List<Card> cards;

    public Deck() {
        cards = new ArrayList<>();
    }

    public Deck(List<Card> cards) {
        this.cards = cards;
    }

    public List<Card> getCards() {
        return cards;
    }

    public int cardCount() {
        return cards.size();
    }

    public boolean isEmpty() {
        return cards.size() == 0;
    }

    public boolean has(Card.Suit suit, Card.Rank rank) {
        Card card = new Card(suit, rank);
        return cards.stream().anyMatch(c -> c.equals(card));
    }

    public List<Card> hasN(Card.Rank rank, int n) {
        List<Card> found = new ArrayList<>();
        for (Card card : cards) {
            if (card.getRank() == rank) {
                found.add(card);
                if (found.size() == n) return found;
            }
        }
        return null;
    }

    public boolean has(Card card) {
        return cards.stream().anyMatch(c -> c.equals(card));
    }

    public boolean hasAll(List<Card> cards) {
        return cards.stream().allMatch(c -> this.cards.stream().anyMatch(d -> d.equals(c)));
    }

    public Card draw(Card.Suit suit, Card.Rank rank) {
        Card card = new Card(suit, rank);
        cards.removeIf(c -> c.equals(card));
        return card;
    }

    public Card draw(Card card) {
        cards.removeIf(c -> c.equals(card));
        return card;
    }

    public List<Card> drawAll(List<Card> cards) {
        cards.forEach(card -> this.cards.removeIf(c -> c.equals(card)));
        return cards;
    }

    public Deck add(Card.Suit suit, Card.Rank rank) {
        cards.add(new Card(suit, rank));
        return this;
    }

    public Deck add(Card card) {
        cards.add(card);
        return this;
    }

    public Deck addAll(List<Card> cards) {
        this.cards.addAll(cards);
        return this;
    }

    public void deal(Hand hand) {
        hand.add(cards.remove(0));
    }

    public Deck shuffle() {
        Collections.shuffle(cards);
        return this;
    }

    public enum SortType {
        SUIT_RANK_ASCENDING,
        SUIT_RANK_DESCENDING,
        RANK_SUIT_ASCENDING,
        RANK_SUIT_DESCENDING;
    }

    public Deck sort(SortType sortType) {
        switch (sortType) {
            case SUIT_RANK_ASCENDING -> srSort();
            case SUIT_RANK_DESCENDING -> srSort().reverse();
            case RANK_SUIT_ASCENDING -> rsSort();
            case RANK_SUIT_DESCENDING -> rsSort().reverse();
        }
        return this;
    }

    public Deck sort(HashMap<Card, Integer> values) {
        cards.sort(Comparator.comparingInt(card ->
            values.entrySet().stream()
                .filter(e -> e.getKey().equals(card))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(0)));
        return this;
    }

    public Deck sort(HashMap<Card, Integer> values, boolean reverse) {
        sort(values);
        if (reverse) reverse();
        return this;
    }

    //default is ascending
    private Deck srSort() {
        List<Card> sorted = new ArrayList<>();
        for (Card.Suit suit : Card.Suit.values()) {
            List<Card> temp = new ArrayList<>();
            cards.forEach(card -> {
                if (card.getSuit() == suit) temp.add(card);
            });
            temp.sort(Comparator.comparingInt(c -> c.getRank().getNumericValue()));
            sorted.addAll(temp);
        }
        cards = sorted;
        return this;
    }

    private Deck rsSort() {
        List<Card> sorted = new ArrayList<>();
        for (Card.Rank rank : Card.Rank.values()) {
            List<Card> temp = new ArrayList<>();
            cards.forEach(card -> {
                if (card.getRank() == rank) temp.add(card);
            });
            temp.sort(Comparator.comparingInt(c -> c.getSuit().getNumericValue()));
            sorted.addAll(temp);
        }
        cards = sorted;
        return this;
    }

    private Deck reverse() {
        List<Card> reverse = new ArrayList<>();
        cards.forEach(card -> {
            reverse.add(0, card);
        });
        cards = reverse;
        return this;
    }

    public Deck clone() {
        return new Deck(new ArrayList<>(cards));
    }

    public String toString() {
        return cards.toString();
    }

    private static final Deck STANDARD_DECK;

    static {
        List<Card> cards = new ArrayList<>();
        for (Card.Suit suit : Card.Suit.values()) {
            if (suit != Card.Suit.JOKER) {
                for (Card.Rank rank : Card.Rank.values()) {
                    if (rank.isStandard())
                        cards.add(0, new Card(suit, rank));
                }
            }
        }
        cards.add(0, new Card(Card.Suit.JOKER, Card.Rank.SMALL));
        cards.add(0, new Card(Card.Suit.JOKER, Card.Rank.BIG));
        STANDARD_DECK = new Deck(cards);
    }

    public static Deck createStandardDeck() {
        return STANDARD_DECK.clone();
    }
}