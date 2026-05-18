package com.mithrilmania.blocktopograph;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.navigation.NavigationView;
import com.mithrilmania.blocktopograph.chunk.NBTChunkData;
import com.mithrilmania.blocktopograph.databinding.ActivityWorldBinding;
import com.mithrilmania.blocktopograph.editor.world.WorldMapModel;
import com.mithrilmania.blocktopograph.map.Dimension;
import com.mithrilmania.blocktopograph.map.MapFragment;
import com.mithrilmania.blocktopograph.map.renderer.MapType;
import com.mithrilmania.blocktopograph.util.SpecialDBEntryType;
import com.mithrilmania.blocktopograph.world.World;
import com.mithrilmania.blocktopograph.world.WorldKt;
import com.mithrilmania.blocktopograph.world.WorldModel;
import com.mithrilmania.blocktopograph.world.WorldModelKt;

public abstract class WorldActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, DrawerLayout.DrawerListener {

    public static final String PREF_KEY_SHOW_MARKERS = "showMarkers";
    private static final String TAG = WorldActivity.class.getSimpleName();
    protected ActivityWorldBinding mBinding;
    protected WorldMapModel model;
    protected WorldModel worldModel;
    protected MapFragment mapFragment;

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*
        Retrieve world from previous state or intent
         */
        LogUtil.d(this, "World activity creating...");
        WorldModel worldModel;
        try {
            worldModel = WorldModelKt.getOrCreateWorldModel(this);
        } catch (Exception e) {
            Log.e(TAG, "Failed to open world", e);
            Toast.makeText(this, "cannot open: world == null", Toast.LENGTH_SHORT).show();
            //WTF, try going back to the previous screen by finishing this hopeless activity...
            this.finish();
            //Finish does not guarantee codes below won't be executed!
            //Shit
            return;
        }
        World handler = worldModel.getWorld();
        worldModel.open(this);
        this.worldModel = worldModel;

        WorldMapModel model = new ViewModelProvider(this).get(WorldMapModel.class);
        this.model = model;
        model.getShowMarkers().setValue(getPreferences(MODE_PRIVATE).getBoolean(PREF_KEY_SHOW_MARKERS, true));

        /*
                Layout
         */
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_world);
        //Toolbar toolbar = mBinding.bar.toolbar;
        //assert toolbar != null;
        //setSupportActionBar(toolbar);

        NavigationView navigationView = mBinding.navView;
        assert navigationView != null;
        navigationView.setNavigationItemSelectedListener(this);

        // Main drawer, quick access to different menus, tools and map-types.
