package com.funintended.morsepals;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.games.multiplayer.turnbased.OnTurnBasedMatchUpdatedListener;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.example.games.basegameutils.BaseGameActivity;
import com.google.gson.Gson;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;

/**
 * Created by dustin on 1/17/14. :)
 */
public class GameActivity extends BaseGameActivity implements OnTurnBasedMatchUpdatedListener, MorseGameHost {

    public static final int RESULT_GAME_FINISHED = 1000;

    public static final String TURN_BASED_MATCH_KEY = "GameActivity:TurnBasedMatch";

    private static final String HELP_DIALOG_TAG = "GameActivity:HelpDialog";
    private static final String PRE_GAME_TAG = "GameActivity:PreGame";
    private static final String GAME_TAG = "GameActivity:Game";
    private TurnBasedMatch mMatch;
    private ArrayList<String> mParticipantIds;
    private String mCurrentParticipantid;
    private boolean mIsSoloGame;

    private int mRoundSeconds = 5;

    private GameState mGameState;
    private Gson mGson = new Gson();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_game);

        if (getIntent().hasExtra(TURN_BASED_MATCH_KEY) && (mMatch = getIntent().getParcelableExtra(TURN_BASED_MATCH_KEY)) != null) {
            mParticipantIds = mMatch.getParticipantIds();
            mCurrentParticipantid = mMatch.getPendingParticipantId();
            mIsSoloGame = false;
            if (mMatch.getData() != null) {
                try {
                    String jsonMatchData = new String(mMatch.getData());
                    Gson gson = new Gson();
                    mGameState = gson.fromJson(jsonMatchData, GameState.class);
                } catch (com.google.gson.JsonSyntaxException e) {
                    //bail!
                    finish();
                }
            } else {
                mGameState = new GameState().init();
                mGameState.setPlayerIds(mParticipantIds);
            }
            mGameState.setCurrentPlayer(mCurrentParticipantid);
        } else {
            mIsSoloGame = true;
        }


    }

    @Override
    protected void onResume() {
        super.onResume();
        BusProvider.getInstance().register(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        BusProvider.getInstance().unregister(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_help:
                MorseHelpDialog dialog = new MorseHelpDialog();
                dialog.show(getSupportFragmentManager(), HELP_DIALOG_TAG);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSignInFailed() {
        if (!mIsSoloGame) {
            Toast.makeText(this, "Something went wrong with Play Games Sign In. Try again later", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onSignInSucceeded() {
        PreGameFragment preGame = PreGameFragment.create(mRoundSeconds);
        getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.fragment_container, preGame, PRE_GAME_TAG).commit();
    }

    @Subscribe
    public void onPlayerReady(PreGameFragment.OnMatchReadyEvent event) {
        //move the game challenge fragment on
        String challengeString = mGameState.getCurrentWord();
        if (challengeString == null) {
            String[] letters = getResources().getStringArray(R.array.morse_letters);
            int randomIndex = (int) (Math.random() * letters.length);
            challengeString = letters[randomIndex];
            mGameState.setCurrentWord(challengeString);
        }
        GameChallengeFragment frag = GameChallengeFragment.create(challengeString, mRoundSeconds);
        getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.fragment_container, frag, GAME_TAG).setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left).commit();
    }

    @Subscribe
    public void onHelpDialogDismissed(MorseHelpDialog.HelpDialogDismissedEvent event) {

    }

    @Subscribe
    public void onRoundFinished(GameChallengeFragment.RoundFinishedEvent event) {
        //round is finished, show the dialog and take the turn

        takeTurn(event.isSuccess());
        String modalMessage = "";
        if (event.isSuccess()) {
            modalMessage = getString(R.string.success_message);
        } else {
            modalMessage = getString(R.string.failure_message) + getResources().getString(R.string.current_score, mGameState.getCurrentPlayer().getMORSEScore());
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(event.isSuccess() ? getString(R.string.success_title) : getString(R.string.failure_title));

        builder.setMessage(modalMessage);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        }).setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                finish();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void takeTurn(boolean success) {
        mGameState.takeTurn(mCurrentParticipantid, success);
        if (mGameState.isGameOver()) {
            //finish the game
            getGamesClient().finishTurnBasedMatch(this, mMatch.getMatchId(), mGson.toJson(mGameState, GameState.class).getBytes());
            Intent intent = new Intent();
            intent.putExtra(TURN_BASED_MATCH_KEY, mMatch);
            setResult(RESULT_GAME_FINISHED, intent);
        } else {
            String nextPlayerId = mGameState.getNextPlayer().getPlayerId();
            getGamesClient().takeTurn(this, mMatch.getMatchId(), mGson.toJson(mGameState, GameState.class).getBytes(), nextPlayerId);
        }
    }

    @Override
    public void onTurnBasedMatchUpdated(int i, TurnBasedMatch turnBasedMatch) {

    }

    @Override
    public GameState.Player getCurrentPlayer() {
        if (mGameState != null) {
            return mGameState.getCurrentPlayer();
        } else {
            return null;
        }
    }
}
