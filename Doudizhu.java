import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;

public class Doudizhu {
    private static EngineBridge bridge = new EngineBridge("./Doudizhu");
    private static boolean firstListener = true;

    public static void main(String[] args) {
        // Scanner scanner = new Scanner(System.in);
        // CardSet cs1 = input(scanner);
        // CardSet cs2 = input(scanner);
        // System.out.println("Compared to the first set, the second set is "+cs2.compare(cs1));
        bridge.startListening(firstListener);
        firstListener = false;
        bridge.sendCommand("isready");

        new Doudizhu().play();
    }

    private static CardSet input(Scanner scanner) {
        System.out.print("Enter set of cards (ranks only): ");
        String input = scanner.nextLine();
        List<Card> set = new ArrayList<>();
        String[] strings = input.split(" ");
        for (String string : strings) {
            Card.Rank rank = null;
            for (Card.Rank r : Card.Rank.values()) {
                if (r.getDisplay().equals(string)) rank = r;
            }
            set.add(new Card(Card.Suit.SPADES, rank));
        }
        CardSet cardSet = getCardSet(set);
        System.out.println(cardSet);
        return cardSet;
    }

    private static final HashMap<Card, Integer> SORT_ORDER;

    static {
        SORT_ORDER = new HashMap<>();
        for (Card.Rank rank : Card.Rank.values()) {
            if (rank.isStandard()) {
                for (Card.Suit suit : Card.Suit.values()) {
                    if (suit != Card.Suit.JOKER) {
                        int value = (rank == Card.Rank.TWO) ? 15 : rank.getNumericValue();
                        SORT_ORDER.put(new Card(suit, rank), value);
                    }
                }
            }
        }
        SORT_ORDER.put(new Card(Card.Suit.JOKER, Card.Rank.SMALL), 16);
        SORT_ORDER.put(new Card(Card.Suit.JOKER, Card.Rank.BIG), 17);
    }

    private static int getValue(Card card) {
        return SORT_ORDER.entrySet().stream()
            .filter(e -> e.getKey().equals(card))
            .map(e -> e.getValue())
            .findFirst()
            .orElse(0);
    }

    private static class Player extends Hand {
        Scanner scanner;
        String name;

        Player(Scanner scanner, String name) {
            this.scanner = scanner;
            this.name = name;
        }

        public String toString() {
            return name+"'s Cards: "+super.toString();
        }

        int bid() {
            int bid = -1;
            while (bid > 3 || bid < 0) {
                System.out.print("Player "+name+" bids: ");
                bid = scanner.nextInt();
            }
            scanner.nextLine(); // consume leftover newline after nextInt
            return bid;
        }

        CardSet turn(CardSet current) {
            System.out.println(this);
            CardSet play = input(current);
            play.display(this);
            return play;
        }

        CardSet input(CardSet current) {
            List<Card> set = null;
            boolean valid = false;
            CardSet cardSet = null;
            while (!valid) {
                System.out.print(name+", please enter the set you wish to play (ranks only, blank to pass): ");
                String input = scanner.nextLine().trim();
                if (input.isEmpty() || input.equalsIgnoreCase("pass")) {
                    if (current == null) {
                        System.out.println("You control the table — you must play.");
                        continue;
                    }
                    System.out.println(name+" passed");
                    return null;
                }
                String[] strings = input.split(" ");
                HashMap<Card.Rank, Integer> counts = new HashMap<>();
                for (String string : strings) {
                    Card.Rank rank = null;
                    for (Card.Rank r : Card.Rank.values()) {
                        if (r.getDisplay().equals(string)) rank = r;
                    }
                    if (rank != null) {
                        counts.put(rank, counts.getOrDefault(rank, 0) + 1);
                    }
                }
                set = canPlay(counts);
                if (set != null) {
                    cardSet = getCardSet(set);
                    if (current == null) valid = true;
                    else if (cardSet != null)
                        valid = cardSet.compare(current) == CardSet.ComparisonResult.LARGER;
                }
            }
            play(set);
            return cardSet;
        }