//        DrawerLayout drawer = mBinding.drawerLayout;
//        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
//                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
//        assert drawer != null;
//        drawer.addDrawerListener(toggle);
//        toggle.syncState();


        View headerView = navigationView.getHeaderView(0);
        assert headerView != null;

        // Title = world-name
        TextView title = headerView.findViewById(R.id.world_drawer_title);
        assert title != null;
        title.setText(handler.getPlainName());

        // Subtitle = world-seed (Tap worldseed to choose to copy it)
        TextView subtitle = headerView.findViewById(R.id.world_drawer_subtitle);
        assert subtitle != null;

        /*
            World-seed & world-name analytics.

            Send anonymous world data to the Firebase (Google analytics for Android) server.
            This data will be pushed to Google-BigQuery.
            Google-BigQuery will crunch the world-data,
              storing hundreds of thousands world-seeds + names.
            The goal is to automatically create a "Top 1000" popular seeds for every week.
            This "Top 1000" will be published as soon as it gets out of BigQuery,
             keep it for the sake of this greater goal. It barely uses internet bandwidth,
             and this makes any forced intrusive revenue alternatives unnecessary.

            TODO BigQuery is not configured yet,
             @mithrilmania (author of Blocktopograph) is working on it!

             Ahhh good idea anyway... Then why didn't you continue it.

            *link to results will be included here for reference when @mithrilmania is done*
         */
        subtitle.setText(String.valueOf(WorldKt.resolveSeed(handler, this)));

        // Open the world-map as default content
        openWorldMap();
        this.getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                DrawerLayout drawer = mBinding.drawerLayout;
                if (drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START);
                    return;
                }

                final FragmentManager manager = getSupportFragmentManager();
                int count = manager.getBackStackEntryCount();

                // No opened fragments, so it is using the default fragment
                // Ask the user if he/she wants to close the world.
                if (count == 0) {

                    new AlertDialog.Builder(WorldActivity.this)
                            .setMessage(R.string.ask_close_world)
                            .setCancelable(false)
                            .setPositiveButton(android.R.string.yes, (dialog, id) -> WorldActivity.this.finish())
                            .setNegativeButton(android.R.string.no, null)
                            .show();

                } else if (confirmContentClose != null) {
                    //An important fragment is opened,
                    // something that couldn't be reopened in its current state easily,
                    // ask the user if he/she intended to close it.
                    new AlertDialog.Builder(WorldActivity.this)
                            .setMessage(confirmContentClose)
                            .setCancelable(false)
                            .setPositiveButton(android.R.string.yes, (dialog, id) -> {
                                manager.popBackStack();
                                confirmContentClose = null;
                            })
                            .setNegativeButton(android.R.string.no, null)
                            .show();
                } else {
                    //fragment is open, but it may be closed without warning
                    manager.popBackStack();
                }
            }
        });
        this.model.getShowActionBar().observe(this, visible -> {
            ActionBar bar = this.getSupportActionBar();
            if (bar != null) {
                if (visible) {
                    bar.show();
                } else {
                    bar.hide();
                }
            }
        });
        this.model.getShowDrawer().observe(this, visible -> {
            if (visible) {
                mBinding.drawerLayout.openDrawer(mBinding.navView, true);
            } else {
                mBinding.drawerLayout.closeDrawer(mBinding.navView, false);
            }
        });
        mBinding.drawerLayout.addDrawerListener(this);
        LogUtil.d(this, "World activity created");
    }

    public abstract void openCustomEntry();

    /**
     * Open NBT editor fragment for special database entry
     */
    public abstract void openSpecialDBEntry(final SpecialDBEntryType entryType);

    public abstract void openMultiplayerEditor();

    public abstract void openLocalPlayer();

    public abstract void openLevelEditor();

    //TODO the dimension should be derived from mapTypes.
    // E.g. split xray into xray-overworld and xray-nether, but still use the same [MapRenderer],
    //  splitting allows to pass more sophisticated use of [MapRenderer]s
    private Dimension dimension = Dimension.OVERWORLD;

    public Dimension getDimension() {
        return this.dimension;
    }

    private MapType mapType = dimension.defaultMapType;

    public MapType getMapType() {
        return this.mapType;
    }


    // TODO grid should be rendered independently of tiles, it could be faster and more responsive.
    // However, it does need to adjust itself to the scale and position of the map,
    //  which is not an easy task.

    public void closeWorldActivity() {

        //TODO not translation-friendly

        new AlertDialog.Builder(this)
                .setMessage(R.string.confirm_close_world)
                .setCancelable(false)
                .setIcon(R.drawable.ic_action_exit)
                .setPositiveButton(android.R.string.yes,
                        (dialog, id) -> {
                            //finish this activity
                            mapFragment.closeChunks();
                            finish();
                        })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    @Override
    public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
    }

    @Override
    public void onDrawerOpened(@NonNull View drawerView) {
        this.model.getShowDrawer().setValue(true);
    }

    @Override
    public void onDrawerClosed(@NonNull View drawerView) {
        this.model.getShowDrawer().setValue(false);
    }

    @Override
    public void onDrawerStateChanged(int newState) {
    }

    public String confirmContentClose = null;

    public void changeContentFragment(final Runnable callback) {

        final FragmentManager manager = getSupportFragmentManager();

        // confirmContentClose shouldn't be both used as boolean and as close-message,
        //  this is a bad pattern
        if (confirmContentClose != null) {
            new AlertDialog.Builder(this)
                    .setMessage(confirmContentClose)
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.yes,
                            (dialog, id) -> {
                                manager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                                callback.run();
                            })
                    .setNegativeButton(android.R.string.no, null)
                    .show();
        } else {
            manager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            callback.run();
        }

    }

    /**
     * Replace current content fragment with a fresh MapFragment
     */
    public void openWorldMap() {

        //TODO should this use cached world-position etc.?

        this.confirmContentClose = null;
        this.mapFragment = new MapFragment();

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.world_content, this.mapFragment);
        transaction.commit();

    }


    /**
     * Open a dialog; user chooses chunk-type -> open editor for this type
     **/
    public void openChunkNBTEditor(final int chunkX, final int chunkZ, final NBTChunkData nbtChunkData, final ViewGroup viewGroup) {
        /*if (nbtChunkData == null) {
            //should never happen
            Log.e(TAG, "User tried to open null chunkData in the nbt-editor!!!");
            return;
        }


        try {
            nbtChunkData.load();
        } catch (Exception e) {
            Snackbar.make(viewGroup, this.getString(R.string.failed_to_load_x, this.getString(R.string.nbt_chunk_data)), Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            return;
        }

        final List<Tag> tags = nbtChunkData.tags;
        if (tags == null) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.nbt_editor)
                    .setMessage(R.string.data_does_not_exist_for_chunk_ask_if_create)
                    .setIcon(R.drawable.ic_action_save_b)
                    .setPositiveButton(android.R.string.yes,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    nbtChunkData.createEmpty();
                                    try {
                                        nbtChunkData.write();
                                        Snackbar.make(viewGroup, R.string.created_and_saved_chunk_NBT_data, Snackbar.LENGTH_LONG)
                                                .setAction("Action", null).show();
                                        //WorldActivity.this.openChunkNBTEditor(chunkX, chunkZ, nbtChunkData, viewGroup);fixme
                                    } catch (Exception e) {
                                        Snackbar.make(viewGroup, R.string.failed_to_create_or_save_chunk_NBT_data, Snackbar.LENGTH_LONG)
                                                .setAction("Action", null).show();
                                        LogUtil.d(this, e);
                                    }
                                }
                            })
                    .setNegativeButton(android.R.string.no, null)
                    .show();
            return;
        }


        //open nbt editor for entity data
        changeContentFragment(() -> {

            //make a copy first, the user might not want to save changed tags.
            final List<Tag> workCopy = new ArrayList<>();
            for (Tag tag : tags) {
                workCopy.add(tag.getDeepCopy());
            }

            final EditableNBT editableChunkData = new EditableNBT() {

                @Override
                public Iterable<Tag> getTags() {
                    return workCopy;
                }

                @Override
                public boolean save() {
                    try {
                        final List<Tag> saveCopy = new ArrayList<>();
                        for (Tag tag : workCopy) {
                            saveCopy.add(tag.getDeepCopy());
                        }
                        nbtChunkData.tags = saveCopy;
                        nbtChunkData.write();
                        return true;
                    } catch (Exception e) {
                        Log.e("OldEditorImpl", "Failed to save data", e);
                    }
                    return false;
                }

                @Override
                public String getRootTitle() {
                    final String format = "%s (cX:%d;cZ:%d)";
                    return switch ((nbtChunkData).dataType) {
                        case ENTITY ->
                                String.format(format, getString(R.string.entity_chunk_data), chunkX, chunkZ);
                        case BLOCK_ENTITY ->
                                String.format(format, getString(R.string.tile_entity_chunk_data), chunkX, chunkZ);
                        default ->
                                String.format(format, getString(R.string.nbt_chunk_data), chunkX, chunkZ);
                    };
                }

                @Override
                public void addRootTag(Tag tag) {
                    workCopy.add(tag);
                }

                @Override
                public void removeRootTag(Tag tag) {
                    workCopy.remove(tag);
                }
            };

            //openNBTEditor(editableChunkData);
        });*/
    }
}