/*
 * Copyright (c) 2016 Vladimir L. Shabanov <virlof@gmail.com>
 *
 * Licensed under the Underdark License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://underdark.io/LICENSE.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fluentic.ubicdn.net.bluetooth.discovery.ble.scanner;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.Arrays;

import io.fluentic.ubicdn.net.bluetooth.BtUtils;
import io.fluentic.ubicdn.net.bluetooth.discovery.Scanner;
import io.fluentic.ubicdn.net.bluetooth.discovery.ble.BleConfig;
import io.fluentic.ubicdn.net.bluetooth.discovery.ble.ManufacturerData;
import io.fluentic.ubicdn.net.bluetooth.discovery.ble.detector.BleDetector;
import io.fluentic.ubicdn.net.bluetooth.discovery.ble.detector.BleDetectorFactory;
import io.fluentic.ubicdn.net.bluetooth.discovery.ble.detector.BleScanRecord;
import io.fluentic.ubicdn.util.G;
import io.fluentic.ubicdn.util.dispatch.DispatchQueue;

//import io.underdark.Config;

@TargetApi(18)
public class BleScanner implements BleDetector.Listener, Scanner
{
	private static final String TAG="BleScanner";
	private boolean running;

	private int appId;
	private BluetoothAdapter adapter;
	Context context;
	private Scanner.Listener listener;
	DispatchQueue queue;

	private BleDetector detector;

	private Runnable stopCommand;

	public BleScanner(
			int appId,
			Context context,
			Scanner.Listener listener,
			DispatchQueue queue
	)
	{
		this.appId = appId;
		this.context = context;
		this.listener = listener;
		this.queue = queue;
	}

	//region Scanner
	@Override
	public void startScan(long durationMs)
	{
		if(Build.VERSION.SDK_INT < 18)
		{
			queue.dispatch(new Runnable()
			{
				@Override
				public void run()
				{
					listener.onScanStopped(BleScanner.this, true);
				}
			});
			return;
		}

		if(running)
			return;

		if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
		{
			G.Log(TAG,"Bluetooth LE is not supported on this device.");
			queue.dispatch(new Runnable()
			{
				@Override
				public void run()
				{
					listener.onScanStopped(BleScanner.this, true);
				}
			});
			return;
		}

		final BluetoothManager bluetoothManager =
				(BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
		this.adapter = bluetoothManager.getAdapter();

		if(this.adapter == null)
		{
			G.Log(TAG,"Bluetooth is not supported on this device.");
			queue.dispatch(new Runnable()
			{
				@Override
				public void run()
				{
					listener.onScanStopped(BleScanner.this, true);
				}
			});
			return;
		}

		running = true;

		queue.dispatch(new Runnable()
		{
			@Override
			public void run()
			{
				G.Log(TAG,"ble scan started");
				listener.onScanStarted(BleScanner.this);
			}
		});

		this.detector = BleDetectorFactory.create(adapter, this, queue);
		this.detector.startScan();

		stopCommand =
				queue.dispatchAfter(durationMs, new Runnable()
				{
					@Override
					public void run()
					{
						stopScan();
					}
				});
	} // startScan()

	@Override
	public void stopScan()
	{
		if(!running)
			return;

		queue.cancel(stopCommand);
		stopCommand = null;

		running = false;

		detector.stopScan();
	} // stopScan()
	//endregion

	//region BleDetector.Listener
	@Override
	public void onScanStarted()
	{
	}

	@Override
	public void onScanStopped(final boolean error)
	{
		//Logger.debug("ble scan stopped");
		running = false;
		detector = null;

		queue.dispatch(new Runnable()
		{
			@Override
			public void run()
			{
				listener.onScanStopped(BleScanner.this, error);
			}
		});
	}

	@Override
	public void onDeviceDetected(BluetoothDevice device, byte[] scanRecordData)
	{
		//G.Log(TAG,"onDevicedetected1 " + device.getAddress() + " " + device.getName());
		if(!running)
			return;
		//G.Log(TAG,"onDevicedetected2");

		BleScanRecord scanRecord = BleScanRecord.parseFromBytes(scanRecordData);
		if(scanRecord == null)
			return;
		//G.Log(TAG,"onDevicedetected3");

		byte[] data = scanRecord.getManufacturerSpecificData(BleConfig.manufacturerId);
		if(data == null)
			return;
		//G.Log(TAG,"onDevicedetected4");

		final ManufacturerData manufacturerData =
				ManufacturerData.parse(data);
		if(manufacturerData == null || manufacturerData.getAppId() != appId)
			return;
		//G.Log(TAG,"onDevicedetected5");

		final byte[] localAddress = BtUtils.getBytesFromAddress(adapter.getAddress());

		if(Arrays.equals(localAddress, manufacturerData.getAddress()))
			return;
		//G.Log(TAG,"onDevicedetected6");

		final BluetoothDevice remoteDevice = adapter.getRemoteDevice(manufacturerData.getAddress());
		//final BluetoothDevice remoteDevice = device;
		G.Log(TAG,"onDevicedetected " + device.getAddress() + " " + device.getName());

		queue.dispatch(new Runnable()
		{
			@Override
			public void run()
			{
				listener.onDeviceChannelsDiscovered(BleScanner.this, remoteDevice, manufacturerData.getChannels());
			}
		});
	} // onDeviceDetected
	//endregion
} // BleScanner
