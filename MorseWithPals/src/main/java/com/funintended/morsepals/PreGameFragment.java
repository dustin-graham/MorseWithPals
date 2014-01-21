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
public class PreGameFragment extends Fragment {

    private static final String SECONDS_TO_COMPLETE_ARG = "PreGameFragment:SecondsToComplete";

    @InjectView(R.id.challenge_description)
    TextView mChallengeDescriptionTextView;

    @InjectView(R.id.start_turn_button)
    Button mStartMatchButton;

    private int mSecondsToComplete = 0;

    public static PreGameFragment create(int secondsToComplete) {
        PreGameFragment frag = new PreGameFragment();
        Bundle args = new Bundle();
        args.putInt(SECONDS_TO_COMPLETE_ARG, secondsToComplete);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pre_game, container, false);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.inject(this, view);
        String challengeDescriptionString = getResources().getString(R.string.convert_to_morse,30);
        String currentScoreString = getString(R.string.current_score, ((MorseGameHost) getActivity()).getCurrentPlayer().getMORSEScore());
        mChallengeDescriptionTextView.setText(challengeDescriptionString + "\n" +currentScoreString);
    }

    @OnClick(R.id.start_turn_button)
    void onStartMatchClicked() {
        BusProvider.getInstance().post(new OnMatchReadyEvent());
    }

    public static class OnMatchReadyEvent {

    }
}