        List<Card> canPlay(HashMap<Card.Rank, Integer> req) {
            List<Card> all = new ArrayList<>();
            for (var entry : req.entrySet()) {
                Card.Rank rank = entry.getKey();
                int count = entry.getValue();
                List<Card> possible = hasN(rank, count);
                if (possible == null) return null;
                all.addAll(possible);
            }
            return (all.isEmpty() ? null : all);
        }
    }

    private final static class CardSet {
        static enum Type {
            SINGLE(false) {
                @Override
                String formatted(List<Card> main, List<Card> burn) {
                    return "a single "+main.getFirst().getRank().getDisplay();
                }
            },
            PAIR(false) {
                @Override
                String formatted(List<Card> main, List<Card> burn) {
                    return "a pair of "+main.getFirst().getRank().getDisplay()+"'s";
                }
            },
            TRIPLE(false) {
                @Override
                String formatted(List<Card> main, List<Card> burn) {
                    return "a triplet of "+main.getFirst().getRank().getDisplay()+"'s";
                }
            },
            TRIPLE_BURN_SINGLE(false) {
                @Override
                String formatted(List<Card> main, List<Card> burn) {
                    return "a triplet of "+main.getFirst().getRank().getDisplay()+"'s while burning a single "+burn.getFirst().getRank().getDisplay();
                }
            },
            TRIPLE_BURN_PAIR(false) {
                @Override
                String formatted(List<Card> main, List<Card> burn) {
                    return "a triplet of "+main.getFirst().getRank().getDisplay()+"'s while burning a pair of "+burn.getFirst().getRank().getDisplay()+"'s";
                }
            },
            STRAIGHT(false) {
                @Override
                String formatted(List<Card> main, List<Card> burn) {
                    return "a straight of "+main.size()+" singles from "+main.getFirst().getRank().getDisplay()+" to "+main.getLast().getRank().getDisplay();
                }
            },
            PAIR_STRAIGHT(false) {
                @Override
                String formatted(List<Card> main, List<Card> burn) {
                    return "a straight of "+(main.size() / 2)+" pairs from "+main.getFirst().getRank().getDisplay()+" to "+main.getLast().getRank().getDisplay();
                }
            },
            TRIPLE_STRAIGHT(false) {
                @Override
                String formatted(List<Card> main, List<Card> burn) {
                    return "a straight of "+(main.size() / 3)+" triplets from "+main.getFirst().getRank().getDisplay()+" to "+main.getLast().getRank().getDisplay();
                }
            },
            TRIPLE_STRAIGHT_BURN_SINGLE(false) {
                @Override
                String formatted(List<Card> main, List<Card> burn) {
                    String[] separated = (String[]) burn.stream().map(String::valueOf).toArray();
                    return "a straight of "+(main.size() / 3)+" triplets from "+main.getFirst().getRank().getDisplay()+" to "+main.getLast().getRank().getDisplay()+" while burning the singles: "+String.join(", ", separated);
                }
            },
            TRIPLE_STRAIGHT_BURN_PAIR(false) {
                @Override
                String formatted(List<Card> main, List<Card> burn) {
                    List<Card> combed = new ArrayList<>();
                    for (int i = 0; i < burn.size(); i+=2) combed.add(burn.get(i));
                    String[] separated = (String[]) burn.stream().map(String::valueOf).toArray();
                    return "a straight of "+(main.size() / 3)+" triplets from "+main.getFirst().getRank().getDisplay()+" to "+main.getLast().getRank().getDisplay()+" while burning the pairs: "+String.join(", ", separated);
                }
            },
            BOMB(true) {
                @Override
                String formatted(List<Card> main, List<Card> burn) {
                    return "a "+main.getFirst().getRank().getDisplay()+" bomb";
                }
            },
            QUAD_BURN_SINGLE(false) {
                @Override
                String formatted(List<Card> main, List<Card> burn) {
                    return "quadruple "+main.getFirst().getRank().getDisplay()+"'s while burning a single "+burn.getFirst().getRank().getDisplay();
                }
            },
            QUAD_BURN_PAIR(false) {
                @Override
                String formatted(List<Card> main, List<Card> burn) {
                    return "quadruple "+main.getFirst().getRank().getDisplay()+"'s while burning a pair of "+burn.getFirst().getRank().getDisplay();
                }
            },
            ROCKET(true) {
                @Override
                String formatted(List<Card> main, List<Card> burn) {
                    return "a rocket";
                }
            };

