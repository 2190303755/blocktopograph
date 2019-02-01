package com.mithrilmania.blocktopograph.worldlist;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.mithrilmania.blocktopograph.CreateWorldActivity;
import com.mithrilmania.blocktopograph.Log;
import com.mithrilmania.blocktopograph.R;
import com.mithrilmania.blocktopograph.World;
import com.mithrilmania.blocktopograph.util.io.IOUtil;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class WorldItemListActivity extends AppCompatActivity {

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 4242;
    private static final String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    public static final int REQUEST_CODE_CREATE_WORLD = 2012;
    /**
     * Whether or not the activity is in two-pane mode, d.e. running on a tablet.
     */
    private boolean mTwoPane;
    private WorldItemRecyclerViewAdapter worldItemAdapter;

    /**
     * Checks if the app has permission to write to device storage
     * <p>
     * If the app does not has permission then the user will be prompted to grant permissions
     * </p>
     */
    public static boolean verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
            return false;
        } else return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_worldlist);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());
        setSupportActionBar(toolbar);

        if (findViewById(R.id.worlditem_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }

        FloatingActionButton fabChooseWorldFile = findViewById(R.id.fab_create);
        fabChooseWorldFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickCreateWorld();
            }
        });

        RecyclerView recyclerView = findViewById(R.id.worlditem_list);
        worldItemAdapter = new WorldItemRecyclerViewAdapter();
        recyclerView.setAdapter(this.worldItemAdapter);

        if (verifyStoragePermissions(this)) {
            //directly open the world list if we already have access
            worldItemAdapter.enable();
        }


    }

    private void onClickCreateWorld() {
        if (worldItemAdapter.isDisabled()) {
            Snackbar.make(getWindow().getDecorView(), R.string.no_read_write_access, Snackbar.LENGTH_SHORT).show();
            return;
        }
        startActivityForResult(new Intent(this, CreateWorldActivity.class), REQUEST_CODE_CREATE_WORLD);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_CREATE_WORLD:
                    worldItemAdapter.loadWorldList();
                    return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay!
                    this.worldItemAdapter.enable();

                } else {

                    // permission denied, boo! Disable the
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    TextView msg = new TextView(this);
                    float dpi = this.getResources().getDisplayMetrics().density;
                    msg.setPadding((int) (19 * dpi), (int) (5 * dpi), (int) (14 * dpi), (int) (5 * dpi));
                    msg.setMaxLines(20);
                    msg.setMovementMethod(LinkMovementMethod.getInstance());
                    msg.setText(R.string.no_sdcard_access);
                    builder.setView(msg)
                            .setTitle(R.string.action_help)
                            .setCancelable(true)
                            .setNeutralButton(android.R.string.ok, null)
                            .show();
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.world, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        //some text pop-up dialogs, some with simple HTML tags.
        switch (item.getItemId()) {
            case R.id.action_open: {
                if (worldItemAdapter.isDisabled()) {
                    Snackbar.make(getWindow().getDecorView(), R.string.no_read_write_access, Snackbar.LENGTH_SHORT).show();
                    return true;
                }
                final EditText pathText = new EditText(WorldItemListActivity.this);
                pathText.setHint(R.string.storage_path_here);

                AlertDialog.Builder alert = new AlertDialog.Builder(WorldItemListActivity.this);
                alert.setTitle(R.string.open_world_custom_path);
                alert.setView(pathText);

                alert.setPositiveButton(R.string.open, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        //new tag name
                        Editable pathEditable = pathText.getText();
                        String path = (pathEditable == null || pathEditable.toString().equals("")) ? null : pathEditable.toString();
                        if (path == null) {
                            return;//no path, no world
                        }

                        String levelDat = "/level.dat";
                        int levelIndex = path.lastIndexOf(levelDat);
                        //if the path ends with /level.dat, remove it!
                        if (levelIndex >= 0 && path.endsWith(levelDat))
                            path = path.substring(0, levelIndex);

                        String defaultPath = Environment.getExternalStorageDirectory().toString() + "/games/com.mojang/minecraftWorlds/";
                        File worldFolder = new File(path);
                        String errTitle = null, errMsg = String.format(getString(R.string.report_path_and_previous_search), path, defaultPath);
                        if (!worldFolder.exists()) {
                            errTitle = getString(R.string.no_file_folder_found_at_path);
                        }
                        if (!worldFolder.isDirectory()) {
                            errTitle = getString(R.string.worldpath_is_not_directory);
                        }
                        if (!(new File(worldFolder, "level.dat").exists())) {
                            errTitle = getString(R.string.no_level_dat_found);
                        }
                        if (errTitle != null) {
                            new AlertDialog.Builder(WorldItemListActivity.this)
                                    .setTitle(errTitle)
                                    .setMessage(errMsg)
                                    .setCancelable(true)
                                    .setPositiveButton(android.R.string.ok, null)
                                    .show();
                        } else {

                            try {
                                World world = new World(worldFolder, null);

                                if (mTwoPane) {
                                    Bundle arguments = new Bundle();
                                    arguments.putSerializable(World.ARG_WORLD_SERIALIZED, world);
                                    WorldItemDetailFragment fragment = new WorldItemDetailFragment();
                                    fragment.setArguments(arguments);
                                    WorldItemListActivity.this.getSupportFragmentManager().beginTransaction()
                                            .replace(R.id.worlditem_detail_container, fragment)
                                            .commit();
                                } else {
                                    Intent intent = new Intent(WorldItemListActivity.this, WorldItemDetailActivity.class);
                                    intent.putExtra(World.ARG_WORLD_SERIALIZED, world);

                                    WorldItemListActivity.this.startActivity(intent);
                                }

                            } catch (Exception e) {
                                Snackbar.make(getWindow().getDecorView(), R.string.error_opening_world, Snackbar.LENGTH_SHORT)
                                        .setAction("Action", null).show();
                            }
                        }

                    }
                });

                alert.setCancelable(true);

                //or alert is cancelled
                alert.setNegativeButton(android.R.string.cancel, null);

                alert.show();

                //TODO: browse for custom located world file, not everybody understands filesystem paths
                //Then it also cannot find a world. --rbq2012.
                return true;
            }
            case R.id.action_about: {

                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                TextView msg = new TextView(this);
                msg.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                float dpi = getResources().getDisplayMetrics().density;
                msg.setPadding((int) (19 * dpi), (int) (5 * dpi), (int) (14 * dpi), (int) (5 * dpi));
                msg.setMaxLines(20);
                msg.setMovementMethod(LinkMovementMethod.getInstance());
                msg.setText(R.string.app_about);
                builder.setView(msg)
                        .setTitle(R.string.action_about)
                        .setCancelable(true)
                        .setNeutralButton(android.R.string.ok, null)
                        .show();

                return true;
            }
            case R.id.action_help: {
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                TextView msg = new TextView(this);
                float dpi = getResources().getDisplayMetrics().density;
                msg.setPadding((int) (19 * dpi), (int) (5 * dpi), (int) (14 * dpi), (int) (5 * dpi));
                msg.setMaxLines(20);
                msg.setMovementMethod(LinkMovementMethod.getInstance());
                msg.setText(R.string.app_help);
                builder.setView(msg)
                        .setTitle(R.string.action_help)
                        .setCancelable(true)
                        .setNeutralButton(android.R.string.ok, null)
                        .show();

                return true;
            }
