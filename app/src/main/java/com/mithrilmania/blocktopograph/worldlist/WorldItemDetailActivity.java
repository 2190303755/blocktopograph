package com.mithrilmania.blocktopograph.worldlist;


import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.mithrilmania.blocktopograph.R;
import com.mithrilmania.blocktopograph.view.WorldModel;

/**
 * An activity representing a single WorldItem detail screen. This
 * activity is only used narrow width devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a {@link OldWorldItemListActivity}.
 */
public class WorldItemDetailActivity extends AppCompatActivity {
    private WorldModel world;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worlditem_detail);
        WorldModel model = new ViewModelProvider(this).get(WorldModel.class);
        if (model.getInstance() == null) {
            try {
                model.init(this, this.getIntent().getData());
            } catch (Exception e) {
                Toast.makeText(this, "cannot open: world == null", Toast.LENGTH_SHORT).show();
                this.finish();
                return;
            }
        }
        this.world = model;
        Toolbar toolbar = (Toolbar) findViewById(R.id.detail_toolbar);
        setSupportActionBar(toolbar);
        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // savedInstanceState is non-null when there is fragment state
        // saved from previous configurations of this activity
        // (e.g. when rotating the screen from portrait to landscape).
        // In this case, the fragment will automatically be re-added
        // to its container so we don't need to manually add it.
        // For more information, see the Fragments API guide at:
        //
        // http://developer.android.com/guide/components/fragments.html
        //
        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.
            WorldItemDetailFragment fragment = new WorldItemDetailFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.worlditem_detail_container, fragment)
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            navigateUpTo(new Intent(this, OldWorldItemListActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

