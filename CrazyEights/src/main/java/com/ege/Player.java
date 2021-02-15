package com.ege;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class Player implements Serializable {

    /*
     * score sheet is saved as a hashmap upper one, two, three, four, five, six
     * lower 3ok, 4ok, full, sst, lst, yahtzee, chance, lowerbonus, upperbonus
     */

    /*
     *
     */
    private static final long serialVersionUID = 1L;
    public String name;

    int playerId = 0;

    static Client clientConnection;

    Player[] players = new Player[3];


    /*
     * play a round of the game
     */



    public Player getPlayer() {
        return this;
    }

    /*
     * ----------Network Stuff------------
     */

    /*
     * send the to do to test server
     */
    public void sendStringToServer(String str) {
        clientConnection.sendString(str);
    }

    public void connectToClient() {
        clientConnection = new Client();
    }

    public void connectToClient(int port) {
        clientConnection = new Client(port);
    }

    public void initializePlayers() {
        for (int i = 0; i < 3; i++) {
            players[i] = new Player(" ");
        }
    }

    private CardCollection playableHand(CardCollection hand, int suit, int rank) {
        CardCollection playable = new CardCollection("playableHand");
        for( Card c : hand.cards) {
            if(c.getSuit() == suit) {
                playable.addCard(c);
            } else if(c.getRank() == rank) {
                playable.addCard(c);
            } else if(c.getRank() == 8) {
                playable.addCard(c);
            }
        }
        return playable;
    }

    public void startGame() {
        boolean gameCont = true;
        while (gameCont) {
            Card prevTop = null;
            Scanner sc = new Scanner(System.in);
            boolean roundCont = true;
            CardCollection hand = clientConnection.receiveHand();
            if(hand == null) {
                break;
            }
            System.out.println("Hand: " + hand);
            int cardsToDraw =0;
            int [] scoresheet = {0,0,0,0};
            boolean deckOver = false;
            while(roundCont) {
                Card top = clientConnection.receiveTop();
                if(top == null) {
                    roundCont = false;
                    break;
                }
                else {
                    System.out.println("Top of the Discard pile: " + top);
                    if(prevTop != null && prevTop.getRank() == 2 && prevTop == top) {
                        cardsToDraw = 0;
                    }
                }
                if(top.getRank() == 2) {
                    cardsToDraw+=2;
                } else {
                    cardsToDraw=0;
                }
                System.out.println("Direction: " + clientConnection.receiveString());
                boolean played2 = false;
                int turn = clientConnection.getTurn() + 1;
                if(turn == playerId) {
                    System.out.println("Your turn...");
                    CardCollection playable = playableHand(hand, top.getSuit(), top.getRank());
                    if(top.getRank() == 2 && cardsToDraw != 0) {
                        if(cardsToDraw == 4) {
                            for(int i=0;i<4;i++) {
                                clientConnection.sendString("D");
                                System.out.println("Drawing card...");
                                Card newCard = clientConnection.receiveTop();
                                hand.addCard(newCard);
                                System.out.println("Card drawn: " + newCard);
                            }
                            playable = playableHand(hand, top.getSuit(), top.getRank());
                        } else {
                            int i2 = 1000;
                            for(int k=0;k<hand.size();k++) {
                                if(hand.cards.get(k).getRank() == 2) {
                                    i2 = k;
                                    break;
                                }
                            }
                            if(i2 != 1000) {
                                System.out.println("Playing 2...");
                                Card playedCard = hand.popCard(i2);
                                clientConnection.sendString("A");
                                clientConnection.sendCard(playedCard);
                                System.out.println("Played card " + playedCard);
                                cardsToDraw = 0;
                                played2 = true;
                            }
                            else if(playable.size() >= 2) {
                                System.out.println(playable + " out of: " + hand);
                                System.out.println("You can play 2 cards or draw 2 cards...");
                                System.out.println("Type the 'P' to play, 'D' to draw");
                                String move = sc.next();
                                if(move.equals("P")) {
                                    clientConnection.sendString("AA");
                                    for(int j=0;j<2;j++) {
                                        Card playedCard = null;
                                        while(playedCard == null) {
                                            System.out.println("Type the symbol of the card you want to play...");
                                            String played = sc.next();
                                            for(int i = 0; i < hand.size(); i++) {
                                                Card myCard = hand.cards.get(i);
                                                if(myCard.toString().split("/")[1].substring(0, 2).equals(played)) {
                                                    playedCard = hand.popCard(i);
                                                    clientConnection.sendCard(playedCard);
                                                    System.out.println("Played card " + playedCard);
                                                    if(i==2 && playedCard.getRank() == 8) {
                                                        System.out.println("Select a suit to be played by typing the number after / : Clubs/0, Diamonds/1, Hearts/2, Spades/3");
                                                        int suit = sc.nextInt();
                                                        System.out.println("Selected " + Card.SUITS[suit] + "/" + suit);
                                                        clientConnection.sendSuit(suit);
                                                    }
                                                    break;
                                                }
                                            }
                                            if(playedCard == null) {
                                                System.out.println("Please play a valid card...\n" + playable);
                                            }
                                        }
                                    }
                                    played2 = true;
                                } else {
                                    for(int i=0;i<2;i++) {
                                        if(!deckOver)
                                        {
                                            clientConnection.sendString("D");
                                            System.out.println("Drawing card...");
                                            Card newCard = clientConnection.receiveTop();
                                            hand.addCard(newCard);
                                            if(newCard == null) {
                                                deckOver = true;
                                            }
                                            System.out.println("Card drawn: " + newCard);
                                        }
                                        else {
                                            clientConnection.sendString("P");
                                            break;
                                        }
                                    }
                                    playable = playableHand(hand, top.getSuit(), top.getRank());
                                }
                                cardsToDraw = -2;
                            }
                            else {
                                for(int i=0;i<2;i++) {
                                    if(!deckOver)
                                    {
                                        clientConnection.sendString("D");
                                        System.out.println("Drawing card...");
                                        Card newCard = clientConnection.receiveTop();
                                        hand.addCard(newCard);
                                        if(newCard == null) {
                                            deckOver = true;
                                        }
                                        System.out.println("Card drawn: " + newCard);
                                    }
                                    else {
                                        clientConnection.sendString("P");
                                        break;
                                    }
                                }
                                playable = playableHand(hand, top.getSuit(), top.getRank());
                                cardsToDraw = -2;
                            }
                        }
                    }
                    if(!played2) {
                        System.out.println(playable + " out of: " + hand);
                        int drawn = 0;
                        while(!deckOver && playable.size() == 0 && drawn < 3) {
                            System.out.println("Drawing card...");
                            clientConnection.sendString("D");
                            Card newCard = clientConnection.receiveTop();
                            if(newCard == null) {
                                deckOver = true;
                            }
                            hand.addCard(newCard);
                            System.out.println("Card drawn: " + newCard);
                            drawn ++;
                            playable = playableHand(hand, top.getSuit(), top.getRank());
                        }
                        if (playable.size() == 0) {
                            clientConnection.sendString("P");
                            System.out.println("Turn passed...");
                        } else {
                            if(drawn > 0) {
                                System.out.println(playable + " out of: " + hand);
                            }
                            Card playedCard = null;
                            while(playedCard == null) {
                                System.out.println("Type the symbol of the card you want to play or 'D' to draw...");
                                String played = sc.next();
                                while(!deckOver && played.equals("D") && drawn < 3) {
                                    System.out.println("Drawing card...");
                                    clientConnection.sendString("D");
                                    Card newCard = clientConnection.receiveTop();
                                    if(newCard == null) {
                                        deckOver = true;
                                    }
                                    hand.addCard(newCard);
                                    System.out.println("Card drawn: " + newCard);
                                    drawn ++;
                                    playable = playableHand(hand, top.getSuit(), top.getRank());
                                    System.out.println(playable+" out of "+hand);
                                    System.out.println("Type the symbol of the card you want to play or 'D' to draw...");
                                    played = sc.next();
                                }
                                for(int i = 0; i < hand.size(); i++) {
                                    Card myCard = hand.cards.get(i);
                                    if(myCard.toString().split("/")[1].substring(0, 2).equals(played) && playable.cards.contains(myCard)) {
                                        playedCard = hand.popCard(i);
                                        clientConnection.sendString("A");
                                        clientConnection.sendCard(playedCard);
                                        System.out.println("Played card " + playedCard);
                                        if(playedCard.getRank() == 8) {
                                            System.out.println("Select a suit to be played by typing the number after / : Clubs/0, Diamonds/1, Hearts/2, Spades/3");
                                            int suit = sc.nextInt();
                                            System.out.println("Selected " + Card.SUITS[suit] + "/" + suit);
                                            clientConnection.sendSuit(suit);
                                        }
                                        break;
                                    }
                                }
                                if(playedCard == null) {
                                    System.out.println("Please play a valid card...\n" + playable);
                                }
                            }
                        }
                    }
                } else {
                    System.out.println("Player " + turn + "'s turn...");
                }
                prevTop = top;
            }
            System.out.println("Round over...\nScores: " + clientConnection.receiveScores());
        }
        System.out.println("Game Over...");
    }


    private class Client {
        Socket socket;
        private ObjectInputStream dIn;
        private ObjectOutputStream dOut;

        public Client() {
            try {
                socket = new Socket("localhost", 3333);
                dOut = new ObjectOutputStream(socket.getOutputStream());
                dIn = new ObjectInputStream(socket.getInputStream());

                playerId = dIn.readInt();

                System.out.println("Connected as " + playerId);
                sendPlayer();

            } catch (IOException ex) {
                System.out.println("Client failed to open");
            }
        }

        public Client(int portId) {
            try {
                socket = new Socket("localhost", portId);
                dOut = new ObjectOutputStream(socket.getOutputStream());
                dIn = new ObjectInputStream(socket.getInputStream());

                playerId = dIn.readInt();

                System.out.println("Connected as " + playerId);
                sendPlayer();

            } catch (IOException ex) {
                System.out.println("Client failed to open");
            }
        }

        public ArrayList<Integer> receiveScores() {
            try{
                return (ArrayList<Integer>) dIn.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
            return null;
        }

        public void sendSuit(int suit) {
            try {
                dOut.writeInt(suit);
                dOut.flush();
            } catch (IOException ex) {
                System.out.println("Suit not sent");
                ex.printStackTrace();
            }
        }

        public void sendPlayer() {
            try {
                dOut.writeObject(getPlayer());
                dOut.flush();
            } catch (IOException ex) {
                System.out.println("Player not sent");
                ex.printStackTrace();
            }
        }

        public void sendString(String str) {
            try {
                dOut.writeUTF(str);
                dOut.flush();
            } catch (IOException ex) {
                System.out.println("Player not sent");
                ex.printStackTrace();
            }
        }

        public String receiveString() {
            try {
                return dIn.readUTF();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return "";
        }

        public void sendCard(Card card) {
            try {
                dOut.writeObject(card);
                dOut.flush();
            } catch (IOException ex) {
                System.out.println("Player not sent");
                ex.printStackTrace();
            }
        }


        public int getTurn() {
            try{
                return dIn.readInt();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return 1000;
        }

        public CardCollection receiveHand() {
            try {
                return (CardCollection) dIn.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
            return null;
        }

        public Card receiveTop() {
            try {
                return (Card) dIn.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
            return null;
        }

    }

    /*
     * ---------Constructor and Main class-----------
     */

    /*
     * constructor takes the name of the player and sets the score to 0
     */
    public Player(String n) {
        name = n;
    }

    public static void main(String args[]) {
        Scanner myObj = new Scanner(System.in);
        System.out.print("What is your name ? ");
        String name = myObj.next();
        Player p = new Player(name);
        p.initializePlayers();
        p.connectToClient();
        p.startGame();
        //p.returnWinner();
        myObj.close();
    }
}