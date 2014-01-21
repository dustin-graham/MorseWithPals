package com.funintended.morsepals;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.OnInvitationReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.turnbased.OnTurnBasedMatchCanceledListener;
import com.google.android.gms.games.multiplayer.turnbased.OnTurnBasedMatchInitiatedListener;
import com.google.android.gms.games.multiplayer.turnbased.OnTurnBasedMatchUpdateReceivedListener;
import com.google.android.gms.games.multiplayer.turnbased.OnTurnBasedMatchUpdatedListener;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchConfig;
import com.google.example.games.basegameutils.BaseGameActivity;
import com.google.gson.Gson;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class MainActivity extends BaseGameActivity implements OnTurnBasedMatchInitiatedListener, OnInvitationReceivedListener, OnTurnBasedMatchUpdateReceivedListener, OnTurnBasedMatchCanceledListener, OnTurnBasedMatchUpdatedListener {

    private static final int SELECT_PLAYERS = 100;
    private static final int CHECK_GAMES = 101;
    private static final int PLAY_ROUND = 102;
    private static final String TAG = "MainActivity";

    @InjectView(R.id.start_match_button)
    Button mStartMatchButton;

    @InjectView(R.id.check_games_button)
    Button mCheckGamesButton;

    private AlertDialog mAlertDialog;
    private boolean isDoingTurn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);
    }

    @Override
    protected void onActivityResult(int request, int response, Intent data) {
        super.onActivityResult(request, response, data);

        if (request == SELECT_PLAYERS) {
            // Returned from 'Select players to Invite' dialog

            if (response != Activity.RESULT_OK) {
                // user canceled
                return;
            }

            // get the invitee list
            final ArrayList<String> invitees = data
                    .getStringArrayListExtra(GamesClient.EXTRA_PLAYERS);

            // get automatch criteria
            Bundle autoMatchCriteria = null;

            int minAutoMatchPlayers = data.getIntExtra(
                    GamesClient.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
            int maxAutoMatchPlayers = data.getIntExtra(
                    GamesClient.EXTRA_MAX_AUTOMATCH_PLAYERS, 0);

            if (minAutoMatchPlayers > 0) {
                autoMatchCriteria = RoomConfig.createAutoMatchCriteria(
                        minAutoMatchPlayers, maxAutoMatchPlayers, 0);
            } else {
                autoMatchCriteria = null;
            }

            TurnBasedMatchConfig tbmc = TurnBasedMatchConfig.builder()
                    .addInvitedPlayers(invitees)
                    .setAutoMatchCriteria(autoMatchCriteria).build();

            // Kick the match off
            getGamesClient().createTurnBasedMatch(this, tbmc);
        } else if (request == CHECK_GAMES) {
            // Returning from the 'Select Match' dialog

            if (response != Activity.RESULT_OK) {
                // user canceled
                return;
            }

            TurnBasedMatch match = data
                    .getParcelableExtra(GamesClient.EXTRA_TURN_BASED_MATCH);

            if (match != null) {
                updateMatch(match);
            }

            Log.d(TAG, "Match = " + match);
        } else if (request == PLAY_ROUND) {
            if (response == GameActivity.RESULT_GAME_FINISHED) {
                TurnBasedMatch match = data.getParcelableExtra(GameActivity.TURN_BASED_MATCH_KEY);
                directUserToPlayGamesGUI("This game is now finished. Click 'View' to see results");
            }
        }
    }

    @OnClick(R.id.start_match_button)
    void onStartMatch() {
        Intent intent = getGamesClient()
                .getSelectPlayersIntent(1, 7, true);
        startActivityForResult(intent, SELECT_PLAYERS);

    }

    @OnClick(R.id.check_games_button)
    void checkGames() {
        Intent intent = getGamesClient().getMatchInboxIntent();
        startActivityForResult(intent, CHECK_GAMES);
    }

    @Override
    public void onSignInFailed() {

    }

    @Override
    public void onSignInSucceeded() {
        if (mHelper.getTurnBasedMatch() != null) {
            // GameHelper will cache any connection hint it gets. In this case,
            // it can cache a TurnBasedMatch that it got from choosing a turn-based
            // game notification. If that's the case, you should go straight into
            // the game.
            updateMatch(mHelper.getTurnBasedMatch());
            return;
        }

        // As a demonstration, we are registering this activity as a handler for
        // invitation and match events.

        // This is *NOT* required; if you do not register a handler for
        // invitation events, you will get standard notifications instead.
        // Standard notifications may be preferable behavior in many cases.
        getGamesClient().registerInvitationListener(this);

        // Likewise, we are registering the optional MatchUpdateListener, which
        // will replace notifications you would get otherwise. You do *NOT* have
        // to register a MatchUpdateListener.
        getGamesClient().registerMatchUpdateListener(this);
    }

    // This is the main function that gets called when players choose a match
    // from the inbox, or else create a match and want to start it.
    public void updateMatch(TurnBasedMatch match) {
        int status = match.getStatus();
        int turnStatus = match.getTurnStatus();

        switch (status) {
            case TurnBasedMatch.MATCH_STATUS_CANCELED:
                showWarning("Canceled!", "This game was canceled!");
                return;
            case TurnBasedMatch.MATCH_STATUS_EXPIRED:
                showWarning("Expired!", "This game is expired.  So sad!");
                return;
            case TurnBasedMatch.MATCH_STATUS_AUTO_MATCHING:
                showWarning("Waiting for auto-match...",
                        "We're still waiting for an automatch partner.");
                return;
            case TurnBasedMatch.MATCH_STATUS_COMPLETE:
                directUserToPlayGamesGUI("This game is complete. Click 'View' to see results");
                return;
        }

        // OK, it's active. Check on turn status.
        switch (turnStatus) {
            case TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN:
                launchRound(match);
                return;
            case TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN:
                // Should return results.
                showWarning("Alas...", "It's not your turn.");
                break;
            case TurnBasedMatch.MATCH_TURN_STATUS_INVITED:
                showWarning("Good inititative!",
                        "Still waiting for invitations.\n\nBe patient!");
        }

    }

    private void showGameSummary(TurnBasedMatch match) {
        GameState finalGameState = new Gson().fromJson(new String(match.getData()), GameState.class);
        String thisPlayerId = match.getParticipantId(getGamesClient().getCurrentPlayerId());
        boolean playerDidWin = finalGameState.playerDidWin(thisPlayerId);
        String resultMessage = "";
        if (playerDidWin) {
            resultMessage = getString(R.string.congratulations, finalGameState.getPlayer(thisPlayerId).getMORSEScore());
        } else {
            resultMessage = getString(R.string.consolation);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Match Results");
        builder.setMessage(resultMessage);
        builder.create().show();
    }

    // Generic warning/info dialog
    public void showWarning(String title, String message) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set title
        alertDialogBuilder.setTitle(title).setMessage(message);

        // set dialog message
        alertDialogBuilder.setCancelable(false).setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // if this button is clicked, close
                        // current activity
                    }
                });

        // create alert dialog
        mAlertDialog = alertDialogBuilder.create();

        // show it
        mAlertDialog.show();
    }

    @Override
    public void onTurnBasedMatchInitiated(int statusCode, TurnBasedMatch match) {
        //start the game activity and pass the match in
        if (!checkStatusCode(match, statusCode)) {
            return;
        }

        if (match.getData() != null) {
            // This is a game that has already started, so I'll just start
            updateMatch(match);
            return;
        }

        launchRound(match);
    }

    private void launchRound(TurnBasedMatch match) {
        Intent gameIntent = new Intent(this, GameActivity.class);
        gameIntent.putExtra(GameActivity.TURN_BASED_MATCH_KEY, match);
        startActivityForResult(gameIntent, PLAY_ROUND);
    }

    @Override
    public void onInvitationReceived(Invitation invitation) {
        directUserToPlayGamesGUI("Invitation Received");
    }

    private void directUserToPlayGamesGUI(String alertTitle) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(alertTitle).setPositiveButton("View", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                checkGames();
            }
        }).setNegativeButton("Ignore", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.create().show();
    }

    @Override
    public void onInvitationRemoved(String s) {
        Toast.makeText(this, "An invitation was removed.", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onTurnBasedMatchReceived(TurnBasedMatch match) {
        int status = match.getStatus();
        int turnStatus = match.getTurnStatus();
        if (status != TurnBasedMatch.MATCH_STATUS_CANCELED &&
                status != TurnBasedMatch.MATCH_STATUS_EXPIRED &&
                status != TurnBasedMatch.MATCH_STATUS_AUTO_MATCHING &&
                status != TurnBasedMatch.MATCH_STATUS_COMPLETE) {
            if (turnStatus == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN) {
                directUserToPlayGamesGUI("Received Match Update. It's your turn!");
            } else if (turnStatus == TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN) {
                Toast.makeText(this, "Next Player's turn. We'll notify you when it's your turn again.", Toast.LENGTH_LONG).show();
            } else if (turnStatus == TurnBasedMatch.MATCH_TURN_STATUS_INVITED) {
                Toast.makeText(this, "Player invitations have been sent", Toast.LENGTH_LONG).show();
            }
        } else if (status == TurnBasedMatch.MATCH_STATUS_COMPLETE) {
            directUserToPlayGamesGUI("You have a game that just finished. Click 'View' to see the results");
        }

    }

    @Override
    public void onTurnBasedMatchRemoved(String matchId) {
        Toast.makeText(this, "A match was removed.", Toast.LENGTH_LONG).show();

    }

    @Override
    public void onTurnBasedMatchCanceled(int statusCode, String matchId) {

        if (!checkStatusCode(null, statusCode)) {
            return;
        }

        isDoingTurn = false;

        showWarning("Match",
                "This match is canceled.  All other players will have their game ended.");
    }

    public void showErrorMessage(TurnBasedMatch match, int statusCode,
                                 int stringId) {

        showWarning("Warning", getResources().getString(stringId));
    }

    // Returns false if something went wrong, probably. This should handle
    // more cases, and probably report more accurate results.
    private boolean checkStatusCode(TurnBasedMatch match, int statusCode) {
        switch (statusCode) {
            case GamesClient.STATUS_OK:
                return true;
            case GamesClient.STATUS_NETWORK_ERROR_OPERATION_DEFERRED:
                // This is OK; the action is stored by Google Play Services and will
                // be dealt with later.
                Toast.makeText(
                        this,
                        "Stored action for later.  (Please remove this toast before release.)",
                        Toast.LENGTH_LONG).show();
                // NOTE: This toast is for informative reasons only; please remove
                // it from your final application.
                return true;
            case GamesClient.STATUS_MULTIPLAYER_ERROR_NOT_TRUSTED_TESTER:
                showErrorMessage(match, statusCode,
                        R.string.status_multiplayer_error_not_trusted_tester);
                break;
            case GamesClient.STATUS_MATCH_ERROR_ALREADY_REMATCHED:
                showErrorMessage(match, statusCode,
                        R.string.match_error_already_rematched);
                break;
            case GamesClient.STATUS_NETWORK_ERROR_OPERATION_FAILED:
                showErrorMessage(match, statusCode,
                        R.string.network_error_operation_failed);
                break;
            case GamesClient.STATUS_CLIENT_RECONNECT_REQUIRED:
                showErrorMessage(match, statusCode,
                        R.string.client_reconnect_required);
                break;
            case GamesClient.STATUS_INTERNAL_ERROR:
                showErrorMessage(match, statusCode, R.string.internal_error);
                break;
            case GamesClient.STATUS_MATCH_ERROR_INACTIVE_MATCH:
                showErrorMessage(match, statusCode,
                        R.string.match_error_inactive_match);
                break;
            case GamesClient.STATUS_MATCH_ERROR_LOCALLY_MODIFIED:
                showErrorMessage(match, statusCode,
                        R.string.match_error_locally_modified);
                break;
            default:
                showErrorMessage(match, statusCode, R.string.unexpected_status);
                Log.d(TAG, "Did not have warning or string to deal with: "
                        + statusCode);
        }

        return false;
    }

    @Override
    public void onTurnBasedMatchUpdated(int i, TurnBasedMatch turnBasedMatch) {
        Toast.makeText(this, "you finished this match!", Toast.LENGTH_LONG).show();
    }
}
