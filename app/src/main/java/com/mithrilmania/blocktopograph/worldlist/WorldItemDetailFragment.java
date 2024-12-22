package com.mithrilmania.blocktopograph.worldlist;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.mithrilmania.blocktopograph.Log;
import com.mithrilmania.blocktopograph.R;
import com.mithrilmania.blocktopograph.WorldActivity;
import com.mithrilmania.blocktopograph.databinding.WorlditemDetailBinding;
import com.mithrilmania.blocktopograph.test.MainTestActivity;
import com.mithrilmania.blocktopograph.world.WorldHandler;
import com.mithrilmania.blocktopograph.world.WorldModel;

import java.util.Arrays;

/**
 * A fragment representing a single WorldItem detail screen.
 * This fragment is either contained in a {@link OldWorldItemListActivity}
 * in two-pane mode (on tablets) or a {@link WorldItemDetailActivity}
 * on handsets.
 */
public class WorldItemDetailFragment extends Fragment implements View.OnClickListener {

    /**
     * The dummy content this fragment is presenting.
     */
    private static final byte[] SEQ_TEST = {0, 1, 1, 0};

    private WorldModel model;
    private byte[] mSequence;
    private WorlditemDetailBinding binding;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public WorldItemDetailFragment() {
    }

//    private String getDate(long time) {
//        Calendar cal = Calendar.getInstance();
//        TimeZone tz = cal.getTimeZone();//get your local time zone.
//        SimpleDateFormat sdf = new SimpleDateFormat(getString(R.string.full_date_format), Locale.ENGLISH);
//        sdf.setTimeZone(tz);//set time zone.
//        return sdf.format(new Date(time * 1000));
//    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.binding = DataBindingUtil.inflate(inflater, R.layout.worlditem_detail, container, false);
        FragmentActivity activity = this.requireActivity();
        this.model = new ViewModelProvider(activity).get(WorldModel.class);
        return binding.getRoot();
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mSequence = new byte[]{-1, -1, -1, -1};
        FragmentActivity activity = this.requireActivity();
        WorldHandler handler = this.model.getHandler();
        String barTitle = handler == null ? activity.getString(R.string.error_could_not_open_world) : handler.getPlainName();

        CollapsingToolbarLayout appBarLayout = activity.findViewById(R.id.toolbar_layout);
        if (appBarLayout != null) {
            appBarLayout.setTitle(barTitle);
        }
        WorlditemDetailBinding binding = this.binding;
        try {
            if (handler != null) {
                Context context = this.requireContext();
                binding.setName(handler.getPlainName());
                binding.setSize(/*IoUtil.getFileSizeInText(FileUtils.sizeOf(world.getRoot()))*/"Calculating...");
                binding.setMode(handler.getWorldGameMode(context));
                binding.setTime(handler.getFormattedLastPlayedTimestamp(context));
                binding.setSeed(String.valueOf(handler.getWorldSeed(context)));
                binding.setPath(handler.getPath());
            }
        } catch (Exception e) {
            Log.d(this, e);
        }
        binding.fabOpenWorld.setOnClickListener(fab -> startWorldActivity());

        binding.buttonLeft.setOnClickListener(this);
        binding.buttonRight.setOnClickListener(this);
        binding.buttonBackup.setOnClickListener(this);
    }

    private void startWorldActivity() {
        Activity activity = getActivity();
        assert activity != null;
        activity.startActivity(this.model.getHandler().prepareIntent(new Intent(activity, WorldActivity.class)));
    }

    private void sequence(byte code) {
        int pos = mSequence.length - 1;
        System.arraycopy(mSequence, 1, mSequence, 0, pos);
        mSequence[pos] = code;
        if (Arrays.equals(mSequence, SEQ_TEST)) {
            Activity activity = getActivity();
            assert activity != null;
            activity.startActivity(this.model.getHandler().prepareIntent(new Intent(activity, MainTestActivity.class)));
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_left:
                sequence((byte) 0);
                break;
            case R.id.button_right:
                sequence((byte) 1);
                break;
        }
    }
}

