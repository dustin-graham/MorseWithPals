package com.funintended.morsepals;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

/**
 * Created by dustin on 1/20/14. :)
 */
public class GameChallengeFragment extends Fragment implements CountdownTimer.CountdownTimerListener {

    private static final String TOTAL_SECONDS_ARG = "GameChallengeFragment:TotalSeconds";
    private static final String ENGLISH_WORD_ARG = "GameChallengeFragment:EnglishWord";

    @InjectView(R.id.match_challenge_label)
    TextView mMatchChallengeLabel;//the label that shows how much time is left

    @InjectView(R.id.challenge_content)
    TextView mChallengeContent;//the text view that shows the user what they need to translate

    @InjectView(R.id.morse_output)
    TextView mMorseOutput;//the text view that shows what is currently typed by the user

    @InjectView(R.id.backspace_button)
    Button mBackspaceButton;

    @InjectView(R.id.dit_button)
    Button mDitButton;

    @InjectView(R.id.dah_button)
    Button mDahButton;

    @InjectView(R.id.char_space_button)
    Button mCharSpaceButton;

    @InjectView(R.id.word_space_button)
    Button mWordSpaceButton;

    private long mStartTimestamp = 0;
    private int mTotalSeconds = 0;
    private String mEnglishWord = "";
    private String mTargetEnglishWordPattern = "";

    private static final String DIT = ".";
    private static final String DAH = "-";
    private static final String CHAR_SPACE = ",";
    private static final String WORD_SPACE = ";";

    private String mMorseOutputString = "";

    private CountdownTimer mTimer;

    private boolean mIsRoundFinished = false;

    public static GameChallengeFragment create(String word, int seconds) {
        GameChallengeFragment frag = new GameChallengeFragment();
        Bundle args = new Bundle();
        args.putInt(TOTAL_SECONDS_ARG, seconds);
        args.putString(ENGLISH_WORD_ARG, word);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTotalSeconds = getArguments().getInt(TOTAL_SECONDS_ARG);
        mEnglishWord = getArguments().getString(ENGLISH_WORD_ARG);
        mTargetEnglishWordPattern = MorseCodeConverter.stringPhrasePattern(mEnglishWord);
        mTimer = new CountdownTimer(mTotalSeconds, this);
        mIsRoundFinished = false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_game_prompt, container, false);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.inject(this, view);

        mChallengeContent.setText(mEnglishWord);
        updateTimeLeft(mTotalSeconds);
        mTimer.start();
    }

    private void updateTimeLeft(final int secondsRemaining) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String challengeText = getResources().getString(R.string.time_left_label, secondsRemaining);
                mMatchChallengeLabel.setText(challengeText);
            }
        });
    }

    private boolean checkPhrasePattern() {
        return mTargetEnglishWordPattern.equals(mMorseOutputString);
    }

    private void checkForSuccess() {
        checkForSuccess(false);
    }

    private void checkForSuccess(boolean forceCorrect) {
        if (checkPhrasePattern() || forceCorrect) {
            //success
            finishRound(true);
        }
    }

    private void finishRound(final boolean success) {
        mTimer.pause();
        mIsRoundFinished = true;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                BusProvider.getInstance().post(new RoundFinishedEvent(success));
            }
        });
    }

    @OnClick(R.id.backspace_button)
    void onBackspaceClicked() {
        if (mIsRoundFinished) return;
        mMorseOutputString = mMorseOutputString.substring(0, mMorseOutputString.length() - 1);
        mMorseOutput.setText(mMorseOutputString);
        checkForSuccess();
    }

    @OnClick(R.id.dit_button)
    void onDitClicked() {
        if (mIsRoundFinished) return;
        mMorseOutputString += DIT;
        mMorseOutput.setText(mMorseOutputString);
        checkForSuccess(true);
    }

    @OnClick(R.id.dah_button)
    void onDahClicked() {
        if (mIsRoundFinished) return;
        mMorseOutputString += DAH;
        mMorseOutput.setText(mMorseOutputString);
        checkForSuccess();
    }

    @OnClick(R.id.char_space_button)
    void onCharSpaceClicked() {
        if (mIsRoundFinished) return;
        mMorseOutputString += CHAR_SPACE;
        mMorseOutput.setText(mMorseOutputString);
        checkForSuccess();
    }

    @OnClick(R.id.word_space_button)
    void onWordSpaceClicked() {
        if (mIsRoundFinished) return;
        mMorseOutputString += WORD_SPACE;
        mMorseOutput.setText(mMorseOutputString);
        checkForSuccess();
    }

    @Override
    public void onSecondsTicked(int remainingSeconds) {
        updateTimeLeft(remainingSeconds);
    }

    @Override
    public void onTimerStarted() {

    }

    @Override
    public void onTimerFinished() {
        finishRound(checkPhrasePattern());
    }

    public static class RoundFinishedEvent {
        private boolean mSuccess;

        public RoundFinishedEvent(boolean success) {
            mSuccess = success;
        }

        public boolean isSuccess() {
            return mSuccess;
        }
    }
}