            private boolean canOverride;

            Type(boolean canOverride) {
                this.canOverride = canOverride;
            }

            abstract String formatted(List<Card> main, List<Card> burn);
        }

        Type type;
        List<Card> main;
        List<Card> burn;
        int weight;
        int extra;

        CardSet(Type type, List<Card> main) {
            this(type, main, List.of(), 0);
        }

        CardSet(Type type, List<Card> main, List<Card> burn) {
            this(type, main, burn, 0);
        }
        
        CardSet(Type type, List<Card> main, int extra) {
            this(type, main, List.of(), extra);
        }
        
        CardSet(Type type, List<Card> main, List<Card> burn, int extra) {
            this.type = type;
            this.main = main;
            this.burn = burn;
            this.extra = extra;

            this.weight = getValue(main.getLast());
            if (type == Type.BOMB) weight *= 100;
            else if (type == Type.ROCKET) weight *= 10000;
        }

        public String toString() {
            return "Type: "+type+", Main: "+main+", Burn: "+burn+", Weight: "+weight+", Extra: "+extra;
        }

        static enum ComparisonResult {
            LARGER,
            EQUAL,
            SMALLER,
            INCOMPARABLE;
        }

        ComparisonResult compare(CardSet other) {
            if (!type.canOverride && (type != other.type || extra != other.extra)) return ComparisonResult.INCOMPARABLE;
            int diff = weight - other.weight;
            if (diff > 0) return ComparisonResult.LARGER;
            if (diff < 0) return ComparisonResult.SMALLER;
            return ComparisonResult.EQUAL;
        }

        void display(Player player) {
            System.out.println(player.name+" played "+type.formatted(main, burn));
        }
    }

    private static List<Card> sort(List<Card> set) {
        set.sort(Comparator.comparingInt(card ->
            getValue(card)
        ));
        return set;
    }

    Scanner scanner;
    //prebid
    private Player p0;
    private Player p1;
    private Player p2;
    //postbid
    private Player dizhu;
    private Player nongmin1;
    private Player nongmin2;

    private CardSet currentSet;

    public Doudizhu() {
        scanner = new Scanner(System.in);
        currentSet = null;
    }

    public void play() {
        boolean proceed = false;
        while (!proceed) {
            p0 = new Player(scanner, "1");
            p1 = new Player(scanner, "2");
            p2 = new Player(scanner, "3");
            List<Card> di = deal();
            proceed = bid(di);
        }
        System.out.println(dizhu);
        System.out.println(nongmin1);
        System.out.println(nongmin2);
        gameLoop();
    }

    private List<Card> deal() {
        Deck deck = Deck.createStandardDeck();
        deck.shuffle();
        int i = 0;
        while (deck.cardCount() > 3) {
            switch (i) {
                case 0 -> deck.deal(p0);
                case 1 -> deck.deal(p1);
                case 2 -> deck.deal(p2);
            }
            i = (i + 1) % 3;
        }
        p0.sort(SORT_ORDER, true);
        p1.sort(SORT_ORDER, true);
        p2.sort(SORT_ORDER, true);
        System.out.println(p0);
        System.out.println(p1);
        System.out.println(p2);
        System.out.println("Di: "+deck);
        return deck.getCards();
    }

