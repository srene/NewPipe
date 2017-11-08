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

package io.fluentic.ubicdn.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.fluentic.ubicdn.R;
import io.fluentic.ubicdn.data.Content;
import io.fluentic.ubicdn.data.DatabaseHandler;
import io.fluentic.ubicdn.data.VideoListService;
import io.fluentic.ubicdn.data.VideoListService.LocalBinder;
import io.fluentic.ubicdn.util.G;

import static android.content.Context.BIND_AUTO_CREATE;


public class VideoListFragment extends ListFragment {
  private List<String> names;

  public static VideoListFragment
  newInstance() {
    return new VideoListFragment();
  }

  public interface Callbacks {
    /**
     * This method is called when a route is selected and more
     * information about it should be presented to the user.
     *
     * @param ribEntry RibEntry instance with information about the selected route
     */
    void onVideoItemSelected(String videoEntry);
  }

  VideoListService mServer;

  @Override
  public void onAttach(Activity activity)
  {
    super.onAttach(activity);
    try {
      m_callbacks = (Callbacks)activity;
    } catch (Exception e) {
      G.Log(TAG,"Hosting activity must implement this fragment's callbacks: " + e);
    }
  }

  @Override
  public void
  onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

   // setHasOptionsMenu(true);
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState)
  {
    super.onViewCreated(view, savedInstanceState);
    View v = getLayoutInflater(savedInstanceState).inflate(R.layout.fragment_video_list_list_header, null);
    getListView().addHeaderView(v, null, false);
  //  getListView().setDivider(getResources().getDrawable(R.drawable.list_item_divider));


    m_videoListInfoUnavailableView = v.findViewById(R.id.video_list_info_unavailable);

    // Get progress bar spinner view
    m_reloadingListProgressBar = (ProgressBar)v.findViewById(R.id.video_list_reloading_list_progress_bar);


  }


  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState)
  {
    super.onActivityCreated(savedInstanceState);
    if (m_videoListAdapter == null) {
      m_videoListAdapter = new VideoListAdapter(getActivity());
    }
    // setListAdapter must be called after addHeaderView.  Otherwise, there is an exception on some platforms.
    // http://stackoverflow.com/a/8141537/2150331
    setListAdapter(m_videoListAdapter);
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
  {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.menu_route_list, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item)
  {
    switch (item.getItemId()) {
      case R.id.route_list_refresh:
        retrieveVideoList();
        return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void
  onResume() {
    super.onResume();
      db = new DatabaseHandler(getActivity());
      startVideoListInfoRetrievalTask();
      /*if (!m_isServiceConnected){
          Intent intent = new Intent(getActivity(), VideoListService.class);
          getActivity().bindService(intent, mConnection, BIND_AUTO_CREATE);
      }*/
      startReceiver();
      Intent intent = new Intent(getActivity(), VideoListService.class);
      getActivity().bindService(intent, mConnection, BIND_AUTO_CREATE);
      getActivity().startService(intent);


  }

    ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceDisconnected(ComponentName name) {
            // Toast.makeText(Client.this, "Service is disconnected", 1000).show();
            mServer = null;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            // Toast.makeText(Client.this, "Service is connected", 1000).show();
            LocalBinder mLocalBinder = (LocalBinder)service;
            mServer = mLocalBinder.getServerInstance();
        }
    };
     // getActivity().startService(intent);



  @Override
  public void
  onPause() {
    super.onPause();
    //stopVideoListInfoRetrievalTask();
      /*if (m_isServiceConnected) {
          getActivity().unbindService(mConnection);
          m_isServiceConnected = false;
      }*/
      getActivity().unbindService(mConnection);
      getActivity().unregisterReceiver(mBroadcastReceiver);
  }

  @Override
  public void onDestroyView()
  {
    super.onDestroyView();
    setListAdapter(null);
      Intent intent = new Intent(getActivity(), VideoListService.class);
      //getActivity().stopService(intent);
  }

  @Override
  public void onListItemClick(ListView l, View v, int position, long id)
  {
    if (m_callbacks != null) {
        String video = names.get((int)l.getAdapter().getItemId(position));
        m_callbacks.onVideoItemSelected(video);
    //  RibEntry ribEntry = (RibEntry)l.getAdapter().getItem(position);
    //  m_callbacks.onRouteItemSelected(ribEntry);
    }
  }


  /////////////////////////////////////////////////////////////////////////

  /**
   * Updates the underlying adapter with the given list of RibEntry.
   *
   * Note: This method should only be called from the UI thread.
   *
   * @param list Update ListView with the given List&lt;RibEntry&gt;
   */
  private void updateVideoList(List<Pair<String,Bitmap>> list,List<String> names) {
    if (list == null) {
      m_videoListInfoUnavailableView.setVisibility(View.VISIBLE);
      return;
    }

    ((VideoListAdapter)getListAdapter()).updateList(list,names);
  }

  /**
   * Convenience method that starts the AsyncTask that retrieves the
   * list of available routes.
   */
  private void retrieveVideoList() {
    // Update UI
    m_videoListInfoUnavailableView.setVisibility(View.GONE);

    // Stop if running; before starting the new Task
  //  stopVideoListInfoRetrievalTask();
    startVideoListInfoRetrievalTask();
  }

  /**
   * Create a new AsynTask for route list information retrieval.
   */
  private void startVideoListInfoRetrievalTask() {

      File dir = getActivity().getFilesDir();
      File[] subFiles = dir.listFiles();
      G.Log(TAG,"Files " +subFiles.length);

      List<Pair<String,Bitmap>> videos = new ArrayList<>();

      List<Content> content = db.getContent();

      names = new ArrayList<>();
      for (Content cn : content) {
          // Writing Contacts to log
          G.Log(TAG,"Name:"+ cn.getName()+ " desc:" + cn.getText() + " url:"+cn.getUrl());
          boolean found=false;
          if (subFiles != null) {
              //G.Log("Files " +subFiles);
              for (File file : subFiles) {
                  G.Log("Filename " + cn.getName() + " " +file.getAbsolutePath());
                  if (file.getName().equals(cn.getName()+".mp4")) {
                     // G.Lo
                      String meta = "";
                      MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                      try {
                          retriever.setDataSource(file.getAbsolutePath());
                         // meta = retriever.extractMetadata(METADATA_KEY_TITLE);

                          //if (meta != null && !meta.isEmpty()) {
                              //ImageView capturedImageView = (ImageView) findViewById(R.id.capturedimage);

                              Bitmap bmFrame = retriever.getFrameAtTime(); //unit in microsecond
                              found = true;
                              videos.add(new Pair<String,Bitmap>(cn.getText(),bmFrame));
                              names.add(cn.getName());
                         // }


                      } catch (Exception e) {
                          G.Log("Exception : " + e.getMessage());
                      }
                  }
              }
          }
          if(!found)
          {
              Bitmap bmFrame= BitmapFactory.decodeResource(getContext().getResources(), R.drawable.arrow_down);
              videos.add(new Pair<String, Bitmap>(cn.getText(), bmFrame));
              names.add("");
          }

      }



      updateVideoList(videos,names);

  //  m_videoListAsyncTask = new VideoListAsyncTask();
  //  m_videoListAsyncTask.execute();
  }

  /**
   * Stops a previously started AsyncTask.

  private void stopVideoListInfoRetrievalTask() {
    if (m_videoListAsyncTask != null) {
      m_videoListAsyncTask.cancel(false);
      m_videoListAsyncTask = null;
    }
  } */
  public void startReceiver()
  {
      G.Log("startReceiver()");
      mBroadcastReceiver = new BroadcastReceiver() {
          @Override
          public void onReceive(Context context, Intent intent) {
              G.Log("onReceive:" + intent);
              //hideProgressDialog();

              switch (intent.getAction()) {
                  case VideoListService.DOWNLOAD_COMPLETED:
                      G.Log("Download completed");
                      retrieveVideoList();
                      break;
                  case VideoListService.NEW_VIDEO_AVAILABLE

                          :
                      G.Log("New video");
                      retrieveVideoList();
              }
          }
      };
     getActivity().registerReceiver(mBroadcastReceiver, VideoListService.getIntentFilter());
      // manager.registerReceiver(mBroadcastReceiver,VideoListService.getIntentFilter());
  }
  /////////////////////////////////////////////////////////////////////////

  private static class VideoListAdapter extends BaseAdapter {

    public VideoListAdapter(Context context) {
      m_layoutInflater = LayoutInflater.from(context);
    }

    public void
    updateList(List<Pair<String,Bitmap>> videoEntries,List<String> names) {
      m_videoEntries = videoEntries;
      m_names = names;
      notifyDataSetChanged();
    }

    @Override
    public int getCount()
    {
      return (m_videoEntries == null) ? 0 : m_videoEntries.size();
    }

    @Override
    public Pair<String,Bitmap>
    getItem(int i)
    {
      assert m_videoEntries != null;
      return m_videoEntries.get(i);
    }

    public String
    getItemName(int i)
    {
        assert m_names != null;
        return m_names.get(i);
    }
    @Override
    public long
    getItemId(int i)
    {
      return i;
    }

    @Override
    public View
    getView(int position, View convertView, ViewGroup parent) {
      VideoItemHolder holder;

      if (convertView == null) {
        holder = new VideoItemHolder();

        convertView = m_layoutInflater.inflate(R.layout.list_item_video_item, null);
        convertView.setTag(holder);

        holder.m_uri = (TextView) convertView.findViewById(R.id.list_item_video_uri);
        holder.m_frame = (ImageView) convertView.findViewById(R.id.list_item_video_frame);
      } else {
        holder = (VideoItemHolder) convertView.getTag();
      }

      Pair<String,Bitmap> entry = getItem(position);

      // Prefix
      holder.m_uri.setText(entry.first);
      holder.m_frame.setImageBitmap(entry.second);


      return convertView;
    }

    private static class VideoItemHolder {
      private TextView m_uri;
      private ImageView m_frame;
    }

    private final LayoutInflater m_layoutInflater;
    private List<Pair<String,Bitmap>> m_videoEntries;
    private List<String> m_names;
  }



  /////////////////////////////////////////////////////////////////////////////

  /** Callback handler of the hosting activity */
  private Callbacks m_callbacks;

  /** Reference to the most recent AsyncTask that was created for listing routes */
 // private VideoListAsyncTask m_videoListAsyncTask;

  /** Reference to the view to be displayed when no information is available */
  private View m_videoListInfoUnavailableView;

  /** Progress bar spinner to display to user when destroying faces */
  private ProgressBar m_reloadingListProgressBar;

  private VideoListAdapter m_videoListAdapter;

  private boolean m_isServiceConnected = false;

  private boolean m_isServiceStarted = false;
    BroadcastReceiver mBroadcastReceiver;

  private DatabaseHandler db;

  private static final String TAG = VideoListFragment.class.getName();

}