//            case R.id.action_changelog: {
//                AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
//                TextView msg = new TextView(ctx);
//                float dpi = ctx.getResources().getDisplayMetrics().density;
//                msg.setPadding((int) (19 * dpi), (int) (5 * dpi), (int) (14 * dpi), (int) (5 * dpi));
//                msg.setMaxLines(20);
//                msg.setMovementMethod(LinkMovementMethod.getInstance());
//                String content = String.format(ctx.getResources().getString(R.string.app_changelog), BuildConfig.VERSION_NAME);
//                //noinspection deprecation
//                msg.setText(Html.fromHtml(content));
//                builder.setView(msg)
//                        .setTitle(R.string.action_changelog)
//                        .setCancelable(true)
//                        .setNeutralButton(android.R.string.ok, null)
//                        .show();
//
//                return true;
//            }
            default: {
                return false;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        worldItemAdapter.loadWorldList();
    }

    public class WorldItemRecyclerViewAdapter extends RecyclerView.Adapter<WorldItemRecyclerViewAdapter.ViewHolder> {

        private final List<World> mWorlds;

        private boolean disabled;

        WorldItemRecyclerViewAdapter() {
            mWorlds = new ArrayList<>(16);
            disabled = true;
        }

        void enable() {
            disabled = false;
        }

        public boolean isDisabled() {
            return disabled;
        }

        //returns true if it has loaded a new list of worlds, false otherwise
        void loadWorldList() {
            if (disabled) return;
            mWorlds.clear();
            List<File> saveFolders;
            List<String> marks;
            saveFolders = new ArrayList<>(4);
            marks = new ArrayList<>(4);

            File sd = Environment.getExternalStorageDirectory();

            saveFolders.add(new File(sd, "games/com.mojang/minecraftWorlds"));
            marks.add(null);

            File[] datas = new File(sd, "Android/data").listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File file, String s) {
                    return s.startsWith("com.netease");
                }
            });

            if (datas != null) for (File f : datas) {
                File ff = new File(f, "files/minecraftWorlds");
                if (ff.exists()) {
                    saveFolders.add(ff);
                    marks.add(getString(R.string.world_mark_neteas));
                }
            }

            for (int i = 0, saveFoldersSize = saveFolders.size(); i < saveFoldersSize; i++) {
                File dir = saveFolders.get(i);
                File[] files = dir.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        if (!file.isDirectory()) return false;
                        return new File(file, "level.dat").exists();
                    }
                });
                if (files != null) for (File f : files) {
                    try {
                        mWorlds.add(new World(f, marks.get(i)));
                    } catch (World.WorldLoadException e) {
                        Log.d(this, e);
                    }
                }
            }

            Collections.sort(mWorlds, new Comparator<World>() {
                @Override
                public int compare(World a, World b) {
                    try {
                        long tA = WorldListUtil.getLastPlayedTimestamp(a);
                        long tB = WorldListUtil.getLastPlayedTimestamp(b);
                        return Long.compare(tB, tA);
                    } catch (Exception e) {
                        Log.d(this, e);
                        return 0;
                    }
                }
            });

            //load data into view
            this.notifyDataSetChanged();

            if (mWorlds.size() == 0) {
                AlertDialog dia = new AlertDialog.Builder(WorldItemListActivity.this)
                        .setTitle(R.string.err_noworld_1)
                        .setView(R.layout.dialog_noworlds)
                        .create();
                dia.show();
            }

        }


        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.worlditem_list_content, parent, false);
            return new ViewHolder(view);
        }


        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
            holder.mWorld = mWorlds.get(position);
            holder.mWorldNameView.setText(holder.mWorld.getWorldDisplayName());
            holder.mWorldSize.setText(IOUtil.getFileSizeInText(holder.mWorld.worldFolder));
            holder.mWorldGamemode.setText(WorldListUtil.getWorldGamemodeText(WorldItemListActivity.this, holder.mWorld));
            holder.mWorldLastPlayed.setText(WorldListUtil.getLastPlayedText(WorldItemListActivity.this, holder.mWorld));
            holder.mWorldPath.setText(holder.mWorld.worldFolder.getName());
            holder.mWorldMark.setText(holder.mWorld.mark);


            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mTwoPane) {
                        Bundle arguments = new Bundle();
                        arguments.putSerializable(World.ARG_WORLD_SERIALIZED, holder.mWorld);
                        WorldItemDetailFragment fragment = new WorldItemDetailFragment();
                        fragment.setArguments(arguments);
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.worlditem_detail_container, fragment)
                                .commit();
                    } else {
                        Context context = v.getContext();
                        Intent intent = new Intent(context, WorldItemDetailActivity.class);
                        intent.putExtra(World.ARG_WORLD_SERIALIZED, holder.mWorld);

                        context.startActivity(intent);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return mWorlds.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final View mView;
            final TextView mWorldNameView;
            final TextView mWorldMark;
            final TextView mWorldSize;
            final TextView mWorldGamemode;
            final TextView mWorldLastPlayed;
            final TextView mWorldPath;
            World mWorld;

            ViewHolder(View view) {
                super(view);
                mView = view;
                mWorldNameView = view.findViewById(R.id.world_name);
                mWorldMark = view.findViewById(R.id.world_mark);
                mWorldSize = view.findViewById(R.id.world_size);
                mWorldGamemode = view.findViewById(R.id.world_gamemode);
                mWorldLastPlayed = view.findViewById(R.id.world_last_played);
                mWorldPath = view.findViewById(R.id.world_path);
            }

            @Override
            public String toString() {
                return super.toString() + " '" + mWorldNameView.getText() + "'";
            }
        }
    }
}

