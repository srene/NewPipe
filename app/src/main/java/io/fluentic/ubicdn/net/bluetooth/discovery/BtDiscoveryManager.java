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

package io.fluentic.ubicdn.net.bluetooth.discovery;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;

import java.util.List;

import io.fluentic.ubicdn.net.bluetooth.BtServiceDiscovery;
import io.fluentic.ubicdn.net.bluetooth.discovery.ble.ManufacturerData;
import io.fluentic.ubicdn.net.bluetooth.discovery.ble.advertiser.BleAdvertiser;
import io.fluentic.ubicdn.net.bluetooth.discovery.ble.scanner.BleScanner;
import io.fluentic.ubicdn.net.bluetooth.discovery.idle.IdleAdvertiser;
import io.fluentic.ubicdn.util.Config;
import io.fluentic.ubicdn.util.dispatch.DispatchQueue;

//import io.fluentic.ubicdn.bluetooth.BtLink;
//import io.fluentic.ubicdn.net.bluetooth.discovery.classic.BtScanner;

public class BtDiscoveryManager implements Scanner.Listener, Advertiser.Listener
{
	private enum State
	{
		IDLE,
		BLE_SCANNING,
		BT_SCANNING,
		ADVERTISING
	}

	private boolean running;

	private BtServiceDiscovery transport;
	private Advertiser idleAdvertiser;
	//private Scanner btScanner;
	private Scanner bleScanner;
	private Advertiser bleAdvertiser;

	private State state;
	private boolean foreground = true;

	public BtDiscoveryManager(
			BtServiceDiscovery transport,
			BluetoothAdapter adapter,
			DispatchQueue queue,
			Context context,
	        List<String> uuids
	)
	{
		this.transport = transport;

		this.idleAdvertiser = new IdleAdvertiser(this, queue);

		//this.btScanner = new BtScanner(adapter, context, this, queue, uuids);

		this.bleScanner = new BleScanner(transport.getAppId(), context, this, queue);
		this.bleAdvertiser = new BleAdvertiser(transport.getAppId(), context, this, queue);
	}

	public boolean isPeripheralSupported()
	{
		return bleAdvertiser.isSupported();
	}

	public void start()
	{
		if(running)
			return;

		running = true;

		state = State.IDLE;
		nextState();
	}

	public void stop()
	{
		if(!running)
			return;

		running = false;
		nextState();
	}

	public void onMainActivityResumed()
	{
		foreground = true;

		if(!running)
			return;

		state = State.IDLE;

		idleAdvertiser.stopAdvertise();
		//btScanner.stopScan();
		bleScanner.stopScan();
		bleAdvertiser.stopAdvertise();
	}

	public void onMainActivityPaused()
	{
		foreground = false;

		if(!running)
			return;

		state = State.IDLE;

		idleAdvertiser.stopAdvertise();
		//btScanner.stopScan();
		bleScanner.stopScan();
		bleAdvertiser.stopAdvertise();
	}

	public void onChannelsListeningChanged()
	{
		bleAdvertiser.touch();
	}

	private void nextState()
	{
		if(!running)
		{
			state = State.IDLE;
			idleAdvertiser.stopAdvertise();
			//btScanner.stopScan();
			bleScanner.stopScan();
			bleAdvertiser.stopAdvertise();
			return;
		}

		if(state == State.IDLE)
		{
			state = State.BLE_SCANNING;
			bleScanner.startScan(Config.bleScanDuration);
			return;
		}

		if(state == State.BLE_SCANNING)
		{
			//state = State.BT_SCANNING;
			//btScanner.startScan(Config.btScanDuration);

			long duration = foreground
					? Config.bleAdvertiseForegroundDuration
					: Config.bleAdvertiseBackgroundDuration;

			if(!bleAdvertiser.isSupported())
			{
				duration = foreground ? duration / 2 : duration;

				state = State.IDLE;
				idleAdvertiser.startAdvertise(duration);
				return;
			}

			state = State.ADVERTISING;
			bleAdvertiser.startAdvertise(duration);

			return;
		}

		if(state == State.BT_SCANNING)
		{
			state = State.ADVERTISING;
			bleAdvertiser.startAdvertise(
					foreground
							? Config.bleAdvertiseForegroundDuration
							: Config.bleAdvertiseBackgroundDuration);
			return;
		}

		if(state == State.ADVERTISING && foreground)
		{
			state = State.BLE_SCANNING;
			bleScanner.startScan(Config.bleScanDuration);
			return;
		}

		if(state == State.ADVERTISING && !foreground)
		{
			state = State.IDLE;
			idleAdvertiser.startAdvertise(Config.bleIdleBackgroundDuration);
			return;
		}
	} // nextState()

	//region Advertiser.Listener
	@Override
	public void onAdvertiseStarted(Advertiser advertiser)
	{
	}

	@Override
	public void onAdvertiseStopped(Advertiser advertiser, boolean error)
	{
		nextState();
	}
	//endregion

	@Override
	public ManufacturerData onAdvertisementDataRequested()
	{
		ManufacturerData data = ManufacturerData.create(
				transport.getAppId(),
				transport.getAddress(),
				transport.getChannelsListening()
		);

		return data;
	}

	//region Scanner.Listener
	@Override
	public void onScanStarted(Scanner scanner)
	{
	}

	@Override
	public void onScanStopped(Scanner scanner, boolean error)
	{
		nextState();
	}

	@Override
	public void onDeviceUuidsDiscovered(Scanner scanner, BluetoothDevice device, List<String> deviceUuids)
	{
		transport.onDeviceUuidsDiscovered(device, deviceUuids);
	}

	@Override
	public void onDeviceChannelsDiscovered(Scanner scanner, BluetoothDevice device, List<Integer> channels)
	{
		transport.onDeviceChannelsDiscovered(device, channels);
	}
	//endregion
} // BtDiscoveryManager
