package net.alegen.android.netclip.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;

import android.content.res.Configuration;

import android.os.Bundle;

import android.util.DisplayMetrics;
import android.util.Log;

import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBarDrawerToggle;

import java.lang.String;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.alegen.android.netclip.netio.CommunicationsManager;
import net.alegen.android.netclip.netio.ConnectionsManager;

import net.alegen.android.netclip.R;

/*
 * Relevant but don`t know where to save this yet:
 * https://superuser.com/questions/346958/can-the-telnet-or-netcat-clients-communicate-over-ssl
 */

public class MainActivity extends AppCompatActivity {

    private static int currWidth;
    private static int currHeight;
    private static int currOrientation;

    public static int getCurrWidth() {
        return currWidth;
    }

    public static int getCurrHeight() {
        return currHeight;
    }

    public static int getCurrOrientation() {
        return currOrientation;
    }

    private String activityTitle;
    private List<String> receivedText;

    private Fragment currentFragment;
    private ConnectionsFragment connectionsFragment;
    private ReceivedTextFragment receivedTextFragment;

    private ActionBarDrawerToggle sideMenuToggle;
    private ArrayAdapter<String> adapter;
    private DrawerLayout sideMenuLayout;
    private FragmentManager fragmentManager;
    private ListView sideMenu;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i("netclip", "MainActivity.onCreate");
        super.onCreate(savedInstanceState);

        this.activityTitle = "Received text";
        this.receivedText = new ArrayList<String>();

        // hide notification window
        // https://stackoverflow.com/questions/2591036
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // get visible size of the activity
        // https://stackoverflow.com/questions/3060619
        // https://stackoverflow.com/questions/20142133
        DisplayMetrics dm = new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(dm);
        currWidth = dm.widthPixels;
        currHeight = dm.heightPixels;
        currOrientation = this.getResources().getConfiguration().orientation;

        this.getSupportActionBar().setTitle(this.activityTitle);
        setContentView(R.layout.activity_main);

        this.initSideMenu();
        this.enableSideMenuToggling();
        this.fragmentManager = this.getFragmentManager();

        // initialize managers if needed
        ConnectionsManager.getInstance();
        CommunicationsManager.getInstance();

        this.connectionsFragment = new ConnectionsFragment();
        this.receivedTextFragment = new ReceivedTextFragment();

        FragmentTransaction fragmentTransaction = this.fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.main_ui, this.connectionsFragment);
        fragmentTransaction.add(R.id.main_ui, this.receivedTextFragment);
        fragmentTransaction.hide(this.connectionsFragment);
        fragmentTransaction.show(this.receivedTextFragment);
        fragmentTransaction.commit();
        this.currentFragment = this.receivedTextFragment;
    }

    @Override
    public void onDestroy() {
        Log.i("netclip", "MainActivity.onDestroy");
        super.onDestroy();
    }

    private void initSideMenu() {
        List<String> options = new ArrayList<String>( Arrays.asList( new String[]{
            "Received text",    // 0
            "Connections",      // 1
            "Send text",        // 2
            "Help",             // 3
            "About"             // 4
        }));

        this.adapter = new ArrayAdapter<String>(this, R.layout.sidemenu_textview, options);
        this.sideMenu = (ListView)this.findViewById(R.id.side_menu);
        this.sideMenu.setAdapter(this.adapter);

        this.sideMenu.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                FragmentTransaction fragmentTransaction = MainActivity.this.fragmentManager.beginTransaction();
                fragmentTransaction.hide(MainActivity.this.currentFragment);
                if (position == 0) {
                    MainActivity.this.activityTitle = "Received text";
                    MainActivity.this.currentFragment = MainActivity.this.receivedTextFragment;
                } else if (position == 1) {
                    MainActivity.this.activityTitle = "Connections";
                    MainActivity.this.currentFragment = MainActivity.this.connectionsFragment;
                }
                fragmentTransaction.show(MainActivity.this.currentFragment);
                fragmentTransaction.commit();
                MainActivity.this.sideMenuLayout.closeDrawer(Gravity.LEFT);
                MainActivity.this.getSupportActionBar().setTitle(MainActivity.this.activityTitle);
            }
        });
    }

    private void enableSideMenuToggling() {
        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        this.getSupportActionBar().setHomeButtonEnabled(true);
        this.sideMenuLayout = (DrawerLayout)this.findViewById(R.id.side_menu_layout);

        this.sideMenuToggle = new ActionBarDrawerToggle(
            this,
            this.sideMenuLayout,
            R.string.side_menu_open,
            R.string.side_menu_close
        ) {
            // called when a drawer has settled in a completely open state
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                MainActivity.this.getSupportActionBar().setTitle("Options");
                MainActivity.this.invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            // called when a drawer has settled in a completely closed state
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                MainActivity.this.getSupportActionBar().setTitle(MainActivity.this.activityTitle);
                MainActivity.this.invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        this.sideMenuToggle.setDrawerIndicatorEnabled(true);
        this.sideMenuLayout.setDrawerListener(this.sideMenuToggle);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (this.sideMenuToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        this.sideMenuToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.sideMenuToggle.onConfigurationChanged(newConfig);
    }
}