    private boolean bid(List<Card> di) {
        int highestBid = 0;
        Player toBeDizhu = null;
        Player toBeNongmin1 = null;
        Player toBeNongmin2 = null;

        int p0b = p0.bid();
        if (p0b != 0) {
            toBeDizhu = p0;
            toBeNongmin1 = p1;
            toBeNongmin2 = p2;
            highestBid = p0b;
        }
        int p1b = p1.bid();
        if (p1b > highestBid) {
            toBeDizhu = p1;
            toBeNongmin1 = p0;
            toBeNongmin2 = p2;
            highestBid = p1b;
        }
        int p2b = p2.bid();
        if (p2b > highestBid) {
            toBeDizhu = p2;
            toBeNongmin1 = p0;
            toBeNongmin2 = p1;
            highestBid = p2b;
        }
        
        if (highestBid == 0) return false;
        dizhu = toBeDizhu;
        nongmin1 = toBeNongmin1;
        nongmin2 = toBeNongmin2;
        dizhu.name = "Dizhu";
        nongmin1.name = "Nongmin 1";
        nongmin2.name = "Nongmin 2";
        dizhu.addAll(di);
        dizhu.sort(SORT_ORDER, true);
        return true;
    }

    private int gameLoop() {
        int turn = 0;
        int owner = -1; // last person to play a bigger card, if its you then that means start new trick
        int won = -1; // 0 = dizhu wins, 1 = nongmin wins
        while (won < 0) {
            Player current = switch (turn) {
                case 0 -> dizhu;
                case 1 -> nongmin1;
                default -> nongmin2;
            };
            if (turn == owner) {
                currentSet = null;
            }
            CardSet played = current.turn(currentSet);
            if (played != null) {
                currentSet = played;
                owner = turn;
            }
            if (dizhu.isEmpty()) won = 0;
            else if (nongmin1.isEmpty() || nongmin2.isEmpty()) won = 1;
            turn = (turn + 1) % 3;
        }
        return won;
    }

    private static CardSet getCardSet(List<Card> set) {
        List<Function<List<Card>, CardSet>> checks = List.of(
            Doudizhu::isSingle,
            Doudizhu::isPair,
            Doudizhu::isTriple,
            Doudizhu::isTripleBurnSingle,
            Doudizhu::isTripleBurnPair,
            Doudizhu::isStraight,
            Doudizhu::isPairStraight,
            Doudizhu::isTripleStraight,
            Doudizhu::isTripleStraightBurnSingle,
            Doudizhu::isTripleStraightBurnPair,
            Doudizhu::isBomb,
            Doudizhu::isQuadBurnSingle,
            Doudizhu::isQuadBurnPair,
            Doudizhu::isRocket
        );
        CardSet result = null;
        for (var function : checks) {
            result = function.apply(new ArrayList<>(set));
            if (result != null) return result;
        }
        return null;
    }

    private static CardSet isSingle(List<Card> set) {
        return (set.size() == 1) ? new CardSet(CardSet.Type.SINGLE, set) : null;
    }

    private static CardSet isPair(List<Card> set) {
        if (set.size() != 2) return null;
        return (set.getFirst().getRank() == set.get(1).getRank()) ? new CardSet(CardSet.Type.PAIR, set) : null;
    }

    private static CardSet isTriple(List<Card> set) {
        if (set.size() != 3) return null;
        Card.Rank rank = set.getFirst().getRank();
        for (Card card : set) {
            if (card.getRank() != rank) return null;
        }
        return new CardSet(CardSet.Type.TRIPLE, set);
    }

    private static Card.Rank containsTriple(List<Card> set) {
        if (set.size() < 3) return null;
        HashMap<Card.Rank, Integer> count = new HashMap<>();
        for (Card card : set) {
            count.put(card.getRank(), count.getOrDefault(card.getRank(), 0)+1);
        }
        for (var entry : count.entrySet()) {
            if (entry.getValue() == 3) return entry.getKey();
        }
        return null;
    }

