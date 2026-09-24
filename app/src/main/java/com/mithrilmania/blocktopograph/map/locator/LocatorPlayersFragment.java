package com.mithrilmania.blocktopograph.map.locator;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mithrilmania.blocktopograph.R;
import com.mithrilmania.blocktopograph.databinding.FragLocatorPlayersBinding;
import com.mithrilmania.blocktopograph.databinding.ItemLocatorPlayerBinding;
import com.mithrilmania.blocktopograph.map.Player;
import com.mithrilmania.blocktopograph.util.math.DimensionVector3;
import com.mithrilmania.blocktopograph.world.World;

import java.lang.ref.WeakReference;
import java.util.List;

public final class LocatorPlayersFragment extends LocatorPageFragment {

    private FragLocatorPlayersBinding mBinding;
    private World mWorld;

    public static LocatorPlayersFragment create(World world) {
        LocatorPlayersFragment ret = new LocatorPlayersFragment();
        ret.mWorld = world;
        return ret;
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(
                inflater, R.layout.frag_locator_players, container, false);
        mBinding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        MarkersFragmentCompatKt.loadPlayerMarkers(this, mWorld, mBinding);
        return mBinding.getRoot();
    }

    static class PlayersAdapter extends RecyclerView.Adapter<PlayersAdapter.MeowHolder> {

        private final WeakReference<LocatorPageFragment> owner;
        private final List<Player> players;

        PlayersAdapter(WeakReference<LocatorPageFragment> owner, List<Player> players) {
            this.owner = owner;
            this.players = players;
        }

        @NonNull
        @Override
        public MeowHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            LocatorPageFragment owner = this.owner.get();
            assert owner != null;
            ItemLocatorPlayerBinding binding = DataBindingUtil.inflate(
                    owner.getLayoutInflater(), R.layout.item_locator_player,
                    viewGroup, false
            );
            MeowHolder meowHolder = new MeowHolder(binding.getRoot());
            meowHolder.binding = binding;
            return meowHolder;
        }

        @Override
        public void onBindViewHolder(@NonNull MeowHolder meowHolder, int i) {
            Player player = players.get(i);
            meowHolder.binding.setPlayer(player);
        }

        @Override
        public int getItemCount() {
            return players.size();
        }

        class MeowHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            ItemLocatorPlayerBinding binding;

            MeowHolder(@NonNull View itemView) {
                super(itemView);
                itemView.setOnClickListener(this);
            }

            @Override
            public void onClick(View view) {
                LocatorPageFragment owner = PlayersAdapter.this.owner.get();
                if (owner == null) return;
                if (owner.mCameraMoveCallback != null) {
                    int index = getAbsoluteAdapterPosition();
                    DimensionVector3<Float> pos = players.get(index).getPosition();
                    if (pos != null)
                        owner.mCameraMoveCallback.moveCamera(pos.x, pos.z);
                }
            }
        }
    }
}
