package fr.pantheonsorbonne.miage;

import fr.pantheonsorbonne.miage.exception.NoMoreCardException;
import fr.pantheonsorbonne.miage.game.Card;
import fr.pantheonsorbonne.miage.game.Deck;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.*;
import java.util.Map.Entry;

/**
 * this class is a abstract version of the engine, to be used locally on through the network
 */
public abstract class ScopaEngine {

    public static final int CARDS_IN_HAND_INITIAL_COUNT = 3;

    /**
     * play a scopa with the provided players
     */
    public void play() {
        //send the initial hand to every players
        Map<String, Queue<Card>> playerCollectedCards = new HashMap<>();
        Map<String, Integer> playerCollectedScopa = new HashMap<>();
        

        giveInitialHandToPLayers(playerCollectedCards, playerCollectedScopa);

        Queue<Card> roundDeck = new LinkedList<>(); // la table du jeu
        roundDeck.addAll(getInitialRoundDeck());

        // make a queue with all the players
        final Queue<String> players = new LinkedList<>();
        players.addAll(this.getInitialPlayers());
        //repeat until there are no more cards in deck

        //a revoir la condition d'arret
        while (Deck.deckSize > 1) {


            //take the first player form the queue
            String currentPlayer = players.poll();
            System.out.print("player " + currentPlayer + ": ");
            getPlayerCards(currentPlayer).stream().forEach(c -> System.out.print(c.toFancyString()));
            System.out.println();
            System.out.print("RoundDeck: ");
            roundDeck.stream().forEach(c -> System.out.print(c.toFancyString()));
            System.out.println();

            //and put it immediately at the end
            players.offer(currentPlayer);

            if (getPlayerCards(currentPlayer).size() > 0) {
                Card pairCard = makePair(currentPlayer, roundDeck);
                if (pairCard != null) {
                    Card retiredCard = removeRoundDeckCard(pairCard, roundDeck);
                    getPlayerCards(currentPlayer).remove(pairCard);
                    playerCollectedCards.get(currentPlayer).offer(retiredCard);
                    playerCollectedCards.get(currentPlayer).offer(pairCard);
                    if (roundDeck.isEmpty()) {
                        int counter = playerCollectedScopa.get(currentPlayer) + 1;
                        playerCollectedScopa.put(currentPlayer, counter);
                    }
                } else {
                    try {
                        // apply avoid to  put 7D strategy
                        Card selectedCard = getCardFromPlayer(currentPlayer);
                        roundDeck.offer(selectedCard);
                    } catch (NoMoreCardException e) {
                        ;
                    }
                }
            } else {
                Card[] cards = Deck.getRandomCards(CARDS_IN_HAND_INITIAL_COUNT);
                String hand = Card.cardsToString(cards);  //changer ça si on utilise pas le string
                giveCardsToPlayer(currentPlayer, hand);
            }

        }


        //since we've left the loop, we have only 1 player left: the winner
        String winner = players.poll();
        //send him the gameover and leave
        declareWinner(winner);
        System.out.println(winner + " won! bye");
        System.exit(0);
    }

    // voir ce que fait l'autre fonction getWinner() line 308 + eventually la supp/modif
    protected String getWinner(Map<String, Queue<Card>> playerCollectedCards){
        int maxCount=0;
        String winner = "";
        Map<String, Integer> playersScores = countPlayersScores(playerCollectedCards);
        for (Map.Entry player : playersScores.entrySet()){
            if (playersScores.get(player.toString()) > maxCount){
                maxCount=playersScores.get(player.toString());
                winner=player.toString();
            }
        }
        return winner;
    }

    protected int getWinnerScore(){
        
    }

    protected Map<String, Integer> countPlayersScores (Map<String, Queue<Card>> playerCollectedCards){
        Map<String, Integer> playerScore = new HashMap<>();
        for (Map.Entry player : playerCollectedCards.entrySet()){
            int count = 0;
            if (player.toString().equals(bestCount(playerCollectedCards))){
                count ++;
            }
            if (player.toString().equals(mostDenierCount(playerCollectedCards))){
                count++;
            }
            if (player.toString().equals(havingSettebello(playerCollectedCards))){
                count++;
            }
            playerScore.put(player.toString(), count);
        }

        return playerScore;
    }

    protected List<Card> getInitialRoundDeck() {
        return Arrays.asList(Deck.getRandomCards(4));
    }

    protected void giveInitialHandToPLayers(Map<String, Queue<Card>> playerCollectedCards, Map<String, Integer> playerCollectedScopa) {
        for (String playerName : getInitialPlayers()) {
            //get random cards
            Card[] cards = Deck.getRandomCards(CARDS_IN_HAND_INITIAL_COUNT);
            // transform them to String
            String hand = Card.cardsToString(cards);  //changer ça si on utilise pas le string
            //send them to this players
            giveCardsToPlayer(playerName, hand);
            playerCollectedCards.put(playerName, new LinkedList<>());
            playerCollectedScopa.put(playerName, 0);
            
        }
    }


    String bestCount(Map<String, Queue<Card>> playerCollectedCards) {
        int maxcount = 0;
        String bestPlayer = "";
        for (String player : playerCollectedCards.keySet()) {
            if (playerCollectedCards.get(player).size() > maxcount) {
                maxcount = playerCollectedCards.get(player).size();
                bestPlayer = player;
            }
        }
        return bestPlayer;
    }