    private static CardSet isTripleBurnSingle(List<Card> set) {
        if (set.size() != 4) return null;
        Card.Rank triple = containsTriple(set);
        if (triple == null) return null;
        List<Card> main = new ArrayList<>();
        set.removeIf(c -> c.getRank() == triple && main.add(c));
        return new CardSet(CardSet.Type.TRIPLE_BURN_SINGLE, main, set);
    }

    private static CardSet isTripleBurnPair(List<Card> set) {
        if (set.size() != 5) return null;
        Card.Rank triple = containsTriple(set);
        if (triple == null) return null;
        List<Card> main = new ArrayList<>();
        set.removeIf(c -> c.getRank() == triple && main.add(c));
        if (isPair(set) == null) return null;
        return new CardSet(CardSet.Type.TRIPLE_BURN_PAIR, main, set);
    }

    private static CardSet isStraight(List<Card> set) {
        if (set.size() < 5 || set.size() > 14) return null;
        sort(set);
        for (int i = 1; i < set.size(); i++) {
            Card card = set.get(i);
            if (card.getSuit() == Card.Suit.JOKER || card.getRank() == Card.Rank.TWO) return null;
            if (card.getRank().getNumericValue() != set.get(i-1).getRank().getNumericValue()+1) return null;
        }
        return new CardSet(CardSet.Type.STRAIGHT, set, set.size());
    }

    private static CardSet isPairStraight(List<Card> set) {
        if (set.size() < 6 || set.size() > 28 || set.size() % 2 != 0) return null;
        sort(set);
        for (int i = 1; i < set.size(); i++) {
            Card card = set.get(i);
            if (card.getSuit() == Card.Suit.JOKER || card.getRank() == Card.Rank.TWO) return null;
            Card last = set.get(i-1);
            if (i % 2 == 0)
                if (getValue(card) != getValue(last)+1) return null;
            else
                if (card.getRank() != last.getRank());
        }
        return new CardSet(CardSet.Type.PAIR_STRAIGHT, set, set.size() / 2);
    }

        private static CardSet isTripleStraight(List<Card> set) {
        if (set.size() < 6) return null;
        List<Card> triples = new ArrayList<>();
        Card.Rank currentTriple;
        while ((currentTriple = containsTriple(set)) != null) {
            currentTriple = containsTriple(set);
            for (int i = 0; i < set.size(); i++) {
                Card card = set.get(i);
                if (card.getRank() == currentTriple && triples.add(card)) {
                    set.remove(card);
                    i--;
                }
            }
        }
        if (triples.size() < 6) return null;
        sort(triples);
        for (int i = 1; i < triples.size(); i++) {
            Card card = triples.get(i);
            if (card.getSuit() == Card.Suit.JOKER || card.getRank() == Card.Rank.TWO) return null;
            Card last = triples.get(i-1);
            if (i % 3 == 0)
                if (getValue(card) != getValue(last)+1) return null;
            else
                if (card.getRank() != last.getRank()) return null;
        }
        return new CardSet(CardSet.Type.TRIPLE_STRAIGHT, triples, triples.size() / 3);
    }

    private static CardSet isTripleStraightBurnSingle(List<Card> set) {
        if (set.size() < 8) return null;
        List<Card> triples = new ArrayList<>();
        Card.Rank currentTriple;
        while ((currentTriple = containsTriple(set)) != null) {
            currentTriple = containsTriple(set);
            for (int i = 0; i < set.size(); i++) {
                Card card = set.get(i);
                if (card.getRank() == currentTriple && triples.add(card)) {
                    set.remove(card);
                    i--;
                }
            }
        }
        if (triples.size() < 6) return null;
        sort(triples);
        for (int i = 1; i < triples.size(); i++) {
            Card card = triples.get(i);
            if (card.getSuit() == Card.Suit.JOKER || card.getRank() == Card.Rank.TWO) return null;
            Card last = triples.get(i-1);
            if (i % 3 == 0)
                if (getValue(card) != getValue(last)+1) return null;
            else
                if (card.getRank() != last.getRank()) return null;
        }
        sort(set);
        if (set.size() == triples.size() / 3)
            return new CardSet(CardSet.Type.TRIPLE_STRAIGHT_BURN_SINGLE, triples, set, triples.size() / 3);
        return null;
    }

