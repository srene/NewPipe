/* -*- Mode:jde; c-file-style:"gnu"; indent-tabs-mode:nil; -*- */
/**
 * Copyright (c) 2015 Regents of the University of California
 *
 * This file is part of NFD (Named Data Networking Forwarding Daemon) Android.
 * See AUTHORS.md for complete list of NFD Android authors and contributors.
 *
 * NFD Android is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * NFD Android is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * NFD Android, e.g., in COPYING.md file.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.schabi.newpipe;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

/*import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;
import com.intel.jndn.management.types.FaceStatus;
import com.intel.jndn.management.types.RibEntry;*/

import org.schabi.newpipe.fragments.list.kiosk.KioskFragment;

import java.util.ArrayList;

//import io.fluentic.ubicdn.utils.G;

//import android.support.v7.app.ActionBarActivity;


/**
 * Created by srenevic on 03/08/17.
 */
public class MainActivity extends AppCompatActivity
    implements DrawerFragment.DrawerCallbacks/*,
        //       LogcatFragment.Callbacks,
               FaceListFragment.Callbacks,
               RouteListFragment.Callbacks,
               VideoListFragment.Callbacks*/
{

    //////////////////////////////////////////////////////////////////////////////

    //private boolean source=false;
    /** Reference to drawer fragment */
    private DrawerFragment m_drawerFragment;


    /** Title that is to be displayed in the ActionBar */
    private int m_actionBarTitleId = -1;

    /** Item code for drawer items: For use in onDrawerItemSelected() callback */
    public static final int DRAWER_ITEM_GENERAL = 1;
    public static final int DRAWER_ITEM_NFD = 2;
    public static final int DRAWER_ITEM_FACES = 3;
    public static final int DRAWER_ITEM_ROUTES = 4;
    // public static final int DRAWER_ITEM_STRATEGIES = 4;
    public static final int DRAWER_ITEM_LOGCAT = 5;

    private ProgressDialog mProgressDialog;
    private static final String TAG = "MainActivity";
    //private FirebaseAuth mAuth;

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
  //  FirebaseMessaging.getInstance().subscribeToTopic("news");
    Log.d("Main", "subscribed to topic news");
    FragmentManager fragmentManager = getSupportFragmentManager();
  //  mAuth = FirebaseAuth.getInstance();

       Toolbar toolbar = findViewById(R.id.toolbar);
       setSupportActionBar(toolbar);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
          //  builder.setTitle("This app needs location access");
          //  builder.setMessage("Please grant location access");
          //  builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                }
            });
            builder.show();
        }
    }

    if (savedInstanceState != null) {
      m_drawerFragment = (DrawerFragment)fragmentManager.findFragmentByTag(DrawerFragment.class.toString());
    }

    if (m_drawerFragment == null) {
      ArrayList<DrawerFragment.DrawerItem> items = new ArrayList<DrawerFragment.DrawerItem>();
      items.add(new DrawerFragment.DrawerItem(R.string.drawer_item_general, 0,
                                             DRAWER_ITEM_GENERAL));
      items.add(new DrawerFragment.DrawerItem(R.string.drawer_item_service, 0,
                                              DRAWER_ITEM_NFD));
      items.add(new DrawerFragment.DrawerItem(R.string.drawer_item_faces, 0,
                                              DRAWER_ITEM_FACES));
      items.add(new DrawerFragment.DrawerItem(R.string.drawer_item_routes, 0,
                                              DRAWER_ITEM_ROUTES));
      //    items.add(new DrawerFragment.DrawerItem(R.string.drawer_item_strategies, 0,
      //                                            DRAWER_ITEM_STRATEGIES));
      items.add(new DrawerFragment.DrawerItem(R.string.drawer_item_logcat, 0,
                                              DRAWER_ITEM_LOGCAT));


      m_drawerFragment = DrawerFragment.newInstance(items);

      fragmentManager
        .beginTransaction()
        .replace(R.id.navigation_drawer, m_drawerFragment, DrawerFragment.class.toString())
        .commit();

       /* List<PackageInfo> cachePackageInfo = null;
        PackageManager pm =  this.getPackageManager();
        cachePackageInfo = pm.getInstalledPackages(0);

        for(PackageInfo inf : cachePackageInfo)
        {
            G.Log("Package info " + inf.packageName);
            G.Log("Package info " + inf.versionName);
            G.Log("Package info " + inf.applicationInfo.loadLabel(pm).toString());
            //this.name = getLabel(info, context);
            //this.description = getDescription(info, context);
            //this.system = isSystem(info.packageName, context);
            //this.internet = hasInternet(info.packageName, context);
            //this.enabled = isEnabled(info, context);
            //this.launch = getIntentLaunch(info.packageName, context);
            //this.settings = getIntentSettings(info.packageName, context);
            //this.datasaver = getIntentDatasaver(info.packageName, context);
          //  G.Log("Package info " + pre_system.get(inf.packageName));

        }*/
    }

  }


  @Override
  public void onStart() {
    super.onStart();
    //signInAnonymously();

  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    Log.d(TAG,"onCreateOptionsMenu" + String.valueOf(m_drawerFragment.shouldHideOptionsMenu()));
    if (!m_drawerFragment.shouldHideOptionsMenu()) {
      updateActionBar();
      return super.onCreateOptionsMenu(menu);
    }
    else
      return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    return super.onOptionsItemSelected(item);
  }

  //////////////////////////////////////////////////////////////////////////////

  /**
   * Convenience method that updates and display the current title in the Action Bar
   */
  @SuppressWarnings("deprecation")
  private void updateActionBar() {
    ActionBar actionBar = getSupportActionBar();
    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
    actionBar.setDisplayShowTitleEnabled(true);
    if (m_actionBarTitleId != -1) {
      actionBar.setTitle(m_actionBarTitleId);
    }
  }

  /**
   * Convenience method that replaces the main fragment container with the
   * new fragment and adding the current transaction to the backstack.
   *
   * @param fragment Fragment to be displayed in the main fragment container.
   */
  private void replaceContentFragmentWithBackstack(Fragment fragment) {
    FragmentManager fragmentManager = getSupportFragmentManager();
    fragmentManager.beginTransaction()
        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        .replace(R.id.main_fragment_container, fragment)
        .addToBackStack(null)
        .commit();
  }

  //////////////////////////////////////////////////////////////////////////////

  @Override
  public void
  onDrawerItemSelected(int itemCode, int itemNameId) {

    String fragmentTag = "org.schabi.newpipe.content-" + String.valueOf(itemCode);
    FragmentManager fragmentManager = getSupportFragmentManager();

    // Create fragment according to user's selection
    Fragment fragment = fragmentManager.findFragmentByTag(fragmentTag);
    if (fragment == null) {
      switch (itemCode) {
        case DRAWER_ITEM_GENERAL:
        //  fragment = VideoListFragment.newInstance();
       try {
            fragment = KioskFragment.getInstance(0,"Trending");
      } catch (Exception e) {}
          break;
        case DRAWER_ITEM_NFD:
        //  fragment = ServiceFragment.newInstance();
          break;
        case DRAWER_ITEM_FACES:
       //   fragment = FaceListFragment.newInstance();
          break;
        case DRAWER_ITEM_ROUTES:
      //    fragment = RouteListFragment.newInstance();
          break;
        // TODO: Placeholders; Fill these in when their fragments have been created
        //    case DRAWER_ITEM_STRATEGIES:
        //      break;
       /* case DRAWER_ITEM_LOGCAT:
          fragment = LogcatFragment.newInstance();
          break;*/
        default:
          // Invalid; Nothing else needs to be done
          return;
      }
    }

    // Update ActionBar title
    m_actionBarTitleId = itemNameId;

    fragmentManager.beginTransaction()
      .replace(R.id.main_fragment_container, fragment, fragmentTag)
      .commit();
  }

    private void showProgressDialog(String caption) {

        mProgressDialog = new ProgressDialog(this);

        final String msg = caption;

        new Thread()
        {
            public void run()
            {
                MainActivity.this.runOnUiThread(new Runnable()
                {
                    public void run()
                    {
                        if (mProgressDialog == null) {
                            mProgressDialog.setIndeterminate(true);
                        }

                        mProgressDialog.setMessage(msg);
                        mProgressDialog.show();
                    }
                });
            }
        }.start();


    }
  /*@Override
  public void onDisplayLogcatSettings() {
    replaceContentFragmentWithBackstack(LogcatSettingsFragment.newInstance());
  }

  @Override
  public void onFaceItemSelected(FaceStatus faceStatus) {
    replaceContentFragmentWithBackstack(FaceStatusFragment.newInstance(faceStatus));
  }

  @Override
  public void onRouteItemSelected(RibEntry ribEntry)
  {
    replaceContentFragmentWithBackstack(RouteInfoFragment.newInstance(ribEntry));
  }

  @Override
  public void onVideoItemSelected(String videoEntry)
  {

    replaceContentFragmentWithBackstack(VideoFragment.newInstance(videoEntry));
  }


    private void signInAnonymously() {
    // Sign in anonymously. Authentication is required to read or write from Firebase Storage.
    showProgressDialog(getString(R.string.progress_auth));
    mAuth.signInAnonymously()
            .addOnSuccessListener(this, new OnSuccessListener<AuthResult>() {
              @Override
              public void onSuccess(AuthResult authResult) {
                Log.d(TAG, "signInAnonymously:SUCCESS");
                hideProgressDialog();
                updateUI(authResult.getUser());
              }
            })
            .addOnFailureListener(this, new OnFailureListener() {
              @Override
              public void onFailure(@NonNull Exception exception) {
                Log.e(TAG, "signInAnonymously:FAILURE", exception);
                hideProgressDialog();
                updateUI(null);
              }
            });
  }



    private void updateUI(FirebaseUser user) {
        // Signed in or Signed out
        if(user==null) {
            String msg = "User auth error";
            Log.d(TAG, msg);
            Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
        }
    }*/

    private void hideProgressDialog() {
        new Thread()
        {
            public void run()
            {
                MainActivity.this.runOnUiThread(new Runnable()
                {
                    public void run()
                    {
                        if (mProgressDialog != null && mProgressDialog.isShowing()) {
                            mProgressDialog.dismiss();
                        }
                    }
                });
            }
        }.start();

    }

    /*public void setSource(boolean source){
        this.source = source;

    }

    public boolean getSource(){
        return source;
    }*/



}
