package com.funintended.morsepals;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dustin on 1/21/14. :)
 */
public class GameState {

    @SerializedName("current_word")
    private String mCurrentWord;

    @SerializedName("players")
    private List<Player> mPlayers;

    @SerializedName("current_round")
    private int mCurrentRound;

    @SerializedName("current_player")
    private Player mCurrentPlayer;

    private transient boolean mFresh;

    public GameState init() {
        mPlayers = new ArrayList<Player>();
        mCurrentRound = 0;
        mFresh = true;
        return this;
    }

    public String getCurrentWord() {
        return mCurrentWord;
    }

    public void setCurrentWord(String currentWord) {
        mCurrentWord = currentWord;
    }

    public List<Player> getPlayers() {
        return mPlayers;
    }

    public Player getPlayer(String playerId) {
        for (Player player : mPlayers) {
            if (player.getPlayerId().equals(playerId)) {
                return player;
            }
        }
        return null;
    }

    public void setPlayers(List<Player> players) {
        mPlayers = players;
    }

    public void addPlayer(Player player) {
        mPlayers.add(player);
    }

    public int getCurrentRound() {
        return mCurrentRound;
    }

    public void setCurrentRound(int currentRound) {
        mCurrentRound = currentRound;
    }

    /**
     * fails the current player for this round and clears the challenge word
     *
     * @param playerId current player Id (participant ID)
     * @param success  whether or not the current player passed the round
     */
    public void takeTurn(String playerId, boolean success) {
        if (!success) {
            mCurrentWord = null;//this will pick a new word for the next player
            for (Player player : mPlayers) {//there are only a max of 8 players
                if (player.mPlayerId.equals(playerId)) {
                    player.fail();
                }
            }
        }
    }

    public void setPlayerIds(List<String> playerIds) {
        if (mPlayers == null) {
            mPlayers = new ArrayList<Player>();
        }
        for (String playerId : playerIds) {
            Player player = new Player(playerId);
            if (!mPlayers.contains(player)) {
                mPlayers.add(player);
            }
        }
    }

    public boolean isFresh() {
        return mFresh;
    }

    public Player getNextPlayer() {
        int nextIndex = mPlayers.indexOf(mCurrentPlayer) + 1;
        Player next = mPlayers.get(nextIndex % mPlayers.size());
        if (next == mCurrentPlayer) {
            return null;
        }
        return next;
    }

    public Player getCurrentPlayer() {
        return mCurrentPlayer;
    }

    public void setCurrentPlayer(String participantId) {
        for (Player player : mPlayers) {
            if (player.getPlayerId().equals(participantId)) {
                setCurrentPlayer(player);
            }
        }
    }

    public void setCurrentPlayer(Player currentPlayer) {
        mCurrentPlayer = currentPlayer;
    }

    /**
     * a game is over when there is just one player left. everyone else MORSE-ed out
     *
     * @return
     */
    public boolean isGameOver() {
        int activePlayerCount = 0;
        for (Player player : mPlayers) {
            if (!player.isOut()) {
                activePlayerCount++;
            }
        }
        if (activePlayerCount <= 1) {
            return true;
        }
        return false;
    }

    public boolean playerDidWin(String participantId) {
        for (Player player : mPlayers) {
            if (player.mPlayerId.equals(participantId)) {
                return !player.isOut();
            }
        }
        return false;
    }

    public static class Player {
        @SerializedName("player_id")
        private String mPlayerId;

        @SerializedName("words_failed")
        private int mWordsFailed;

        public Player(String playerId) {
            mPlayerId = playerId;
        }

        public String getPlayerId() {
            return mPlayerId;
        }

        public void setPlayerId(String playerId) {
            mPlayerId = playerId;
        }

        public int getWordsFailed() {
            return mWordsFailed;
        }

        public void setWordsFailed(int wordsFailed) {
            mWordsFailed = wordsFailed;
        }

        public void fail() {
            mWordsFailed++;
        }

        public boolean isOut() {
            if (mWordsFailed >= 5) {//Spells MORSE
                return true;
            }
            return false;
        }

        @Override
        public boolean equals(Object o) {
            return ((Player) o).mPlayerId.equals(mPlayerId);
        }

        private static final String[] MORSE = new String[]{"M", "O", "R", "S", "E"};

        public String getMORSEScore() {
            String out = "";
            for (int i = 0; i < MORSE.length; i++) {
                if (i < mWordsFailed) {
                    out += MORSE[i];
                } else {
                    out += " _ ";
                }
                if (i < mWordsFailed - 1) {
                    out += ".";
                }
            }
            return out;
        }
    }
}
