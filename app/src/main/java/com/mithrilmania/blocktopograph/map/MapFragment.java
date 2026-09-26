package com.mithrilmania.blocktopograph.map;

import static com.mithrilmania.blocktopograph.editor.world.v2.WorldEditorModelKt.CHUNK_DIMENSION;
import static com.mithrilmania.blocktopograph.world.DimensionKt.defaultMapTypeCompat;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.mithrilmania.blocktopograph.LogUtil;
import com.mithrilmania.blocktopograph.R;
import com.mithrilmania.blocktopograph.block.KnownBlockRepr;
import com.mithrilmania.blocktopograph.chunk.Chunk;
import com.mithrilmania.blocktopograph.chunk.NBTChunkData;
import com.mithrilmania.blocktopograph.databinding.MapFragmentBinding;
import com.mithrilmania.blocktopograph.editor.world.WorldViewerModel;
import com.mithrilmania.blocktopograph.map.edit.EditFunction;
import com.mithrilmania.blocktopograph.map.edit.EditFunctionCompatKt;
import com.mithrilmania.blocktopograph.map.edit.RectEditTarget;
import com.mithrilmania.blocktopograph.map.locator.AdvancedLocatorFragment;
import com.mithrilmania.blocktopograph.map.marker.AbstractMarker;
import com.mithrilmania.blocktopograph.map.marker.CustomNamedBitmapProvider;
import com.mithrilmania.blocktopograph.map.picer.PicerDialogFragment;
import com.mithrilmania.blocktopograph.map.picer.PicerState;
import com.mithrilmania.blocktopograph.map.renderer.MapType;
import com.mithrilmania.blocktopograph.map.selection.SelectionMenuFragment;
import com.mithrilmania.blocktopograph.util.AsyncKt;
import com.mithrilmania.blocktopograph.util.NamedBitmapProvider;
import com.mithrilmania.blocktopograph.util.NamedBitmapProviderHandle;
import com.mithrilmania.blocktopograph.util.math.DimensionVec3f;
import com.mithrilmania.blocktopograph.util.math.DimensionVector3;
import com.mithrilmania.blocktopograph.world.Dimension;
import com.mithrilmania.blocktopograph.world.DimensionKt;
import com.mithrilmania.blocktopograph.world.WorldKt;
import com.mithrilmania.blocktopograph.world.WorldModel;
import com.mithrilmania.blocktopograph.world.WorldModelKt;
import com.mithrilmania.blocktopograph.world.WorldStorage;
import com.mithrilmania.blocktopograph.world.chunk.ChunkTag;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import kotlinx.coroutines.Job;

public class MapFragment extends Fragment {

    private final static int MARKER_INTERVAL_CHECK = 50;
    private static final String TAG_PICER = "picer";
    private static final int MARKERS_ON_SCREEN_TOO_MANY = 100;
    private static final String PREF_KEY_HAS_NOTIFIED_MARKERS_TOO_MANY = "has_notified_markers_too_many";
    private static final String PREF_KEY_HAS_USED_SELECTION = "hasUsedSelection";
    public static final String KEY_HAS_DOUBLE_TAP = "hasDoubleTap";
    //static, remember choice while app is open.
    private static Map<NamedBitmapProvider, BitmapChoiceListAdapter.NamedBitmapChoice> markerFilter = new HashMap<>();

    static {
        //entities are enabled by default
        for (Entity v : Entity.values()) {
            //skip things without a bitmap (dropped items etc.)
            //skip entities with placeholder ids (900+)
            if (v.sheetPos < 0 || v.id >= 900) continue;
            markerFilter.put(v.getNamedBitmapProvider(),
                    new BitmapChoiceListAdapter.NamedBitmapChoice(v, true));
        }
        //tile-entities are disabled by default
        for (TileEntity v : TileEntity.values()) {
            markerFilter.put(v.getNamedBitmapProvider(),
                    new BitmapChoiceListAdapter.NamedBitmapChoice(v, false));
        }

    }

    //procedural markers can be iterated while other threads add things to it;
    // iterating performance is not really affected because of the small size;
    private CopyOnWriteArraySet<AbstractMarker> proceduralMarkers = new CopyOnWriteArraySet<>();

    private Set<AbstractMarker> staticMarkers = new HashSet<>();

    private AbstractMarker spawnMarker;
    private AbstractMarker localPlayerMarker;
    private MCTileProvider minecraftTileProvider;
    private int proceduralMarkersInterval = 0;
    private volatile @NonNull Job shrinkProceduralMarkersJob = MapFragmentCompatKt.dummyJob();
    private WorldViewerModel model;
    private WorldModel worldModel;

    /**
     * Only one floating fragment is allowed at the same time.
     * We use one to display locator.
     */
    private Fragment mFloatingFragment;

    /**
     * Data binding of this fragment view.
     */
    private MapFragmentBinding mBinding;

    @Override
    public void onPause() {
        super.onPause();

        //pause drawing the map
        mBinding.tileView.pause();
    }

    @Override
    public void onStart() {
        super.onStart();
        FragmentActivity activity = getActivity();
        if (activity == null) return;
        activity.setTitle(worldModel.getWorld().getPlainName());
    }

    @Override
    public void onResume() {
        super.onResume();

        //resume drawing the map
        mBinding.tileView.resume();
    }

    public void closeChunks() {
        WorldStorage storage = this.worldModel.getWorld().getStorage();
        if (storage == null) return;
        storage.resetCache();
    }

    private String[] getMarkerTapOptions() {
        MarkerTapOption[] values = MarkerTapOption.values();
        int len = values.length;
        String[] options = new String[len];
        for (int i = 0; i < len; i++) {
            options[i] = getString(values[i].stringId);
        }
        return options;
    }

