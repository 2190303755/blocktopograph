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
import com.mithrilmania.blocktopograph.databinding.ItemLocatorMarkerBinding;
import com.mithrilmania.blocktopograph.map.marker.AbstractMarker;
import com.mithrilmania.blocktopograph.world.World;

import java.lang.ref.WeakReference;
import java.util.List;

public final class LocatorMarkersFragment extends LocatorPageFragment {

    private FragLocatorPlayersBinding mBinding;
    private World mWorld;

    public static LocatorMarkersFragment create(World world) {
        LocatorMarkersFragment ret = new LocatorMarkersFragment();
        ret.mWorld = world;
        return ret;
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(
                inflater, R.layout.frag_locator_players, container, false);
        mBinding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        MarkersFragmentCompatKt.loadLocatorMarkers(this, mWorld, mBinding);
        return mBinding.getRoot();
    }

    static class MarkersAdapter extends RecyclerView.Adapter<MarkersAdapter.MeowHolder> {

        @NonNull
        private final WeakReference<LocatorPageFragment> owner;

        @NonNull
        private final List<AbstractMarker> markers;

        MarkersAdapter(@NonNull WeakReference<LocatorPageFragment> owner, @NonNull List<AbstractMarker> markers) {
            this.owner = owner;
            this.markers = markers;
        }

        @NonNull
        @Override
        public MeowHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            LocatorPageFragment owner = this.owner.get();
            LayoutInflater inflater;
            if (owner != null) inflater = owner.getLayoutInflater();
            else inflater = LayoutInflater.from(viewGroup.getContext());

            ItemLocatorMarkerBinding binding = DataBindingUtil.inflate(
                    inflater, R.layout.item_locator_marker,
                    viewGroup, false
            );
            MeowHolder meowHolder = new MeowHolder(binding.getRoot());
            meowHolder.binding = binding;
            return meowHolder;
        }

        @Override
        public void onBindViewHolder(@NonNull MeowHolder meowHolder, int i) {
            AbstractMarker marker = markers.get(i);
            meowHolder.binding.setMarker(marker);
        }

        @Override
        public int getItemCount() {
            return markers.size();
        }

        class MeowHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            ItemLocatorMarkerBinding binding;

            MeowHolder(@NonNull View itemView) {
                super(itemView);
                itemView.setOnClickListener(this);
            }

            @Override
            public void onClick(View view) {
                LocatorPageFragment owner = MarkersAdapter.this.owner.get();
                if (owner == null) return;
                if (owner.mCameraMoveCallback != null) {
                    int index = getAbsoluteAdapterPosition();
                    AbstractMarker marker = markers.get(index);
                    owner.mCameraMoveCallback.moveCamera(marker.x, marker.z);
                }
            }
        }

    }

}