    private static CardSet isTripleStraightBurnPair(List<Card> set) {
        if (set.size() < 10) return null;
        List<Card> triples = new ArrayList<>();
        Card.Rank currentTriple;
        while ((currentTriple = containsTriple(set)) != null) {
            currentTriple = containsTriple(set);
            for (int i = 0; i < set.size(); i++) {
                Card card = set.get(i);
                if (card.getRank() == currentTriple && triples.add(card)) {
                    set.remove(card);
                    i--;
                }
            }
        }
        if (triples.size() < 6) return null;
        sort(triples);
        for (int i = 1; i < triples.size(); i++) {
            Card card = triples.get(i);
            if (card.getSuit() == Card.Suit.JOKER || card.getRank() == Card.Rank.TWO) return null;
            Card last = triples.get(i-1);
            if (i % 3 == 0)
                if (getValue(card) != getValue(last)+1) return null;
            else
                if (card.getRank() != last.getRank()) return null;
        }
        sort(set);
        if (set.size() == triples.size() / 3 * 2) {
            for (int i = 1; i < set.size(); i+=2) {
                if (set.get(i).getRank() != set.get(i-1).getRank()) return null;
            }
            return new CardSet(CardSet.Type.TRIPLE_STRAIGHT_BURN_PAIR, triples, set, triples.size() / 3);
        }
        return null;
    }

    private static CardSet isBomb(List<Card> set) {
        if (set.size() != 4) return null;
        Card.Rank rank = set.getFirst().getRank();
        for (Card card : set) {
            if (card.getRank() != rank) return null;
        }
        return new CardSet(CardSet.Type.BOMB, set);
    }

    private static Card.Rank containsQuad(List<Card> set) {
        if (set.size() < 4) return null;
        HashMap<Card.Rank, Integer> count = new HashMap<>();
        for (Card card : set) {
            count.put(card.getRank(), count.getOrDefault(card.getRank(), 0)+1);
        }
        for (var entry : count.entrySet()) {
            if (entry.getValue() == 4) return entry.getKey();
        }
        return null;
    }

    private static CardSet isQuadBurnSingle(List<Card> set) {
        if (set.size() != 6) return null;
        Card.Rank quad = containsQuad(set);
        if (quad == null) return null;
        List<Card> main = new ArrayList<>();
        set.removeIf(c -> c.getRank() == quad && main.add(c));
        return new CardSet(CardSet.Type.QUAD_BURN_SINGLE, main, sort(set));
    }

    private static CardSet isQuadBurnPair(List<Card> set) {
        if (set.size() != 8) return null;
        Card.Rank quad = containsQuad(set);
        if (quad == null) return null;
        List<Card> main = new ArrayList<>();
        set.removeIf(c -> c.getRank() == quad && main.add(c));
        sort(set);
        if (set.get(0).getRank() == set.get(1).getRank() && set.get(2).getRank() == set.get(3).getRank() && set.get(0).getRank() != set.get(2).getRank())
            return new CardSet(CardSet.Type.QUAD_BURN_PAIR, main, set);
        else return null;
    }

    private static CardSet isRocket(List<Card> set) {
        if (set.size() != 2) return null;
        sort(set);
        if (!set.get(0).equals(new Card(Card.Suit.JOKER, Card.Rank.SMALL))) return null;
        if (!set.get(1).equals(new Card(Card.Suit.JOKER, Card.Rank.BIG))) return null;
        return new CardSet(CardSet.Type.ROCKET, set);
    }
}
