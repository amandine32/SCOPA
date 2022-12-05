package fr.pantheonsorbonne.miage;

import com.google.common.collect.Lists;
import fr.pantheonsorbonne.miage.enums.CardColor;
import fr.pantheonsorbonne.miage.enums.CardValue;
import fr.pantheonsorbonne.miage.game.Card;
import fr.pantheonsorbonne.miage.game.Deck;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Unit test for simple App.
 */
public class AppTest {


    class LocalScopaTest extends LocalScopa {
        Map<String, Card[]> affectedCards;
        List<Card> initialRoundDeck;

        public LocalScopaTest(Set<String> initialPlayers, Map<String, Card[]> affectedCards, List<Card> initialRoundDeck) {
            super(initialPlayers);
            this.affectedCards = affectedCards;
            this.initialRoundDeck = initialRoundDeck;
        }

        @Override
        protected List<Card> getInitialRoundDeck() {
            return initialRoundDeck;
        }

        @Override
        protected void giveInitialHandToPLayers(Map<String, Queue<Card>> playerCollectedCards, Map<String, Integer> playerCollectedScopa) {
            for (String playerName : getInitialPlayers()) {
                //get random cards
                Card[] cards = affectedCards.get(playerName);
                // transform them to String
                String hand = Card.cardsToString(cards);  //changer ça si on utilise pas le string
                //send them to this players
                giveCardsToPlayer(playerName, hand);
                playerCollectedCards.put(playerName, new LinkedList<>());
                playerCollectedScopa.put(playerName, 0);
            }
        }
    }

    @Test
    public void shouldAnswerWithTrue() {

        Card[] cardsJ1 = new Card[]{new Card(CardColor.BATON, CardValue.ACE), new Card(CardColor.DENIER, CardValue.SEVEN)};
        Card[] cardsJ2 = new Card[]{new Card(CardColor.EPEE, CardValue.FOUR), new Card(CardColor.EPEE, CardValue.FIVE)};
        Deck.getRandomCards(40);
        LocalScopaTest localScopaTest = new LocalScopaTest(

                Arrays.stream((new String[]{"J1", "J2"})).collect(Collectors.toSet()), Map.of("J1", cardsJ1, "J2", cardsJ2), Lists.newArrayList(new Card(CardColor.BATON, CardValue.KING)));
        localScopaTest.play();

    }


}
