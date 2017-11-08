
package io.fluentic.ubicdn.fragments;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.intel.jndn.management.types.ForwarderStatus;

import net.grandcentrix.tray.AppPreferences;

import org.joda.time.Period;
import org.joda.time.format.PeriodFormat;

import io.fluentic.ubicdn.R;
import io.fluentic.ubicdn.data.UbiCDNService;
import io.fluentic.ubicdn.data.VideoListService;
import io.fluentic.ubicdn.util.G;
import io.fluentic.ubicdn.util.MyNfdc;

public class ServiceFragment extends Fragment {

  public static ServiceFragment newInstance() {
    // Create fragment arguments here (if necessary)
    return new ServiceFragment();
  }

  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    m_handler = new Handler();
  }

  @Override
  public View onCreateView(LayoutInflater inflater,
                           @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState)
  {
    @SuppressLint("InflateParams")
    View v =  inflater.inflate(R.layout.fragment_service, null);

    isSource = (CheckBox) v.findViewById(R.id.checkbox_source);
    isSource.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {

          if (((CheckBox) v).isChecked()) {
              m_appPreferences.put(PREF_UBICDN_SERVICE_SOURCE,true);
              G.Log("Set source "+m_appPreferences.getBoolean(PREF_UBICDN_SERVICE_SOURCE,false));
          } else {
              m_appPreferences.put(PREF_UBICDN_SERVICE_SOURCE,false);
              G.Log("Set source "+m_appPreferences.getBoolean(PREF_UBICDN_SERVICE_SOURCE,false));

          }
      }
    });

    m_serviceStartStopSwitch = (Switch)v.findViewById(R.id.service_start_stop_switch);
    m_serviceStartStopSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton compoundButton, boolean isOn)
      {
          m_sharedPreferences.edit()
                  .putBoolean(PREF_UBICDN_SERVICE_STATUS, isOn)
                  .apply();
          G.Log("Shared preferences "+m_sharedPreferences.getBoolean(PREF_UBICDN_SERVICE_STATUS,true)+" isOn "+ isOn);

          if (isOn) {
              isSource.setEnabled(false);
              startUbiCDNService();
          }
          else {
              isSource.setEnabled(true);
              stopUbiCDNService();
          }
      }
    });

    m_statusView = (ViewGroup)v.findViewById(R.id.status_view);
    m_statusView.setVisibility(View.GONE);
    m_versionView = (TextView)v.findViewById(R.id.version);
    m_uptimeView = (TextView)v.findViewById(R.id.uptime);
    m_nameTreeEntriesView = (TextView)v.findViewById(R.id.name_tree_entries);
    m_fibEntriesView = (TextView)v.findViewById(R.id.fib_entries);
    m_pitEntriesView = (TextView)v.findViewById(R.id.pit_entries);
    m_measurementEntriesView = (TextView)v.findViewById(R.id.measurement_entries);
    m_csEntriesView = (TextView)v.findViewById(R.id.cs_entries);
    m_inInterestsView = (TextView)v.findViewById(R.id.in_interests);
    m_outInterestsView = (TextView)v.findViewById(R.id.out_interests);
    m_inDataView = (TextView)v.findViewById(R.id.in_data);
    m_outDataView = (TextView)v.findViewById(R.id.out_data);
    m_inNacksView = (TextView)v.findViewById(R.id.in_nacks);
    m_outNacksView = (TextView)v.findViewById(R.id.out_nacks);


    m_wifi_status_view = (ViewGroup)v.findViewById(R.id.wifi_status_view);
    m_wifi_status_view.setVisibility(View.GONE);
    m_wdStatusView = (TextView)v.findViewById(R.id.status);
    m_sdView = (TextView)v.findViewById(R.id.sd);
    m_apView = (TextView)v.findViewById(R.id.ap);
    m_restartView = (TextView)v.findViewById(R.id.restarts);

    m_btStatusView = (ViewGroup)v.findViewById(R.id.bt_status_view);
    m_btStatusView.setVisibility(View.GONE);
    m_btCtView = (TextView)v.findViewById(R.id.btstatus);
    m_btStView = (TextView)v.findViewById(R.id.btconnect);
    return v;
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState)
  {
      G.Log("ServiceFragment::onActivityCreated()");
      super.onActivityCreated(savedInstanceState);
      m_sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
      m_appPreferences = new AppPreferences(getContext()); // this Preference comes for free from the library

  }

  @Override
  public void
  onResume() {
    G.Log("ServiceFragment::onResume()");
    super.onResume();
      //boolean shouldBeSource = m_sharedPreferences.getBoolean(PREF_UBICDN_SERVICE_SOURCE, false);
      //G.Log("Shared preferences "+m_sharedPreferences.getBoolean(PREF_UBICDN_SERVICE_SOURCE,false));
      boolean shouldBeSource = m_appPreferences.getBoolean(PREF_UBICDN_SERVICE_SOURCE,false);
      isSource.setChecked(shouldBeSource);
      bindService();
  }

  @Override
  public void
  onPause() {
    super.onPause();
    G.Log("ServiceFragment::onPause()");

    unbindService();
    m_handler.removeCallbacks(m_statusUpdateRunnable);
    m_handler.removeCallbacks(m_retryConnectionToService);
  }


  VideoListService mServer;


 ServiceConnection mConnection = new ServiceConnection() {

    public void onServiceDisconnected(ComponentName name) {
      // Toast.makeText(Client.this, "Service is disconnected", 1000).show();
      mServer = null;
    }

    public void onServiceConnected(ComponentName name, IBinder service){
      /*  m_serviceMessenger2 = new Messenger(service);
        boolean shouldBeSource = m_sharedPreferences.getBoolean(PREF_UBICDN_SERVICE_SOURCE, false);
        G.Log("Shared preferences "+m_sharedPreferences.getBoolean(PREF_UBICDN_SERVICE_SOURCE,false));
        int msg;
        if(shouldBeSource){
            msg=VideoListService.VIDEOLIST_SOURCE;
        } else msg = VideoListService.VIDEOLIST_NOSOURCE;
        try {
            m_serviceMessenger2.send(Message.obtain(null, msg));
        } catch (RemoteException e) {
        }*/
      // Toast.makeText(Client.this, "Service is connected", 1000).show();
     VideoListService.LocalBinder mLocalBinder = (VideoListService.LocalBinder)service;
        G.Log("Videlistservice:ServiceConnected()");
        mServer = mLocalBinder.getServerInstance();
    }
  };

  /**
   * Method that binds the current activity to the UbiCDN Service.
   */
  private void
  bindService() {
    if (!m_isServiceConnected) {
      // Bind to Service
      getActivity().bindService(new Intent(getActivity(), UbiCDNService.class),
              m_ServiceConnection, Context.BIND_AUTO_CREATE);
      getActivity().bindService(new Intent(getActivity(), VideoListService.class),
              mConnection, Context.BIND_AUTO_CREATE);
      G.Log("ServiceFragment::bindUbiCDNService()");
    }
  }

  /**
   * Method that unbinds the current activity from the UbiCDN Service.
   */
  private void
  unbindService() {
    if (m_isServiceConnected) {
      // Unbind from Service
      getActivity().unbindService(m_ServiceConnection);

      m_isServiceConnected = false;
        getActivity().unbindService(mConnection);

      G.Log("ServiceFragment::unbindUbiCDNService()");
    }

      //Intent intent = new Intent(getActivity(), VideoListService.class);
   // getActivity().unbindService(mConnection);
  }

  public void
  startUbiCDNService() {
    assert m_isServiceConnected;

    m_serviceStartStopSwitch.setText(R.string.starting_service);
    //sendServiceMessage(UbiCDNService.START_UBICDN_SERVICE_SOURCE);
    sendServiceMessage(UbiCDNService.START_UBICDN_SERVICE);
     // Intent myService = new Intent(this, UbiCDNServiceFG.class);
     // startService(myService);
      /*Intent notificationIntent = new Intent(this, UbiCDNServiceFG.class);
      PendingIntent pendingIntent =
              PendingIntent.getActivity(this, 0, notificationIntent, 0);

      Notification notification =
              new Notification.Builder(this, CHANNEL_DEFAULT_IMPORTANCE)
                      .setContentTitle(getText(R.string.notification_title))
                      .setContentText(getText(R.string.notification_message))
                      .setSmallIcon(R.drawable.icon)
                      .setContentIntent(pendingIntent)
                      .setTicker(getText(R.string.ticker_text))
                      .build();

      getContext().startForegroundService(ONGOING_NOTIFICATION_ID, notification)*/

     /* try {
          final Intent prepare = VpnService.prepare(getContext());
          if (prepare == null) {
              G.Log("Prepare done");
              onActivityResult(REQUEST_VPN, RESULT_OK, null);
          } else {

              G.Log("Start intent=" + prepare);
              try {
                  // com.android.vpndialogs.ConfirmDialog required
                  startActivityForResult(prepare, REQUEST_VPN);
              } catch (Throwable ex) {
                  G.Log(ex.toString() + "\n" + Log.getStackTraceString(ex));
                  onActivityResult(REQUEST_VPN, RESULT_CANCELED, null);
        //          prefs.edit().putBoolean("enabled", false).apply();
              }

          }
      } catch (Throwable ex) {
          // Prepare failed
          G.Log(ex.toString() + "\n" + Log.getStackTraceString(ex));
       //   prefs.edit().putBoolean("enabled", false).apply();
      }*/
  }

  public void
  stopUbiCDNService() {
    assert m_isServiceConnected;

    m_serviceStartStopSwitch.setText(R.string.stopping_service);
    sendServiceMessage(UbiCDNService.STOP_UBICDN_SERVICE);

    // disable status block
    m_statusView.setVisibility(View.GONE);
    m_wifi_status_view.setVisibility(View.GONE);
    m_btStatusView.setVisibility(View.GONE);
    m_handler.removeCallbacks(m_statusUpdateRunnable);
  }

  /**
   * Convenience method to send a message to the UbiCDN Service
   * through a Messenger.
   *
   * @param message Message from a set of predefined UbiCDN Service messages.
   */
  private void
  sendServiceMessage(int message) {
    if (m_serviceMessenger == null) {
      G.Log("UbiCDN Service not yet connected");
      return;
    }
    try {
      Message msg = Message.obtain(null, message);
      msg.replyTo = m_clientMessenger;
      m_serviceMessenger.send(msg);
    } catch (RemoteException e) {
      // If Service crashes, nothing to do here
      G.Log("UbiCDN service Disconnected: " + e);
    }
  }

  private void
  setServiceRunning() {
    m_serviceStartStopSwitch.setEnabled(true);
    m_serviceStartStopSwitch.setText(R.string.service_started);
    m_serviceStartStopSwitch.setChecked(true);
  }

  private void
  setServiceStopped() {
    m_serviceStartStopSwitch.setEnabled(true);
    m_serviceStartStopSwitch.setText(R.string.service_stopped);
    m_serviceStartStopSwitch.setChecked(false);

  }

  private void
  setServiceDisconnected() {
    m_serviceStartStopSwitch.setEnabled(false);
    m_serviceStartStopSwitch.setText(R.string.reconnect_to_service);
    m_serviceStartStopSwitch.setChecked(false);
  }

  /**
   * Client Message Handler.
   *
   * This handler is used to handle messages that are being sent back
   * from the UbiCDN Service to the current application.
   */
  private class ClientHandler extends Handler {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case UbiCDNService.UBICDN_SERVICE_RUNNING:
          setServiceRunning();
          G.Log("ClientHandler: UbiCDN is Running.");

          m_handler.postDelayed(m_statusUpdateRunnable, 500);
          break;

        case UbiCDNService.UBICDN_SERVICE_STOPPED:
          setServiceStopped();
          G.Log("ClientHandler: UbiCDN is Stopped.");
          break;

        default:
          super.handleMessage(msg);
          break;
      }
    }
  }

  /**
   * Client ServiceConnection to UbiCDN Service.
   */
  private final ServiceConnection m_ServiceConnection = new ServiceConnection() {
    @Override
    public void
    onServiceConnected(ComponentName className, IBinder service) {
      // Establish Messenger to the Service
      m_serviceMessenger = new Messenger(service);
      m_isServiceConnected = true; // onServiceConnected runs on the main thread

      // Check if UbiCDN  Service is running
      try {
        boolean shouldServiceBeOn = m_sharedPreferences.getBoolean(PREF_UBICDN_SERVICE_STATUS, false);
        Message msg;

          G.Log("onServiceConnected(): " + shouldServiceBeOn);
          //if(source)  msg = Message.obtain(null, shouldServiceBeOn ? UbiCDNService.START_UBICDN_SERVICE_SOURCE : UbiCDNService.STOP_UBICDN_SERVICE);
        //else
        msg = Message.obtain(null, shouldServiceBeOn ? UbiCDNService.START_UBICDN_SERVICE : UbiCDNService.STOP_UBICDN_SERVICE);
        msg.replyTo = m_clientMessenger;
        m_serviceMessenger.send(msg);
      } catch (RemoteException e) {
        // If Service crashes, nothing to do here
        G.Log("onServiceConnected(): " + e);
      }

      G.Log("m_ServiceConnection::onServiceConnected()");
    }

    @Override
    public void
    onServiceDisconnected(ComponentName componentName) {
      // In event of unexpected disconnection with the Service; Not expecting to get here.
      G.Log("m_ServiceConnection::onServiceDisconnected()");

      // Update UI
      //setServiceDisconnected();
        restarts++;
      m_isServiceConnected = false; // onServiceDisconnected runs on the main thread
      m_handler.postDelayed(m_retryConnectionToService, 2000);
      //  m_handler.postDelayed()
    }
  };

  /**
   * Attempt to reconnect to the UbiCDN Service.
   *
   * This method attempts to reconnect the application to the UbiCDN Service
   * when the UbiCDN Service has been killed (either by the user or by the OS).
   */
  private Runnable m_retryConnectionToService = new Runnable() {
    @Override
    public void
    run()
    {
      G.Log("Retrying connection to UbiCDN Service ...");
      bindService();
    }
  };

  private class StatusUpdateTask extends AsyncTask<Void, Void, ForwarderStatus> {
    /**
     * @param voids
     * @return ForwarderStatus if operation succeeded, null if operation failed
     */
    @Override
    protected ForwarderStatus
    doInBackground(Void... voids)
    {
      try {
        MyNfdc nfdcHelper = new MyNfdc();
        ForwarderStatus fs = nfdcHelper.generalStatus();
        nfdcHelper.shutdown();
        return fs;
      }
      catch (Exception e) {
        G.Log("Servicefragment","Error communicating with NFD (" + e.getMessage() + ")");
        return null;
      }
    }

    @Override
    protected void
    onPostExecute(ForwarderStatus fs)
    {
      if (fs == null) {
        // when failed, try after 0.5 seconds
        m_handler.postDelayed(m_statusUpdateRunnable, 500);
      }
      else {
        m_versionView.setText(fs.getNfdVersion());
        m_uptimeView.setText(PeriodFormat.getDefault().print(new Period(
          fs.getCurrentTimestamp() - fs.getStartTimestamp())));
        m_nameTreeEntriesView.setText(String.valueOf(
          fs.getNNameTreeEntries()));
        m_fibEntriesView.setText(String.valueOf(fs.getNFibEntries()));
        m_pitEntriesView.setText(String.valueOf(fs.getNPitEntries()));
        m_measurementEntriesView.setText(String.valueOf(
          fs.getNMeasurementsEntries()));
        m_csEntriesView.setText(String.valueOf(fs.getNCsEntries()));

        m_inInterestsView.setText(String.valueOf(fs.getNInInterests()));
        m_outInterestsView.setText(String.valueOf(fs.getNOutInterests()));

        m_inDataView.setText(String.valueOf(fs.getNInDatas()));
        m_outDataView.setText(String.valueOf(fs.getNOutDatas()));

        m_inNacksView.setText(String.valueOf(fs.getNInNacks()));
        m_outNacksView.setText(String.valueOf(fs.getNOutNacks()));

        m_statusView.setVisibility(View.VISIBLE);

        m_btStatusView.setVisibility(View.VISIBLE);
        m_btStView.setText("On");
        m_btCtView.setText(Integer.toString(0));
        m_sdView.setText("Connected");
        m_apView.setText("On");
        m_wdStatusView.setText("Activated");
        m_restartView.setText(Integer.toString(restarts));
        m_wifi_status_view.setVisibility(View.VISIBLE);

          // refresh after 5 seconds
        m_handler.postDelayed(m_statusUpdateRunnable, 5000);
      }
    }
  }

  //////////////////////////////////////////////////////////////////////////////

  private CheckBox isSource;
  /** Button that starts and stops the UbiCDN  */
  private Switch m_serviceStartStopSwitch;

  /** Flag that marks that application is connected to the UbiCDN Service */
  private boolean m_isServiceConnected = false;

  /** Client Message Handler */
  private final Messenger m_clientMessenger = new Messenger(new ClientHandler());

  /** Messenger connection to UbiCDN Service */
  private Messenger m_serviceMessenger = null;

  /** ListView holding UbiCDN  status information */
  private ViewGroup m_statusView;

  private TextView m_versionView;
  private TextView m_uptimeView;
  private TextView m_nameTreeEntriesView;
  private TextView m_fibEntriesView;
  private TextView m_pitEntriesView;
  private TextView m_measurementEntriesView;
  private TextView m_csEntriesView;
  private TextView m_inInterestsView;
  private TextView m_outInterestsView;
  private TextView m_inDataView;
  private TextView m_outDataView;
  private TextView m_inNacksView;
  private TextView m_outNacksView;


  private ViewGroup m_btStatusView;
  private TextView m_btStView;
  private TextView m_btCtView;

  private ViewGroup m_wifi_status_view;
  private TextView m_apView;
  private TextView m_wdStatusView;
  private TextView m_sdView;
  private TextView m_restartView;

  private Handler m_handler;
  private Runnable m_statusUpdateRunnable = new Runnable() {
    @Override
    public void run()
    {
      new StatusUpdateTask().execute();
    }
  };

  private SharedPreferences m_sharedPreferences;
  private AppPreferences m_appPreferences;
//  private Messenger m_serviceMessenger2 = null;

  private static final String PREF_UBICDN_SERVICE_STATUS = "UBICDN_SERVICE_STATUS";
  public static final String PREF_UBICDN_SERVICE_SOURCE = "UBICDN_SERVICE_TYPE";

  private AlertDialog dialogVpn = null;

  private static final int REQUEST_VPN = 1;
  private static final int REQUEST_INVITE = 2;
  private static final int REQUEST_LOGCAT = 3;
  public static final int REQUEST_ROAMING = 4;

    private int restarts=0;

}
