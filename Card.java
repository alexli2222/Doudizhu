public class Card {
    private final Suit suit;
    private final Rank rank;

    public Card(Suit suit, Rank rank) {
        this.suit = suit;
        this.rank = rank;
    }

    public static enum Suit {
        DIAMONDS(1, "♢"),
        CLUBS(2, "♧"),
        HEARTS(3, "♡"),
        SPADES(4, "♤"),
        JOKER(5, "🃏");

        private int numericValue;
        private String display;

        Suit(int numericValue, String display) {
            this.numericValue = numericValue;
            this.display = display;
        }

        public int getNumericValue() {
            return numericValue;
        }

        public String getDisplay() {
            return display;
        }
    }

    public static enum Rank {
        TWO(2, "2"),
        THREE(3, "3"),
        FOUR(4, "4"),
        FIVE(5, "5"),
        SIX(6, "6"),
        SEVEN(7, "7"),
        EIGHT(8, "8"),
        NINE(9, "9"),
        TEN(10, "10"),
        JACK(11, "J"),
        QUEEN(12, "Q"),
        KING(13, "K"),
        ACE(14, "A"),
        SMALL(15, "S"),
        BIG(16, "B");
        
        private int numericValue;
        private String display;
        
        Rank(int numericValue, String display) {
            this.numericValue = numericValue;
            this.display = display;
        }

        public int getNumericValue() {
            return numericValue;
        }

        public String getDisplay() {
            return display;
        }

        public boolean isStandard() {
            return (this != SMALL && this != BIG);
        }
    }

    public Suit getSuit() {
        return suit;
    }
    
    public Rank getRank() {
        return rank;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Card other)) return false;
        return suit == other.suit && rank == other.rank;
    }

    public String toString() {
        return suit.display + rank.display;
    }
}