    String mostDenierCount(Map<String, Queue<Card>> playerCollectedCards) {
        long maxcount = 0;
        String bestPlayer = "";
        for (String player : playerCollectedCards.keySet()) {
            long counter = playerCollectedCards.get(player).stream().filter(card -> card.getColor().name().equals("DIAMON")).count();
            if (counter > maxcount) {
                maxcount = counter;
                bestPlayer = player;
            }
        }
        return bestPlayer;
    }

    String havingSettebello(Map<String, Queue<Card>> playerCollectedCards) {
        for (String player : playerCollectedCards.keySet()) {
            if (playerCollectedCards.get(player).stream().filter(card -> card.toString().equals("7D")).count() > 0)
                return player;
        }
        return null;
    }

    Card removeRoundDeckCard(Card matchCard, Queue<Card> roundDeck) {
        Card returnCard;
        for (Card card : roundDeck) {
            if (card.getValue().getRank() == matchCard.getValue().getRank()) {
                returnCard = card;
                roundDeck.remove(card);
                return returnCard;
            }
        }
        return null;
    }

    Card makePair(String player, Queue<Card> roundDeck) {
        Queue<Card> playerCards = getPlayerCards(player);
        Card selectedCard = null;

        // apply 7D strategy
        for (Card card : roundDeck) {
            if (card.toString().equals("7D")) {
                for (Card pcard : getPlayerCards(player)) {
                    if (pcard.getValue().getStringRepresentation().equals("7")) {
                        return pcard;
                    }
                }
            }
        }

        // apply take max pair strategy
        int maxValue = 0;
        for (Card card : playerCards) {
            if (roundDeck.stream().map(crd -> crd.getValue()).filter(val -> val.equals(card.getValue())).count() > 0) {
                if (card.getValue().getRank() > maxValue) {
                    maxValue = card.getValue().getRank();
                    selectedCard = card;
                }
            }
        }
        return selectedCard;
    }


    /**
     * provide the list of the initial players to play the game
     *
     * @return
     */
    protected abstract Set<String> getInitialPlayers();

    protected abstract Queue<Card> getPlayerCards(String playerName);


    /**
     * give some card to a player
     *
     * @param playerName the player that will receive the cards
     * @param hand       the cards as a string (to be converted later)
     */
    protected abstract void giveCardsToPlayer(String playerName, String hand);

    /**
     * Play a single round
     *
     * @param players             the queue containing the remaining players
     * @param firstPlayerInRound  the first contestant in this round
     * @param secondPlayerInRound the second contestant in this roun
     * @param roundDeck           possible cards left over from previous rounds
     * @return true if we have a winner for this round, false otherwise
     */
    protected boolean playRound(Queue<String> players, String firstPlayerInRound, String secondPlayerInRound, Queue<Card> roundDeck) {


        //here, we try to get the first player card
        Card firstPlayerCard = getCardOrGameOver(roundDeck, firstPlayerInRound, secondPlayerInRound);
        if (firstPlayerCard == null) {
            players.remove(firstPlayerInRound);
            return true;
        }
        //here we also get the second player card
        Card secondPlayerCard = getCardOrGameOver(roundDeck, secondPlayerInRound, firstPlayerInRound);
        if (secondPlayerCard == null) {
            players.remove(secondPlayerInRound);
            return true;
        }

        //put the two cards on the roundDeck
        roundDeck.offer(firstPlayerCard);
        roundDeck.offer(secondPlayerCard);

        //compute who is the winner
        String winner = getWinner(firstPlayerInRound, secondPlayerInRound, firstPlayerCard, secondPlayerCard);
        //if there's a winner, we distribute the card to him
        if (winner != null) {
            giveCardsToPlayer(roundDeck, winner);
            return true;
        }
        //otherwise we do another round.
        return false;
    }

    /**
     * this method must be called when a winner is identified
     *
     * @param winner the final winner of the same
     */
    protected abstract void declareWinner(String winner);

    /**
     * get a card from a player. If the player doesn't have a card, it will be declared loser and all the left over cards will be given to his opponent
     *
     * @param leftOverCard               card left over from another round
     * @param cardProviderPlayer         the player that should give a card
     * @param cardProviderPlayerOpponent the Opponent of this player
     * @return a card of null if player cardProviderPlayer is gameover
     */
    protected abstract Card getCardOrGameOver(Collection<Card> leftOverCard, String cardProviderPlayer, String cardProviderPlayerOpponent);

    /**
     * give the winner of a round
     *
     * @param contestantA     a contestant
     * @param contestantB     another contestant
     * @param contestantACard its card
     * @param contestantBCard its card
     * @return the name of the winner or null if it's a tie
     */
    protected static String getWinner(String contestantA, String contestantB, Card contestantACard, Card contestantBCard) {
        if (contestantACard.getValue().getRank() > contestantBCard.getValue().getRank()) {
            return contestantA;
        } else if (contestantACard.getValue().getRank() < contestantBCard.getValue().getRank()) {
            return contestantB;
        }
        return null;
    }

    /**
     * give some card to a player
     *
     * @param playerName the player that will receive the cards
     * @param cards      the cards as a collection of cards
     */
    protected abstract void giveCardsToPlayer(Collection<Card> cards, String playerName);

    /**
     * get a card from a player
     *
     * @param player the player to give card
     * @return the card from the player
     * @throws NoMoreCardException if the player does not have a remaining card
     */
    protected abstract Card getCardFromPlayer(String player) throws NoMoreCardException;
}
