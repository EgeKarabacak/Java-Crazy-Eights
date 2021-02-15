package com.ege;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class GameServer implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    Server[] playerServer = new Server[4];
    Player[] players = new Player[4];

    ServerSocket ss;

    private Deck gameDeck;
    int numPlayers;

    public static void main(String args[]) throws Exception {
        GameServer sr = new GameServer();

        sr.acceptConnections();
        sr.gameLoop();
    }

    public GameServer() {
        System.out.println("Starting game server");
        numPlayers = 0;
        // initialize the players list with new players
        for (int i = 0; i < players.length; i++) {
            players[i] = new Player(" ");
        }

        try {
            ss = new ServerSocket(3333);
        } catch (IOException ex) {
            System.out.println("Server Failed to open");
        }

    }

    /*
     * -----------Networking stuff ----------
     *
     */
    public void acceptConnections() throws ClassNotFoundException {
        numPlayers = 0;
        try {
            System.out.println("Waiting for players...");
            while (numPlayers <4) {
                Socket s = ss.accept();
                numPlayers++;

                Server server = new Server(s, numPlayers);

                // send the player number
                server.dOut.writeInt(server.playerId);
                server.dOut.flush();

                // get the player name
                Player in = (Player) server.dIn.readObject();
                System.out.println("Player " + server.playerId + " ~ " + in.name + " ~ has joined");
                // add the player to the player list
                players[server.playerId - 1] = in;
                playerServer[numPlayers - 1] = server;
            }
            System.out.println("Four players have joined the game");

            // start the server threads
            for (Server server : playerServer) {
                Thread t = new Thread(server);
                t.start();
            }
            // start their threads
        } catch (IOException ex) {
            System.out.println("Could not connect 3 players");
        }
    }

    private int calcScore(CardCollection hand) {
        int result = 0;
        for(Card card : hand.cards) {
            if(card.getRank() == 8) {
                result+=50;
            }
            else if(card.getRank() > 10) {
                result+=10;
            }
            else {
                result+=card.getRank();
            }
        }
        return result;
    }

    public void gameLoop() {
        ArrayList<Integer> scoresheet = new ArrayList<>(Collections.nCopies(4, 0));
        boolean gameContinues = true;
        int round = 0;
        while(gameContinues) {
            gameDeck = new Deck("deck");
            System.out.println(gameDeck);
            ArrayList<CardCollection> hands = new ArrayList<CardCollection>();
            for(int i=0;i<4;i++) {
                CardCollection hand = new CardCollection(players[i].name + "'s hand");
                gameDeck.deal(hand, 5);
                hands.add(hand);
                playerServer[i].sendHand(hand);
            }
            System.out.println(hands);
            CardCollection discardPile = new CardCollection("discard pile");
            gameDeck.deal(discardPile,1);
            while(discardPile.last().getRank() == 8) {
                gameDeck.addCard(discardPile.popCard());
                gameDeck.shuffle();
                gameDeck.deal(discardPile,1);
            }
            System.out.println("Deck size: " + gameDeck.size());
            System.out.println("Hands: " + hands);
            boolean roundCont = true;
            boolean direction = true;
            boolean deckOver = false;
            int playaTurn = 0;
            int passCounter = 0;
            while (roundCont){
                System.out.println("Top of discard pile: " + discardPile.last());
                String currentDir = direction ? "left" : "right";
                for( Server ps: playerServer) {
                    ps.sendTop(discardPile.last());
                    ps.sendString(currentDir);
                }
                System.out.println("Game direction " + currentDir);
                int turn = (round + playaTurn) % 4;
                for(Server ps : playerServer) {
                    ps.notifyTurn(turn);
                }
                String move = playerServer[turn].receiveString();
                System.out.println(move);
                if(discardPile.last().getRank() == 2 && move.equals("AA")) {
                    for(int j=0;j<2;j++) {
                        Card received = playerServer[turn].receiveCard();
                        System.out.println(received.toString() + " played...");
                        discardPile.addCard(received);
                        for(int i=0;i<hands.get(turn).size();i++) {
                            if(received.toString().equals(hands.get(turn).cards.get(i).toString())) {
                                Card discarded = hands.get(turn).popCard(i);
                            }
                        }
                        if(hands.get(turn).size() == 0) {
                            roundCont = false;
                            for(Server s : playerServer) {
                                s.sendTop((Card) null);
                            }
                        }
                        else {
                            if(j==1) {
                                if(received.getRank() == 1) {//Ace
                                    direction = !direction;
                                }
                                else if(received.getRank() == 12){//Queen
                                    if (direction) {
                                        playaTurn++;
                                    } else {
                                        playaTurn--;
                                    }
                                }
                                else if(received.getRank() == 8) {//8
                                    int suit = playerServer[turn].recieveSuit();
                                    Card topik = new Card(8,suit);
                                    discardPile.addCard(topik);
                                }
                            }
                        }
                    }
                    if (direction) {
                        playaTurn++;
                    } else {
                        playaTurn--;
                    }
                }
                while(move.equals("D")) {
                    if(!deckOver) {
                        Card sentCard = gameDeck.popCard();
                        playerServer[turn].sendTop(sentCard);
                        System.out.println("Deck size: " + gameDeck.size() + " Card sent: " + sentCard);
                        hands.get(turn).addCard(sentCard);
                    }
                    else {
                        Card sentCard = (Card) null;
                        playerServer[turn].sendTop(sentCard);
                        System.out.println("Deck size: " + gameDeck.size());
                    }
                    if(gameDeck.size() == 0) {
                        deckOver = true;
                    }
                    move = playerServer[turn].receiveString();
                }
                if (move.equals("P")) {
                    passCounter++;
                    if (direction) {
                        playaTurn++;
                    } else {
                        playaTurn--;
                    }
                } else if(move.equals("A")){
                    passCounter = 0;
                    Card received = playerServer[turn].receiveCard();
                    discardPile.addCard(received);
                    for(int i=0;i<hands.get(turn).size();i++) {
                        if(received.toString().equals(hands.get(turn).cards.get(i).toString())) {
                            Card discarded = hands.get(turn).popCard(i);
                        }
                    }
                    if(hands.get(turn).size() == 0) {
                        roundCont = false;
                        for(Server s : playerServer) {
                            s.sendTop((Card) null);
                        }
                    }
                    else {
                        if(received.getRank() == 1) {//Ace
                            direction = !direction;
                        }
                        else if(received.getRank() == 12){//Queen
                            if (direction) {
                                playaTurn++;
                            } else {
                                playaTurn--;
                            }
                        }
                        else if(received.getRank() == 8) {//8
                            int suit = playerServer[turn].recieveSuit();
                            Card topik = new Card(8,suit);
                            discardPile.addCard(topik);
                        }
                        if (direction) {
                            playaTurn++;
                        } else {
                            playaTurn--;
                        }
                    }
                }
                playaTurn = (playaTurn + 4) % 4;
                if(passCounter == 4) {
                    roundCont = false;
                    for(Server ps : playerServer) {
                        ps.sendTop((Card) null);
                    }
                }
            }
            ArrayList<Integer> newScoresheet = new ArrayList<>();
            for(int i=0;i<hands.size();i++) {
                int score = calcScore(hands.get(i));
                newScoresheet.add(i, scoresheet.get(i) + score);
                if(newScoresheet.get(i) >= 100) {
                    gameContinues = false;
                }
            }
            for(Server ps : playerServer) {
                ps.sendScores(newScoresheet);
            }
            System.out.println("Round over...\nScores: "+newScoresheet);
            scoresheet = newScoresheet;
            round++;
        }
        int winner = 1000, minscore=1000;
        for(int i=0;i<4;i++) {
            if(scoresheet.get(i) < minscore) {
                winner = i;
            }
        }
        System.out.println(scoresheet);
        System.out.println("The winner is " + players[winner].name);
        for(Server ps : playerServer) {
            ps.sendHand((CardCollection) null);
        }
    }



    public class Server implements Runnable {
        private Socket socket;
        private ObjectInputStream dIn;
        private ObjectOutputStream dOut;
        private final int playerId;

        public Server(Socket s, int playerid) {
            socket = s;
            playerId = playerid;
            try {
                dOut = new ObjectOutputStream(socket.getOutputStream());
                dIn = new ObjectInputStream(socket.getInputStream());
            } catch (IOException ex) {
                System.out.println("Server Connection failed");
            }
        }

        /*
         * run function for threads --> main body of the thread will start here
         */
        public void run() {
            try {
                while (true) {
                }

            } catch (Exception ex) {
                {
                    System.out.println("Run failed");
                    ex.printStackTrace();
                }
            }
        }

        /*
         * send the scores to other players
         */
        public void sendPlayers(Player[] pl) {
            try {
                for (Player p : pl) {
                    dOut.writeObject(p);
                    dOut.flush();
                }

            } catch (IOException ex) {
                System.out.println("Score sheet not sent");
                ex.printStackTrace();
            }

        }
        public void sendHand(CardCollection hand) {
            try {
                dOut.writeObject(hand);
                dOut.flush();
            } catch (Exception e) {
                System.out.println("Hand not received by player " + playerId);
                e.printStackTrace();
            }
        }
        public void sendString(String str) {
            try {
                dOut.writeUTF(str);
                dOut.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        public void sendTop(Card top) {
            try {
                dOut.writeObject(top);
                dOut.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void sendScores(ArrayList<Integer> scores) {
            try {
                dOut.writeObject(scores);
                dOut.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void notifyTurn(int turn) {
            try {
                dOut.writeInt(turn);
                dOut.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        public String receiveString() {
            try{
                return dIn.readUTF();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return "";
        }

        public Card receiveCard() {
            try{
                return (Card) dIn.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
            return null;
        }

        public int recieveSuit(){
            try{
                return dIn.readInt();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return 100;
        }
    }

}