    public void moveCameraToPlayer(DimensionVec3f pos) {
        var dimension = DimensionKt.toVanillaDimension(pos.dimensionId);
        if (dimension == null) return;
        if (dimension != this.model.getDimension()) {
            this.model.setDimension(dimension);
            this.model.getMapType().setValue(defaultMapTypeCompat(dimension));
        }
        this.frameTo(pos.x, pos.z);
        Snackbar.make(
                mBinding.tileView,
                getString(R.string.something_at_xyz_dim_float, getString(R.string.player), pos.x, pos.y, pos.z),
                Snackbar.LENGTH_SHORT
        ).show();
    }

    public void moveCameraToSpawn(Dimension dimension, int x, int y, int z) {
        if (dimension != this.model.getDimension()) {
            this.model.setDimension(dimension);
            this.model.getMapType().setValue(defaultMapTypeCompat(dimension));
        }
        this.frameTo(x, z);
        Snackbar.make(
                mBinding.tileView,
                getString(R.string.something_at_xyz_dim_int, getString(R.string.spawn), x, y, z),
                Snackbar.LENGTH_SHORT
        ).show();
    }

    /**
     * Callback to close the ongoing floating pane.
     */
    @UiThread
    private void closeFloatPane() {
        if (mFloatingFragment != null) {
            WorldStorage storage = this.worldModel.getWorld().getStorage();
            if (storage == null) return;
            FragmentManager fm = getChildFragmentManager();
            FragmentTransaction trans = fm.beginTransaction();
            trans.remove(mFloatingFragment);

            // If in selection mode, recover selection menu.
            if (mBinding.selectionBoard.hasSelection()) {
                SelectionMenuFragment fragment = SelectionMenuFragment
                        .newInstance(mBinding.selectionBoard.getSelection(),
                                this::doSelectionBasedEdit);
                trans.add(R.id.float_window_container, fragment);
                mFloatingFragment = fragment;
                setUpSelectionMenu();
            } else mFloatingFragment = null;

            trans.commit();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mFloatingFragment != null) {
            FloatPaneFragment fragment;
            if (mFloatingFragment instanceof AdvancedLocatorFragment) {
                fragment = AdvancedLocatorFragment.create(worldModel.getWorld(), this::frameTo);
            } else if (mFloatingFragment instanceof SelectionMenuFragment) {
                WorldStorage storage = this.worldModel.getWorld().getStorage();
                if (storage == null) return;
                fragment = SelectionMenuFragment
                        .newInstance(mBinding.selectionBoard.getSelection(),
                                this::doSelectionBasedEdit);
            } else return;
            closeFloatPane();
            openFloatPane(fragment);
            setUpSelectionMenu();
        }
    }

    /**
     * Set up selection menu and connect it with selection board.
     *
     * <p>
     * Called when selection mode begins, or when another float pane that used to be opened and
     * replaced selection menu, we have to recover selection menu when the pane was dead.
     * </p>
     */
    private void setUpSelectionMenu() {
        if (mFloatingFragment instanceof SelectionMenuFragment) {
            SelectionMenuFragment fragment = (SelectionMenuFragment) mFloatingFragment;
            mBinding.selectionBoard.setSelectionChangedListener(fragment::onSelectionChangedOutsides);
            fragment.setSelectionChangedListener(mBinding.selectionBoard::onSelectionChangedOutsides);
        }
    }

    /**
     * When another float pane was opened and replaced selection menu, we disconnect selection
     * menu and selection board.
     */
    private void unsetSelectionMenu() {
        if (mFloatingFragment instanceof SelectionMenuFragment) {
            SelectionMenuFragment fragment = (SelectionMenuFragment) mFloatingFragment;
            fragment.setSelectionChangedListener(null);
        }
        mBinding.selectionBoard.setSelectionChangedListener(null);
    }

    public void openAdvancedLocator() {
        this.openFloatPane(AdvancedLocatorFragment.create(worldModel.getWorld(), this::frameTo));
    }

    /**
     * Show a floating pane fragment. Will remove already existing one.
     *
     * @param fragment pane fragment to be attached.
     */
    @UiThread
    private void openFloatPane(@NonNull FloatPaneFragment fragment) {
        FragmentManager fm = getChildFragmentManager();
        fragment.setOnCloseButtonClickListener(this::closeFloatPane);
        FragmentTransaction trans = fm.beginTransaction();
        // Remove existing float pane.
        if (mFloatingFragment instanceof SelectionMenuFragment) unsetSelectionMenu();
        if (mFloatingFragment != null) trans.remove(mFloatingFragment);
        trans.add(R.id.float_window_container, fragment).commit();
        mFloatingFragment = fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FragmentActivity activity = this.requireActivity();
        WorldModel worldModel = WorldModelKt.getOrCreateWorldModel(activity);
        this.worldModel = worldModel;
        WorldViewerModel model = new ViewModelProvider(activity).get(WorldViewerModel.class);

        mBinding = DataBindingUtil.inflate(
                inflater, R.layout.map_fragment, container, false);
        mBinding.tileView.setSelectionView(mBinding.selectionBoard);
        mBinding.selectionBoard.setTileView(mBinding.tileView);
        mBinding.tileView.setOuterDoubleTapListener(new GestureDetector.OnDoubleTapListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                return false;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                model.showDrawer.trigger();
                FragmentActivity activity = getActivity();
                if (activity != null)
                    activity.getPreferences(Context.MODE_PRIVATE)
                            .edit().putBoolean(KEY_HAS_DOUBLE_TAP, true).apply();
                return false;
            }

            @Override
            public boolean onDoubleTapEvent(MotionEvent e) {
                return false;
            }
        });

        try {
            Entity.loadEntityBitmaps(activity.getAssets());
        } catch (IOException e) {
            LogUtil.d(this, e);
        }

        try {
            KnownBlockRepr.loadBitmaps(activity.getAssets());
        } catch (IOException e) {
            LogUtil.d(this, e);
        }
        try {
            CustomIcon.loadCustomBitmaps(activity.getAssets());
        } catch (IOException e) {
            LogUtil.d(this, e);
        }

        //set the map-type
        MapTileView tileView = mBinding.tileView;
        tileView.getDetailLevelManager().setLevelType(model.getMapType().getValue());

        tileView.setOnLongPressListener(this::onLongPressed);

        /*
        Create tile(=bitmap) provider
         */
        this.minecraftTileProvider = new MCTileProvider(model, worldModel);



        /*
        Set the bitmap-provider of the tile view
         */
        tileView.setBitmapProvider(this.minecraftTileProvider);


        /*
        Change tile view settings
         */

        tileView.setBackgroundColor(0xFF494E8E);

        // markers should align to the coordinate along the horizontal center and vertical bottom
        tileView.setMarkerAnchorPoints(-0.5f, -1.0f);

        tileView.defineBounds(
                -1.0,
                -1.0,
                1.0,
                1.0
        );

        tileView.setSize(MCTileProvider.viewSizeW, MCTileProvider.viewSizeL);

        for (MapType mapType : MapType.values()) {
            tileView.addDetailLevel(0.0625f, "0.0625", MCTileProvider.TILESIZE, MCTileProvider.TILESIZE, mapType);// 1/(1/16)=16 chunks per tile
            tileView.addDetailLevel(0.125f, "0.125", MCTileProvider.TILESIZE, MCTileProvider.TILESIZE, mapType);
            tileView.addDetailLevel(0.25f, "0.25", MCTileProvider.TILESIZE, MCTileProvider.TILESIZE, mapType);
            tileView.addDetailLevel(0.5f, "0.5", MCTileProvider.TILESIZE, MCTileProvider.TILESIZE, mapType);
            tileView.addDetailLevel(1f, "1", MCTileProvider.TILESIZE, MCTileProvider.TILESIZE, mapType);// 1/1=1 chunk per tile
        }

        tileView.setScale(0.5f);

        // Notify user to try using selection.
        if (!activity.getPreferences(Context.MODE_PRIVATE).getBoolean(PREF_KEY_HAS_USED_SELECTION, false)) {
            if (System.currentTimeMillis() % 25 == 0) tileView.postDelayed(() -> {
                FragmentActivity activity1 = getActivity();
                if (activity1 == null) return;
                new AlertDialog.Builder(activity1)
                        .setMessage(R.string.map_notice_try_selection)
                        .setPositiveButton(android.R.string.ok, null)
                        .create()
                        .show();
                activity1.getPreferences(Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean(PREF_KEY_HAS_USED_SELECTION, true)
                        .apply();
            }, 1000);
        }


        tileView.getMarkerLayout().setMarkerTapListener((view, tapX, tapY) -> {
            if (!(view instanceof ImageView)) return;

            var tag = view.getTag();
            if (!(tag instanceof AbstractMarker marker)) return;

            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(String.format(getString(R.string.marker_info), marker.getNamedBitmapProvider().getBitmapDisplayName(), marker.x, marker.y, marker.z))
                    .setItems(getMarkerTapOptions(), (dialog, which) -> {

                        final MarkerTapOption chosen = MarkerTapOption.values()[which];

                        switch (chosen) {
                            case TELEPORT_LOCAL_PLAYER: {
                                /*fixme try {
                                    final EditableNBT playerEditable = worldProvider.get().getEditablePlayer();
                                    if (playerEditable == null)
                                        throw new Exception("Player is null");

                                    Iterator playerIter = playerEditable.getTags().iterator();
                                    if (!playerIter.hasNext())
                                        throw new Exception("Player DB entry is empty!");

                                    //db entry consists of one compound tag
                                    final CompoundTag playerTag = (CompoundTag) playerIter.next();

                                    ListTag posVec = (ListTag) playerTag.getChildTagByKey("Pos");

                                    if (posVec == null)
                                        throw new Exception("No \"Pos\" specified");

                                    final List<Tag> playerPos = posVec.getValue();
                                    if (playerPos == null)
                                        throw new Exception("No \"Pos\" specified");
                                    if (playerPos.size() != 3)
                                        throw new Exception("\"Pos\" value is invalid. value: " + posVec.getValue().toString());

                                    IntTag dimensionId = (IntTag) playerTag.getChildTagByKey("DimensionId");
                                    if (dimensionId == null || dimensionId.getValue() == null)
                                        throw new Exception("No \"DimensionId\" specified");


                                    int newX = marker.x;
                                    int newY = marker.y;
                                    int newZ = marker.z;
                                    Dimension newDimension = marker.dimension;

                                    ((FloatTag) playerPos.get(0)).setValue(((float) newX) + 0.5f);
                                    ((FloatTag) playerPos.get(1)).setValue(((float) newY) + 0.5f);
                                    ((FloatTag) playerPos.get(2)).setValue(((float) newZ) + 0.5f);
                                    dimensionId.setValue(newDimension.id);


                                    if (playerEditable.save()) {

                                        localPlayerMarker = moveMarker(localPlayerMarker, newX, newY, newZ, newDimension);

                                        //TODO could be improved for translation friendliness
                                        Snackbar.make(mBinding.tileView,
                                                activity.getString(R.string.teleported_player_to_xyz_dimension) + newX + ";" + newY + ";" + newZ + " [" + newDimension.name + "] (" + marker.getNamedBitmapProvider().getBitmapDisplayName() + ")",
                                                Snackbar.LENGTH_LONG)
                                                .show();

                                    } else throw new Exception("Failed saving player");

                                } catch (Exception e) {
                                    Log.d(this, e.toString());

                                    Snackbar.make(mBinding.tileView, R.string.failed_teleporting_player,
                                            Snackbar.LENGTH_LONG)
                                            .show();
                                }*/
                                return;
                            }
                            case REMOVE_MARKER: {
                                /*fixme if (marker.isCustom) {
                                    MapFragment.this.removeMarker(marker);
                                    MarkerManager mng = world.getMarkerManager();
                                    mng.removeMarker(marker, true);

                                    mng.save();

                                } else {
                                    //only custom markers are meant to be removable
                                    Snackbar.make(mBinding.tileView, R.string.marker_is_not_removable,
                                            Snackbar.LENGTH_LONG)
                                            .show();
                                }*/
                            }
                        }
                    })
                    .setCancelable(true)
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();

        });


        //do not loop the scale
        tileView.setShouldLoopScale(false);

        //prevents flickering
        tileView.setTransitionsEnabled(false);

        //more responsive rendering
        tileView.setShouldRenderWhilePanning(false);

        tileView.setSaveEnabled(true);
        LifecycleOwner owner = this.getViewLifecycleOwner();
        Observer<Object> refresh = visible -> resetTileView();
        model.getShowGrid().observe(owner, refresh);
        model.getMapType().observe(owner, refresh);
        model.getShowMarkers().observe(owner, visible -> {
            if (visible) {
                resetTileView();
            } else {
                for (AbstractMarker marker : proceduralMarkers) {
                    if (staticMarkers.contains(marker)) continue;
                    removeMarker(marker);
                }
                //resetTileView();
            }
        });
        AsyncKt.openDB(worldModel, this, handler -> {
            boolean framedToPlayer = false;
            try {
                var playerPos = WorldKt.resolveLocalPlayerPosCompat(handler, activity);
                if (playerPos != null) {
                    float x = playerPos.x, y = playerPos.y, z = playerPos.z;
                    LogUtil.d(this, "Placed player marker at: " + x + ";" + y + ";" + z + " [" + playerPos.dimension.getIdentifier() + "]");
                    localPlayerMarker = new AbstractMarker((int) x, (int) y, (int) z,
                            playerPos.dimension, new CustomNamedBitmapProvider(Entity.PLAYER, "~local_player"), false);
                    this.staticMarkers.add(localPlayerMarker);
                    addMarker(localPlayerMarker);
                    if (localPlayerMarker.dimension != model.getDimension()) {
                        model.setDimension(localPlayerMarker.dimension);
                        model.getMapType().setValue(defaultMapTypeCompat(localPlayerMarker.dimension));
                    }
                    frameTo(x, z);
                    framedToPlayer = true;
                }

            } catch (Exception e) {
                LogUtil.d(this, "Failed to place player marker.", e);
            }

            try {
                DimensionVector3<Integer> spawnPos = WorldKt.resolveSpawnPoint(handler, activity);
                spawnMarker = new AbstractMarker(spawnPos.x, spawnPos.y, spawnPos.z, spawnPos.dimension,
                        new CustomNamedBitmapProvider(CustomIcon.SPAWN_MARKER, "Spawn"), false);
                this.staticMarkers.add(spawnMarker);
                addMarker(spawnMarker);

                if (!framedToPlayer) {
                    if (spawnMarker.dimension != model.getDimension()) {
                        model.setDimension(spawnMarker.dimension);
                        model.getMapType().postValue(defaultMapTypeCompat(spawnMarker.dimension));
                    }
                    frameTo((double) spawnPos.x, (double) spawnPos.z);
                }

            } catch (Exception e) {
                //no spawn defined...
                if (!framedToPlayer) frameTo(0.0, 0.0);

            }
        });
        MapFragmentCompatKt.registerSignalListener(this, model);
        this.model = model;
        return mBinding.getRoot();
    }

    public void showPicerDialog() {
        MapFragmentCompatKt.setAnalyzingPicerState(this.model, this.worldModel.getWorld());
        new PicerDialogFragment().show(getChildFragmentManager(), TAG_PICER);
    }

    /**
     * Creates a new marker (looking exactly the same as the old one) on the new position,
     * while removing the old marker.
     *
     * @param marker    Marker to be recreated
     * @param x         new pos X
     * @param y         new pos Y
     * @param z         new pos Z
     * @param dimension new pos Dimension
     * @return the newly created marker, which should replace the old one.
     */
    private AbstractMarker moveMarker(AbstractMarker marker, int x, int y, int z, Dimension dimension) {
        AbstractMarker newMarker = marker.copy(x, y, z, dimension);
        if (staticMarkers.remove(marker)) staticMarkers.add(newMarker);
        this.removeMarker(marker);
        this.addMarker(newMarker);

        /*if (marker.isCustom) {
            MarkerManager mng = world.getMarkerManager();
            mng.removeMarker(marker, true);
            mng.addMarker(newMarker, true);
        }*/

        return newMarker;
    }

    private void doSelectionBasedEdit(@NonNull EditFunction func, @Nullable Bundle args) {
        switch (func) {
            case SNR:
            case LAMPSHADE:
            case CHBIOME:
            case DCHUNK:
                WorldStorage storage = this.worldModel.getWorld().getStorage();
                if (storage == null) return;
                EditFunctionCompatKt.performSelectionBasedEdit(this, new RectEditTarget(
                        storage,
                        mBinding.selectionBoard.getSelection(),
                        this.model.getDimension()
                ), func, args);
                break;
            case PICER: {
                var activity = getActivity();
                if (activity == null) return;
                this.model.commitAnalyzedState(mBinding.selectionBoard.getSelection(), PicerState.SelectionOutOfSize.INSTANCE);
                new PicerDialogFragment().show(activity.getSupportFragmentManager(), TAG_PICER);
            }
        }
    }

    /**
     * Calculates viewport of tileview, expressed in blocks.
     *
     * @param marginX horizontal viewport-margin, in pixels
     * @param marginZ vertical viewport-margin, in pixels
     * @return minimum_X, maximum_X, minimum_Z, maximum_Z, dimension. (min and max are expressed in blocks!)
     */
    public ViewPort calculateViewPort(int marginX, int marginZ) {

        // 1 chunk per tile on scale 1.0
        int pixelsPerBlockW_unscaled = MCTileProvider.TILESIZE / 16;
        int pixelsPerBlockL_unscaled = MCTileProvider.TILESIZE / 16;

        MapTileView tileView = mBinding.tileView;
        float scale = tileView.getScale();

        //scale the amount of pixels, less pixels per oldBlock if zoomed out
        double pixelsPerBlockW = pixelsPerBlockW_unscaled * scale;
        double pixelsPerBlockL = pixelsPerBlockL_unscaled * scale;

        long blockX = Math.round((tileView.getScrollX() - marginX) / pixelsPerBlockW) - MCTileProvider.HALF_WORLDSIZE;
        long blockZ = Math.round((tileView.getScrollY() - marginZ) / pixelsPerBlockL) - MCTileProvider.HALF_WORLDSIZE;
        long blockW = Math.round((tileView.getWidth() + marginX + marginX) / pixelsPerBlockW);
        long blockH = Math.round((tileView.getHeight() + marginZ + marginZ) / pixelsPerBlockL);

        return new ViewPort(
                blockX,
                blockX + blockW,
                blockZ,
                blockZ + blockH,
                this.model.getDimension()
        );
    }

    /**
     * Important: this method should be run from the UI thread.
     *
     * @param marker The marker to remove from the tile view.
     */
    public void removeMarker(AbstractMarker marker) {
        staticMarkers.remove(marker);
        proceduralMarkers.remove(marker);
        if (marker.view != null && mBinding.tileView != null)
            mBinding.tileView.removeMarker(marker.view);
    }

    /**
     * Important: this method should be run from the UI thread.
     *
     * @param marker The marker to add to the tile view.
     */
    public synchronized void addMarker(AbstractMarker marker) {

        Activity act = getActivity();
        if (act == null) return;

        TileEntity.loadIcons(act.getAssets());

        for (AbstractMarker abstractMarker : proceduralMarkers) {
            if (abstractMarker.equals(marker)) {
                removeMarker(abstractMarker);
            }
        }

        if ((shrinkProceduralMarkersJob == null || !shrinkProceduralMarkersJob.isActive())
                && ++proceduralMarkersInterval > MARKER_INTERVAL_CHECK) {

            //shrink set of markers to viewport every so often

            DisplayMetrics displayMetrics = act.getResources().getDisplayMetrics();
            //reset this to start accepting viewport update requests again.
            shrinkProceduralMarkersJob = MapFragmentCompatKt.retainViewPortMarkers(
                    this,
                    this.proceduralMarkers,
                    this.staticMarkers,
                    this.calculateViewPort(
                            displayMetrics.widthPixels / 2,
                            displayMetrics.heightPixels / 2
                    )
            );

            proceduralMarkersInterval = 0;
        }

        proceduralMarkers.add(marker);

        if (proceduralMarkers.size() > MARKERS_ON_SCREEN_TOO_MANY) {
            SharedPreferences pref = act.getPreferences(Context.MODE_PRIVATE);
            if (!pref.getBoolean(PREF_KEY_HAS_NOTIFIED_MARKERS_TOO_MANY, false)) {
                pref.edit().putBoolean(PREF_KEY_HAS_NOTIFIED_MARKERS_TOO_MANY, true).apply();
                AlertDialog dialog = new AlertDialog.Builder(act)
                        .setTitle(R.string.map_smart_notice_too_many_markers)
                        .setMessage(R.string.map_smart_notice_too_many_markers_message)
                        .setPositiveButton(R.string.map_uioption_open_drawer, (dialogInterface, i) -> this.model.showDrawer.trigger())
                        .setNegativeButton(R.string.general_got_it, null)
                        .create();
                dialog.setCanceledOnTouchOutside(false);
                dialog.show();
            }
        }

        var markerView = marker.getView(act);

        this.filterMarker(marker);

        if (mBinding.tileView.getMarkerLayout().indexOfChild(markerView) >= 0) {
            mBinding.tileView.getMarkerLayout().removeMarker(markerView);
        }


        mBinding.tileView.addMarker(markerView,
                (double) marker.x / (double) MCTileProvider.HALF_WORLDSIZE,
                (double) marker.z / (double) MCTileProvider.HALF_WORLDSIZE,
                -0.5f, -0.5f);
    }

    public void toggleMarkers() {
        if (Boolean.TRUE.equals(this.model.getShowMarkers().getValue())) {
            resetTileView();
        } else {
            for (AbstractMarker marker : proceduralMarkers) {
                if (staticMarkers.contains(marker)) continue;
                removeMarker(marker);
            }
            //resetTileView();
        }
    }

    private String[] getLongClickOptions() {
        String[] values = new String[]{
                getString(R.string.teleport_local_player),
                getString(R.string.create_custom_marker),
                getString(R.string.open_chunk_entity_nbt),
                getString(R.string.open_chunk_tile_entity_nbt),
                null
        };
        values[4] = getString(mBinding.selectionBoard.hasSelection() ?
                R.string.func_cancel_selection : R.string.func_begin_selection);
        return values;
    }

    public void triggerLongPressAtCenter() {
        MotionEvent event = MotionEvent.obtain(0L, 0L, 0,
                (float) (mBinding.tileView.getMeasuredWidth() / 2),
                (float) (mBinding.tileView.getMeasuredHeight() / 2), 0);
        onLongPressed(event);
        // ...
    }

    /**
     * Map long press handler, shows a list of options.
     *
     * @param event long press event.
     */
    private void onLongPressed(@NonNull MotionEvent event) {
        Dimension dimension = this.model.getDimension();

        // 1 chunk per tile on scale 1.0
        int pixelsPerBlockW_unscaled = MCTileProvider.TILESIZE / CHUNK_DIMENSION;
        int pixelsPerBlockL_unscaled = MCTileProvider.TILESIZE / CHUNK_DIMENSION;

        float scale = mBinding.tileView.getScale();
        float pixelsPerBlockScaledW = pixelsPerBlockW_unscaled * scale;
        float pixelsPerBlockScaledL = pixelsPerBlockL_unscaled * scale;


        double worldX = (((mBinding.tileView.getScrollX() + event.getX()) / pixelsPerBlockScaledW) - MCTileProvider.HALF_WORLDSIZE);
        double worldZ = (((mBinding.tileView.getScrollY() + event.getY()) / pixelsPerBlockScaledL) - MCTileProvider.HALF_WORLDSIZE);

        //MapFragment.this.onLongClick(worldX, worldZ);

        final Activity activity = getActivity();
        if (activity == null) return;

        double chunkX = worldX / CHUNK_DIMENSION;
        double chunkZ = worldZ / CHUNK_DIMENSION;

        //negative doubles are rounded up when casting to int; floor them
        final int chunkXint = chunkX < 0 ? (((int) chunkX) - 1) : ((int) chunkX);
        final int chunkZint = chunkZ < 0 ? (((int) chunkZ) - 1) : ((int) chunkZ);


        final View container = activity.findViewById(R.id.world_content);
        if (container == null) {
            LogUtil.d(this, "CANNOT FIND MAIN CONTAINER, WTF");
            return;
        }

        AlertDialog alertDialog = new AlertDialog.Builder(new ContextThemeWrapper(activity, R.style.AppTheme_Floating))
                .setTitle(getString(R.string.postion_2D_floats_with_chunkpos, worldX, worldZ, chunkXint, chunkZint))
                .setItems(getLongClickOptions(), (dialog, which) -> {


                    switch (which) {
                        case 0:
                            onChooseTeleportPlayer((float) worldX, (float) worldZ, dimension, container);
                            break;
                        case 1:
                            onChooseAddMarker((int) worldX, (int) worldZ, activity, dimension, container);
                            break;
                        case 2:
                            onChooseEditEntitiesOrTileEntities(dimension, chunkXint, chunkZint, container, true);
                            break;
                        case 3:
                            onChooseEditEntitiesOrTileEntities(dimension, chunkXint, chunkZint, container, false);
                            break;
                        case 4:
                            beginOrEndSelection((int) worldX, (int) worldZ);
                    }


                })
                .setCancelable(true)
                .setNegativeButton(android.R.string.cancel, null)
                .show();
        alertDialog.getListView().post(() ->
                alertDialog.getListView().smoothScrollToPositionFromTop(4, 40));
        Window window = alertDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.bg_dialog_transparent);
            window.setDimAmount(0.3f);
        }

    }

    private void beginOrEndSelection(int worldX, int worldZ) {
        if (mBinding.selectionBoard.hasSelection()) {
            mBinding.selectionBoard.endSelection();
            // If it's already replaced don't kill the wrong pig.
            if (mFloatingFragment instanceof SelectionMenuFragment) closeFloatPane();
        } else {
            mBinding.selectionBoard.beginSelection(worldX, worldZ);
            WorldStorage storage = this.worldModel.getWorld().getStorage();
            if (storage == null) return;
            SelectionMenuFragment fragment = SelectionMenuFragment
                    .newInstance(mBinding.selectionBoard.getSelection(), this::doSelectionBasedEdit);
            openFloatPane(fragment);
            setUpSelectionMenu();
            Activity activity = getActivity();
            if (activity != null) {
                activity.getPreferences(Context.MODE_PRIVATE).edit()
                        .putBoolean(PREF_KEY_HAS_USED_SELECTION, true).apply();
            }
        }
    }

    private void onChooseEditEntitiesOrTileEntities(Dimension dim, int chunkXint, int chunkZint, View container, boolean isEntity) {
        WorldStorage storage = this.worldModel.getWorld().getStorage();
        if (storage == null) return;
        final Chunk chunk;
        try {
            chunk = storage.getChunk(chunkXint, chunkZint, dim);
        } catch (Exception e) {
            LogUtil.d(this, e);
            Toast.makeText(getContext(), R.string.error_could_not_open_world, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!chunkDataNBT(chunk, isEntity)) {
            Snackbar.make(container, String.format(getString(R.string.failed_to_load_x),
                    getString(isEntity ?
                            R.string.open_chunk_entity_nbt : R.string.open_chunk_tile_entity_nbt)),
                    Snackbar.LENGTH_LONG)
                    .show();
        }
    }

    private void onChooseAddMarker(int worldX, int worldZ, Activity activity, Dimension dim, View container) {
        View createMarkerForm = LayoutInflater.from(activity).inflate(R.layout.create_marker_form, null);

        final EditText markerNameInput = createMarkerForm.findViewById(R.id.marker_displayname_input);
        markerNameInput.setText(R.string.default_custom_marker_name);
        final EditText markerIconNameInput = createMarkerForm.findViewById(R.id.marker_iconname_input);
        markerIconNameInput.setText("blue_marker");
        final EditText xInput = createMarkerForm.findViewById(R.id.x_input);
        xInput.setText(String.valueOf(worldX));
        final EditText yInput = createMarkerForm.findViewById(R.id.y_input);
        yInput.setText(String.valueOf(64));
        final EditText zInput = createMarkerForm.findViewById(R.id.z_input);
        zInput.setText(String.valueOf(worldZ));


        new AlertDialog.Builder(activity)
                .setTitle(R.string.create_custom_marker)
                .setView(createMarkerForm)
                .setPositiveButton("Create marker", new DialogInterface.OnClickListener() {

                    void failParseSnackbarReport(int msg) {
                        Snackbar.make(container, msg,
                                Snackbar.LENGTH_LONG)
                                .show();
                    }

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        try {
                            String displayName = markerNameInput.getText().toString();
                            if (displayName.equals("") || displayName.contains("\"")) {
                                failParseSnackbarReport(R.string.marker_invalid_name);
                                return;
                            }
                            String iconName = markerIconNameInput.getText().toString();
                            if (iconName.equals("") || iconName.contains("\"")) {
                                failParseSnackbarReport(R.string.invalid_icon_name);
                                return;
                            }

                            int xM, yM, zM;

                            String xStr = xInput.getText().toString();
                            try {
                                xM = Integer.parseInt(xStr);
                            } catch (NumberFormatException e) {
                                failParseSnackbarReport(R.string.invalid_x_coordinate);
                                return;
                            }
                            String yStr = yInput.getText().toString();
                            try {
                                yM = Integer.parseInt(yStr);
                            } catch (NumberFormatException e) {
                                failParseSnackbarReport(R.string.invalid_y_coordinate);
                                return;
                            }

                            String zStr = zInput.getText().toString();
                            try {
                                zM = Integer.parseInt(zStr);
                            } catch (NumberFormatException e) {
                                failParseSnackbarReport(R.string.invalid_z_coordinate);
                                return;
                            }

                            AbstractMarker marker = MarkerManager.markerFromData(displayName, iconName, xM, yM, zM, dim);
                            /* fixme MarkerManager mng = world.getMarkerManager();
                            mng.addMarker(marker, true);

                            MapFragment.this.addMarker(marker);

                            mng.save();*/

                        } catch (Exception e) {
                            e.printStackTrace();
                            failParseSnackbarReport(R.string.failed_to_create_marker);
                        }
                    }
                })
                .setCancelable(true)
                .setNegativeButton(android.R.string.cancel, null)
                .show();


        return;
    }

    private void onChooseTeleportPlayer(float worldX, float worldZ, Dimension dim, View container) {
        /*fixme try {
            Activity activity = getActivity();
            assert activity != null;

            final EditableNBT playerEditable = worldProvider.get().getEditablePlayer();
            if (playerEditable == null)
                throw new Exception("Player is null");

            Iterator playerIter = playerEditable.getTags().iterator();
            if (!playerIter.hasNext())
                throw new Exception("Player DB entry is empty!");

            //db entry consists of one compound tag
            final CompoundTag playerTag = (CompoundTag) playerIter.next();

            ListTag posVec = (ListTag) playerTag.getChildTagByKey("Pos");

            if (posVec == null) throw new Exception("No \"Pos\" specified");

            final List<Tag> playerPos = posVec.getValue();
            if (playerPos == null)
                throw new Exception("No \"Pos\" specified");
            if (playerPos.size() != 3)
                throw new Exception("\"Pos\" value is invalid. value: " + posVec.getValue().toString());

            final IntTag dimensionId = (IntTag) playerTag.getChildTagByKey("DimensionId");
            if (dimensionId == null || dimensionId.getValue() == null)
                throw new Exception("No \"DimensionId\" specified");


            float playerY = (float) playerPos.get(1).getValue();


            View yForm = LayoutInflater.from(activity).inflate(R.layout.y_coord_form, null);
            final EditText yInput = yForm.findViewById(R.id.y_input);
            yInput.setText(String.valueOf(playerY));

            final float newX = worldX;
            final float newZ = worldZ;

            new AlertDialog.Builder(activity)
                    .setTitle(R.string.teleport_local_player)
                    .setView(yForm)
                    .setPositiveButton(R.string.action_teleport, (dialog1, which1) -> {

                        float newY;
                        Editable value = yInput.getText();
                        if (value == null) newY = 64f;
                        else {
                            try {
                                newY = Float.parseFloat(value.toString());//removes excessive precision
                            } catch (Exception e) {
                                newY = 64f;
                            }
                        }

                        ((FloatTag) playerPos.get(0)).setValue(newX);
                        ((FloatTag) playerPos.get(1)).setValue(newY);
                        ((FloatTag) playerPos.get(2)).setValue(newZ);
                        dimensionId.setValue(dim.id);

                        if (playerEditable.save()) {

                            MapFragment.this.localPlayerMarker = MapFragment.this.moveMarker(
                                    MapFragment.this.localPlayerMarker,
                                    (int) newX, (int) newY, (int) newZ, dim);

                            Snackbar.make(container,
                                    getString(R.string.teleported_player_to_xyz_dim, newX, newY, newZ),
                                    Snackbar.LENGTH_LONG)
                                    .show();
                        } else {
                            Snackbar.make(container,
                                    R.string.failed_teleporting_player,
                                    Snackbar.LENGTH_LONG)
                                    .show();
                        }
                    })
                    .setCancelable(true)
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();

        } catch (Exception e) {
            e.printStackTrace();
            Snackbar.make(container, R.string.failed_to_find_or_edit_local_player_data,
                    Snackbar.LENGTH_LONG)
                    .show();
        }*/
    }

    /**
     * Opens the chunk data nbt editor.
     *
     * @param chunk  the chunk to edit
     * @param entity if it is entity data (True) or oldBlock-entity data (False)
     * @return false when the chunk data could not be loaded.
     */
    private boolean chunkDataNBT(Chunk chunk, boolean entity) {
        NBTChunkData chunkData;
        try {
            chunkData = entity ? chunk.getEntity() : chunk.getBlockEntity();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        //just open the editor if the data is there for us to edit it
        //this.worldProvider.get().openChunkNBTEditor(chunk.mChunkX, chunk.mChunkZ, chunkData, mBinding.tileView);fixme
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBinding != null) {
            mBinding.tileView.destroy();
        }
        // TODO: should we...👇
        //mBinding.tileView = null;
        minecraftTileProvider = null;
    }

    public void openMarkerFilter() {

        final Activity activity = this.getActivity();


        final List<BitmapChoiceListAdapter.NamedBitmapChoice> choices = new ArrayList<>(markerFilter.values());

        //sort on names, nice for the user.
        Collections.sort(choices, (a, b) -> a.namedBitmap.getNamedBitmapProvider().getBitmapDisplayName().compareTo(b.namedBitmap.getNamedBitmapProvider().getBitmapDisplayName()));


        new AlertDialog.Builder(activity)
                .setTitle(R.string.filter_markers)
                .setAdapter(new BitmapChoiceListAdapter(activity, choices), null)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                    //save all the temporary states.
                    for (BitmapChoiceListAdapter.NamedBitmapChoice choice : choices) {
                        choice.enabled = choice.enabledTemp;
                    }
                    MapFragment.this.updateMarkerFilter();
                })
                .setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> {
                    //reset all the temporary states.
                    for (BitmapChoiceListAdapter.NamedBitmapChoice choice : choices) {
                        choice.enabledTemp = choice.enabled;
                    }
                })
                .setOnCancelListener(dialogInterface -> {
                    //reset all the temporary states.
                    for (BitmapChoiceListAdapter.NamedBitmapChoice choice : choices) {
                        choice.enabledTemp = choice.enabled;
                    }
                })
                .show();
    }

    public void updateMarkerFilter() {
        for (AbstractMarker marker : this.proceduralMarkers) {
            filterMarker(marker);
        }
    }

    public void filterMarker(AbstractMarker marker) {
        BitmapChoiceListAdapter.NamedBitmapChoice choice = markerFilter.get(marker.getNamedBitmapProvider());
        if (choice != null) {
            marker.getView(this.getActivity()).setVisibility((choice.enabled && marker.dimension == this.model.getDimension()) ? View.VISIBLE : View.INVISIBLE);
        } else {
            marker.getView(this.getActivity()).setVisibility(marker.dimension == this.model.getDimension() ? View.VISIBLE : View.INVISIBLE);
        }
    }

    public void refreshAfterEdit() {
        mBinding.tileView.getTileCanvasViewGroup().clear();
        mBinding.tileView.getTileCanvasViewGroup().requiredBeginRenderTask();
    }

    public void resetTileView() {
        updateMarkerFilter();
        mBinding.tileView.getDetailLevelManager().setLevelType(this.model.getMapType().getValue());
        invalidateTileView();
    }

    public void invalidateTileView() {
        var manager = mBinding.tileView.getDetailLevelManager();
        var type = this.model.getMapType().getValue();
        //just swap mapType twice; it is not rendered, but it invalidates all tiles.
        manager.setLevelType(MapType.CHESS);
        manager.setLevelType(type);
        //all tiles will now reload as soon as the tileView is drawn (user scrolls -> redraw)
    }

    /**
     * This is a convenience method to scrollToAndCenter after layout (which won't happen if called directly in onCreate
     * see https://github.com/moagrius/TileView/wiki/FAQ
     */
    public void frameTo(final double worldX, final double worldZ) {
        mBinding.tileView.post(() -> mBinding.tileView.scrollToAndCenter(
                worldX / (double) MCTileProvider.HALF_WORLDSIZE,
                worldZ / (double) MCTileProvider.HALF_WORLDSIZE
        ));
    }

    public enum MarkerTapOption {

        TELEPORT_LOCAL_PLAYER(R.string.teleport_local_player),
        REMOVE_MARKER(R.string.remove_custom_marker);

        public final int stringId;

        MarkerTapOption(int id) {
            this.stringId = id;
        }

    }

    public enum LongClickOption {

        TELEPORT_LOCAL_PLAYER(R.string.teleport_local_player, null),
        CREATE_MARKER(R.string.create_custom_marker, null),
        //TODO TELEPORT_MULTI_PLAYER("Teleport other player", null),
        ENTITY(R.string.open_chunk_entity_nbt, ChunkTag.ENTITY),
        TILE_ENTITY(R.string.open_chunk_tile_entity_nbt, ChunkTag.BLOCK_ENTITY),
        BEGIN_SELECTION(R.string.func_begin_selection, null);

        public final int stringId;
        public final ChunkTag dataType;

        LongClickOption(int id, ChunkTag dataType) {
            this.stringId = id;
            this.dataType = dataType;
        }

    }

    public static class MarkerListAdapter extends ArrayAdapter<AbstractMarker> {


        MarkerListAdapter(Context context, List<AbstractMarker> objects) {
            super(context, 0, objects);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {

            RelativeLayout v = (RelativeLayout) convertView;

            if (v == null) {
                LayoutInflater vi;
                vi = LayoutInflater.from(getContext());
                v = (RelativeLayout) vi.inflate(R.layout.marker_list_entry, parent, false);
            }

            AbstractMarker m = getItem(position);

            if (m != null) {
                TextView name = v.findViewById(R.id.marker_name);
                TextView xz = v.findViewById(R.id.marker_xz);
                ImageView icon = v.findViewById(R.id.marker_icon);

                name.setText(m.getNamedBitmapProvider().getBitmapDisplayName());
                String xzStr = String.format(Locale.ENGLISH, "x: %d, y: %d, z: %d", m.x, m.y, m.z);
                xz.setText(xzStr);

                m.loadIcon(icon);

            }

            return v;
        }

    }

    public static class BitmapChoiceListAdapter extends ArrayAdapter<BitmapChoiceListAdapter.NamedBitmapChoice> {

        private CompoundButton.OnCheckedChangeListener changeListener = (compoundButton, b) -> {
            Object tag = compoundButton.getTag();
            if (tag == null) return;
            int position = (int) tag;
            final NamedBitmapChoice m = getItem(position);
            if (m == null) return;
            m.enabledTemp = b;
        };

        BitmapChoiceListAdapter(Context context, List<NamedBitmapChoice> objects) {
            super(context, 0, objects);
        }

        @NonNull
        @Override
        public View getView(final int position, View v, @NonNull ViewGroup parent) {

            final NamedBitmapChoice m = getItem(position);
            if (m == null) return new RelativeLayout(getContext());

            if (v == null) v = LayoutInflater
                    .from(getContext())
                    .inflate(R.layout.img_name_check_list_entry, parent, false);


            ImageView img = v.findViewById(R.id.entry_img);
            TextView text = v.findViewById(R.id.entry_text);
            final CheckBox check = v.findViewById(R.id.entry_check);

            img.setImageBitmap(m.namedBitmap.getNamedBitmapProvider().getBitmap());
            text.setText(m.namedBitmap.getNamedBitmapProvider().getBitmapDisplayName());
            check.setTag(position);
            check.setChecked(m.enabledTemp);
            check.setOnCheckedChangeListener(changeListener);

            return v;
        }

        static class NamedBitmapChoice {

            //Using a handle to facilitate bitmap swapping without breaking the filter.
            final NamedBitmapProviderHandle namedBitmap;
            boolean enabledTemp;
            boolean enabled;

            NamedBitmapChoice(NamedBitmapProviderHandle namedBitmap, boolean enabled) {
                this.namedBitmap = namedBitmap;
                this.enabled = this.enabledTemp = enabled;
            }
        }

    }
}
