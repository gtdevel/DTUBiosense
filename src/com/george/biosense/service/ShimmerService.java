package com.george.biosense.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.shimmerresearch.driver.ObjectCluster;
import com.shimmerresearch.driver.Shimmer;
import com.shimmerresearch.tools.Logging;

/**
 * @author Joro This Service is built on top of the Bluetooth stack and performs
 *         all communication with the Shimmer device
 * 
 */
/**
 * @author Joro
 *
 */
/**
 * @author Joro
 *
 */
/**
 * @author Joro
 * 
 */
public class ShimmerService extends Service {
	private static final String TAG = "MyService";
	public Shimmer shimmerDevice1 = null;
	public Logging shimmerLog1 = null;
	public static String outputFolder = "DTU_Biosense";
	private boolean mEnableLogging = false;
	private final IBinder mBinder = new LocalBinder();
	public HashMap<String, Object> mMultiShimmer = new HashMap<String, Object>(
			7);
	public HashMap<String, Logging> mLogShimmer = new HashMap<String, Logging>(
			7);
	private Handler mHandlerGraph = null;
	private boolean mGraphing = false;
	private String mLogFileName = "Default.csv";

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onCreate() {
		Toast.makeText(this, "My Service Created", Toast.LENGTH_LONG).show();
		Log.d(TAG, "onCreate");
	}

	public class LocalBinder extends Binder {
		public ShimmerService getService() {
			// Return this instance of LocalService so clients can call public
			// methods
			return ShimmerService.this;
		}
	}

	@Override
	public void onDestroy() {
		Toast.makeText(this, "My Service Stopped", Toast.LENGTH_LONG).show();
		Log.d(TAG, "onDestroy");
		Collection<Object> colS = mMultiShimmer.values();
		Iterator<Object> iterator = colS.iterator();
		while (iterator.hasNext()) {
			Shimmer stemp = (Shimmer) iterator.next();
			stemp.stop();
		}

	}
	
	public void connectShimmer(String bluetoothAddress, String selectedDevice) {
		Log.d("Shimmer", "net Connection");
		Shimmer shimmerDevice = new Shimmer(this, mHandler, selectedDevice,
				false);
		mMultiShimmer.remove(bluetoothAddress);
		if (mMultiShimmer.get(bluetoothAddress) == null) {
			mMultiShimmer.put(bluetoothAddress, shimmerDevice);
			((Shimmer) mMultiShimmer.get(bluetoothAddress)).connect(
					bluetoothAddress, "default");
		}
	}

	public void disconnectAllDevices() {
		Collection<Object> colS = mMultiShimmer.values();
		Iterator<Object> iterator = colS.iterator();
		while (iterator.hasNext()) {
			Shimmer stemp = (Shimmer) iterator.next();
			stemp.stop();
		}
		mMultiShimmer.clear();
		mLogShimmer.clear();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d("LocalService", "Received start id " + startId + ": " + intent);
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	@Override
	public void onStart(Intent intent, int startid) {
		Toast.makeText(this, "My Service Started", Toast.LENGTH_LONG).show();

		Log.d(TAG, "onStart");

	}

	public void onStop() {
		Toast.makeText(this, "My Service Stopped", Toast.LENGTH_LONG).show();
		Log.d(TAG, "onDestroy");
		Collection<Object> colS = mMultiShimmer.values();
		Iterator<Object> iterator = colS.iterator();
		while (iterator.hasNext()) {
			Shimmer stemp = (Shimmer) iterator.next();
			stemp.stop();
		}
	}

	public void toggleAllLEDS() {
		Collection<Object> colS = mMultiShimmer.values();
		Iterator<Object> iterator = colS.iterator();
		while (iterator.hasNext()) {
			Shimmer stemp = (Shimmer) iterator.next();
			if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED) {
				stemp.toggleLed();
			}
		}
	}

	/**
	 * Bluetooth communicates via this handler
	 */
	public final Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) { // handlers have a what identifier which is used
								// to identify the type of msg
			case Shimmer.MESSAGE_READ:
				if ((msg.obj instanceof ObjectCluster)) { // within each msg an
															// object can be
															// include,
															// objectclusters
															// are used to
															// represent the
															// data structure of
															// the shimmer
															// device
					ObjectCluster objectCluster = (ObjectCluster) msg.obj;
					if (mEnableLogging == true) {
						shimmerLog1 = (Logging) mLogShimmer
								.get(objectCluster.mBluetoothAddress);
						if (shimmerLog1 != null) {
							shimmerLog1.logData(objectCluster);
						} else {
							char[] bA = objectCluster.mBluetoothAddress
									.toCharArray();
							Logging shimmerLog;
							if (mLogFileName.equals("Default.csv")) {
								shimmerLog = new Logging(Long.toString(System
										.currentTimeMillis())
										+ " Device"
										+ bA[12] + bA[13] + bA[15] + bA[16],
										" ,", outputFolder);
							} else {
								shimmerLog = new Logging(mLogFileName, " ,",
										outputFolder);
							}
							mLogShimmer.remove(objectCluster.mBluetoothAddress);
							if (mLogShimmer
									.get(objectCluster.mBluetoothAddress) == null) {
								mLogShimmer.put(
										objectCluster.mBluetoothAddress,
										shimmerLog);
							}
						}
					}

					if (mGraphing == true) {
						Log.d("ShimmerGraph", "Sending");
						mHandlerGraph.obtainMessage(Shimmer.MESSAGE_READ,
								objectCluster).sendToTarget();
					}
				}
				break;
			case Shimmer.MESSAGE_TOAST:
				Log.d("toast", msg.getData().getString(Shimmer.TOAST));
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(Shimmer.TOAST),
						Toast.LENGTH_SHORT).show();
				if (msg.getData().getString(Shimmer.TOAST)
						.equals("Device connection was lost")) {

				}
				break;
			case Shimmer.MESSAGE_STATE_CHANGE:
				Intent intent = new Intent(
						"com.shimmerresearch.service.ShimmerService");
				Log.d("ShimmerGraph", "Sending");
				mHandlerGraph.obtainMessage(Shimmer.MESSAGE_STATE_CHANGE,
						msg.arg1, -1, msg.obj).sendToTarget();
				switch (msg.arg1) {
				case Shimmer.STATE_CONNECTED:
					Log.d("Shimmer",
							((ObjectCluster) msg.obj).mBluetoothAddress + "  "
									+ ((ObjectCluster) msg.obj).mMyName);

					intent.putExtra("ShimmerBluetoothAddress",
							((ObjectCluster) msg.obj).mBluetoothAddress);
					intent.putExtra("ShimmerDeviceName",
							((ObjectCluster) msg.obj).mMyName);
					intent.putExtra("ShimmerState", Shimmer.STATE_CONNECTED);
					sendBroadcast(intent);

					break;
				case Shimmer.STATE_CONNECTING:
					intent.putExtra("ShimmerBluetoothAddress",
							((ObjectCluster) msg.obj).mBluetoothAddress);
					intent.putExtra("ShimmerDeviceName",
							((ObjectCluster) msg.obj).mMyName);
					intent.putExtra("ShimmerState", Shimmer.STATE_CONNECTING);
					break;
				case Shimmer.STATE_NONE:
					intent.putExtra("ShimmerBluetoothAddress",
							((ObjectCluster) msg.obj).mBluetoothAddress);
					intent.putExtra("ShimmerDeviceName",
							((ObjectCluster) msg.obj).mMyName);
					intent.putExtra("ShimmerState", Shimmer.STATE_NONE);
					sendBroadcast(intent);
					break;
				}

				break;

			case Shimmer.MESSAGE_STOP_STREAMING_COMPLETE:
				String address = msg.getData().getString("Bluetooth Address");
				boolean stop = msg.getData().getBoolean("Stop Streaming");
				if (stop == true) {
					closeAndRemoveFile(address);
				}
				break;
			}
		}
	};

	public void stopStreamingAllDevices() {
		// TODO Auto-generated method stub
		Collection<Object> colS = mMultiShimmer.values();
		Iterator<Object> iterator = colS.iterator();
		while (iterator.hasNext()) {
			Shimmer stemp = (Shimmer) iterator.next();

			if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED) {
				stemp.stopStreaming();

			}
		}
	}

	public void startStreamingAllDevices() {
		// TODO Auto-generated method stub
		Collection<Object> colS = mMultiShimmer.values();
		Iterator<Object> iterator = colS.iterator();
		while (iterator.hasNext()) {
			Shimmer stemp = (Shimmer) iterator.next();
			if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED) {
				stemp.startStreaming();
			}
		}
	}

	public void setEnableLogging(boolean enableLogging) {
		mEnableLogging = enableLogging;
		Log.d("Shimmer", "Logging :" + Boolean.toString(mEnableLogging));
	}

	public boolean getEnableLogging() {
		return mEnableLogging;
	}

	public void setAllSampingRate(double samplingRate) {
		// TODO Auto-generated method stub
		Collection<Object> colS = mMultiShimmer.values();
		Iterator<Object> iterator = colS.iterator();
		while (iterator.hasNext()) {
			Shimmer stemp = (Shimmer) iterator.next();
			if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED) {
				stemp.writeSamplingRate(samplingRate);
			}
		}
	}

	public void setAllAccelRange(int accelRange) {
		// TODO Auto-generated method stub
		Collection<Object> colS = mMultiShimmer.values();
		Iterator<Object> iterator = colS.iterator();
		while (iterator.hasNext()) {
			Shimmer stemp = (Shimmer) iterator.next();
			if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED) {
				stemp.writeAccelRange(accelRange);
			}
		}
	}

	public void setAllGSRRange(int gsrRange) {
		// TODO Auto-generated method stub
		Collection<Object> colS = mMultiShimmer.values();
		Iterator<Object> iterator = colS.iterator();
		while (iterator.hasNext()) {
			Shimmer stemp = (Shimmer) iterator.next();
			if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED) {
				stemp.writeGSRRange(gsrRange);
			}
		}
	}

	public void setAllEnabledSensors(int enabledSensors) {
		// TODO Auto-generated method stub
		Collection<Object> colS = mMultiShimmer.values();
		Iterator<Object> iterator = colS.iterator();
		while (iterator.hasNext()) {
			Shimmer stemp = (Shimmer) iterator.next();
			if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED) {
				stemp.writeEnabledSensors(enabledSensors);
			}
		}
	}

	public void setEnabledSensors(int enabledSensors, String bluetoothAddress) {
		// TODO Auto-generated method stub
		Collection<Object> colS = mMultiShimmer.values();
		Iterator<Object> iterator = colS.iterator();
		while (iterator.hasNext()) {
			Shimmer stemp = (Shimmer) iterator.next();
			if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED
					&& stemp.getBluetoothAddress().equals(bluetoothAddress)) {
				stemp.writeEnabledSensors(enabledSensors);
			}
		}
	}

	public void toggleLED(String bluetoothAddress) {
		// TODO Auto-generated method stub
		Collection<Object> colS = mMultiShimmer.values();
		Iterator<Object> iterator = colS.iterator();
		while (iterator.hasNext()) {
			Shimmer stemp = (Shimmer) iterator.next();
			if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED
					&& stemp.getBluetoothAddress().equals(bluetoothAddress)) {
				stemp.toggleLed();
			}
		}
	}

	public void writePMux(String bluetoothAddress, int setBit) {
		// TODO Auto-generated method stub
		Collection<Object> colS = mMultiShimmer.values();
		Iterator<Object> iterator = colS.iterator();
		while (iterator.hasNext()) {
			Shimmer stemp = (Shimmer) iterator.next();
			if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED
					&& stemp.getBluetoothAddress().equals(bluetoothAddress)) {
				stemp.writePMux(setBit);
			}
		}
	}

	public void write5VReg(String bluetoothAddress, int setBit) {
		// TODO Auto-generated method stub
		Collection<Object> colS = mMultiShimmer.values();
		Iterator<Object> iterator = colS.iterator();
		while (iterator.hasNext()) {
			Shimmer stemp = (Shimmer) iterator.next();
			if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED
					&& stemp.getBluetoothAddress().equals(bluetoothAddress)) {
				stemp.writeFiveVoltReg(setBit);
			}
		}
	}

	public int getEnabledSensors(String bluetoothAddress) {
		// TODO Auto-generated method stub
		Collection<Object> colS = mMultiShimmer.values();
		Iterator<Object> iterator = colS.iterator();
		int enabledSensors = 0;
		while (iterator.hasNext()) {
			Shimmer stemp = (Shimmer) iterator.next();
			if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED
					&& stemp.getBluetoothAddress().equals(bluetoothAddress)) {
				enabledSensors = stemp.getEnabledSensors();
			}
		}
		return enabledSensors;
	}

	public void writeSamplingRate(String bluetoothAddress, double samplingRate) {
		// TODO Auto-generated method stub
		Collection<Object> colS = mMultiShimmer.values();
		Iterator<Object> iterator = colS.iterator();
		while (iterator.hasNext()) {
			Shimmer stemp = (Shimmer) iterator.next();
			if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED
					&& stemp.getBluetoothAddress().equals(bluetoothAddress)) {
				stemp.writeSamplingRate(samplingRate);
			}
		}
	}

	public void writeAccelRange(String bluetoothAddress, int accelRange) {
		Collection<Object> colS = mMultiShimmer.values();
		Iterator<Object> iterator = colS.iterator();
		while (iterator.hasNext()) {
			Shimmer stemp = (Shimmer) iterator.next();
			if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED
					&& stemp.getBluetoothAddress().equals(bluetoothAddress)) {
				stemp.writeAccelRange(accelRange);
			}
		}
	}

	public void writeGyroRange(String bluetoothAddress, int range) {
		Collection<Object> colS = mMultiShimmer.values();
		Iterator<Object> iterator = colS.iterator();
		while (iterator.hasNext()) {
			Shimmer stemp = (Shimmer) iterator.next();
			if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED
					&& stemp.getBluetoothAddress().equals(bluetoothAddress)) {
				stemp.writeGyroRange(range);
			}
		}
	}

	public void writePressureResolution(String bluetoothAddress, int resolution) {
		Collection<Object> colS = mMultiShimmer.values();
		Iterator<Object> iterator = colS.iterator();
		while (iterator.hasNext()) {
			Shimmer stemp = (Shimmer) iterator.next();
			if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED
					&& stemp.getBluetoothAddress().equals(bluetoothAddress)) {
				// currently not supported
				stemp.writePressureResolution(resolution);
			}
		}
	}

	public void writeMagRange(String bluetoothAddress, int range) {
		Collection<Object> colS = mMultiShimmer.values();
		Iterator<Object> iterator = colS.iterator();
		while (iterator.hasNext()) {
			Shimmer stemp = (Shimmer) iterator.next();
			if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED
					&& stemp.getBluetoothAddress().equals(bluetoothAddress)) {
				stemp.writeMagRange(range);
			}
		}
	}

	public void writeGSRRange(String bluetoothAddress, int gsrRange) {
		Collection<Object> colS = mMultiShimmer.values();
		Iterator<Object> iterator = colS.iterator();
		while (iterator.hasNext()) {
			Shimmer stemp = (Shimmer) iterator.next();
			if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED
					&& stemp.getBluetoothAddress().equals(bluetoothAddress)) {
				stemp.writeGSRRange(gsrRange);
			}
		}
	}

	public double getSamplingRate(String bluetoothAddress) {
		// TODO Auto-generated method stub

		Collection<Object> colS = mMultiShimmer.values();
		Iterator<Object> iterator = colS.iterator();
		double SRate = -1;
		while (iterator.hasNext()) {
			Shimmer stemp = (Shimmer) iterator.next();
			if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED
					&& stemp.getBluetoothAddress().equals(bluetoothAddress)) {
				SRate = stemp.getSamplingRate();
			}
		}
		return SRate;
	}

	public int getAccelRange(String bluetoothAddress) {
		// TODO Auto-generated method stub
		Collection<Object> colS = mMultiShimmer.values();
		Iterator<Object> iterator = colS.iterator();
		int aRange = -1;
		while (iterator.hasNext()) {
			Shimmer stemp = (Shimmer) iterator.next();
			if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED
					&& stemp.getBluetoothAddress().equals(bluetoothAddress)) {
				aRange = stemp.getAccelRange();
			}
		}
		return aRange;
	}

	public int getShimmerState(String bluetoothAddress) {

		// TODO Auto-generated method stub
		Collection<Object> colS = mMultiShimmer.values();
		Iterator<Object> iterator = colS.iterator();
		int status = -1;
		while (iterator.hasNext()) {
			Shimmer stemp = (Shimmer) iterator.next();
			if (stemp.getBluetoothAddress().equals(bluetoothAddress)) {
				status = stemp.getShimmerState();
				Log.d("ShimmerState", Integer.toString(status));
			}
		}
		return status;

	}

	public int getGSRRange(String bluetoothAddress) {
		// TODO Auto-generated method stub
		Collection<Object> colS = mMultiShimmer.values();
		Iterator<Object> iterator = colS.iterator();
		int gRange = -1;
		while (iterator.hasNext()) {
			Shimmer stemp = (Shimmer) iterator.next();
			if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED
					&& stemp.getBluetoothAddress().equals(bluetoothAddress)) {
				gRange = stemp.getGSRRange();
			}
		}
		return gRange;
	}

	public int get5VReg(String bluetoothAddress) {
		// TODO Auto-generated method stub
		Collection<Object> colS = mMultiShimmer.values();
		Iterator<Object> iterator = colS.iterator();
		int fiveVReg = -1;
		while (iterator.hasNext()) {
			Shimmer stemp = (Shimmer) iterator.next();
			if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED
					&& stemp.getBluetoothAddress().equals(bluetoothAddress)) {
				fiveVReg = stemp.get5VReg();
			}
		}
		return fiveVReg;
	}

	public boolean isLowPowerMagEnabled(String bluetoothAddress) {
		// TODO Auto-generated method stub
		Collection<Object> colS = mMultiShimmer.values();
		Iterator<Object> iterator = colS.iterator();
		boolean enabled = false;
		while (iterator.hasNext()) {
			Shimmer stemp = (Shimmer) iterator.next();
			if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED
					&& stemp.getBluetoothAddress().equals(bluetoothAddress)) {
				enabled = stemp.isLowPowerMagEnabled();
			}
		}
		return enabled;
	}

	public int getpmux(String bluetoothAddress) {
		// TODO Auto-generated method stub
		Collection<Object> colS = mMultiShimmer.values();
		Iterator<Object> iterator = colS.iterator();
		int pmux = -1;
		while (iterator.hasNext()) {
			Shimmer stemp = (Shimmer) iterator.next();
			if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED
					&& stemp.getBluetoothAddress().equals(bluetoothAddress)) {
				pmux = stemp.getPMux();
			}
		}
		return pmux;
	}

	public void startStreaming(String bluetoothAddress) {
		// TODO Auto-generated method stub
		Collection<Object> colS = mMultiShimmer.values();
		Iterator<Object> iterator = colS.iterator();
		while (iterator.hasNext()) {
			Shimmer stemp = (Shimmer) iterator.next();
			if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED
					&& stemp.getBluetoothAddress().equals(bluetoothAddress)) {
				stemp.startStreaming();
			}
		}
	}

	public int sensorConflictCheckandCorrection(String bluetoothAddress,
			int enabledSensors, int sensorToCheck) {
		// TODO Auto-generated method stub
		int newSensorBitmap = 0;
		Collection<Object> colS = mMultiShimmer.values();
		Iterator<Object> iterator = colS.iterator();
		while (iterator.hasNext()) {
			Shimmer stemp = (Shimmer) iterator.next();
			if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED
					&& stemp.getBluetoothAddress().equals(bluetoothAddress)) {
				newSensorBitmap = stemp.sensorConflictCheckandCorrection(
						enabledSensors, sensorToCheck);
			}
		}
		return newSensorBitmap;
	}

	public List<String> getListofEnabledSensors(String bluetoothAddress) {
		// TODO Auto-generated method stub
		List<String> listofSensors = null;
		Collection<Object> colS = mMultiShimmer.values();
		Iterator<Object> iterator = colS.iterator();
		while (iterator.hasNext()) {
			Shimmer stemp = (Shimmer) iterator.next();
			if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED
					&& stemp.getBluetoothAddress().equals(bluetoothAddress)) {
				listofSensors = stemp.getListofEnabledSensors();
			}
		}
		return listofSensors;
	}

	public String[] getListofSignals(String bluetoothAddress) {
		// TODO Auto-generated method stub
		String[] listofSignals = null;
		Collection<Object> colS = mMultiShimmer.values();
		Iterator<Object> iterator = colS.iterator();
		while (iterator.hasNext()) {
			Shimmer stemp = (Shimmer) iterator.next();
			if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED
					&& stemp.getBluetoothAddress().equals(bluetoothAddress)) {
				listofSignals = stemp.getListofSignals();
			}
		}
		return listofSignals;
	}

	public void stopStreaming(String bluetoothAddress) {
		// TODO Auto-generated method stub
		Collection<Object> colS = mMultiShimmer.values();
		Iterator<Object> iterator = colS.iterator();
		while (iterator.hasNext()) {
			Shimmer stemp = (Shimmer) iterator.next();
			if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED
					&& stemp.getBluetoothAddress().equals(bluetoothAddress)) {
				stemp.stopStreaming();
			}
		}
	}

	public void setBlinkLEDCMD(String bluetoothAddress) {
		// TODO Auto-generated method stub
		Shimmer stemp = (Shimmer) mMultiShimmer.get(bluetoothAddress);
		if (stemp != null) {
			if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED
					&& stemp.getBluetoothAddress().equals(bluetoothAddress)) {
				if (stemp.getCurrentLEDStatus() == 0) {
					stemp.writeLEDCommand(1);
				} else {
					stemp.writeLEDCommand(0);
				}
			}
		}

	}

	public void enableLowPowerMag(String bluetoothAddress, boolean enable) {
		// TODO Auto-generated method stub
		Shimmer stemp = (Shimmer) mMultiShimmer.get(bluetoothAddress);
		if (stemp != null) {
			if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED
					&& stemp.getBluetoothAddress().equals(bluetoothAddress)) {
				stemp.enableLowPowerMag(enable);
			}
		}
	}

	public void setBattLimitWarning(String bluetoothAddress, double limit) {
		// TODO Auto-generated method stub
		Shimmer stemp = (Shimmer) mMultiShimmer.get(bluetoothAddress);
		if (stemp != null) {
			if (stemp.getBluetoothAddress().equals(bluetoothAddress)) {
				stemp.setBattLimitWarning(limit);
			}
		}

	}

	public double getBattLimitWarning(String bluetoothAddress) {
		// TODO Auto-generated method stub
		double limit = -1;
		Shimmer stemp = (Shimmer) mMultiShimmer.get(bluetoothAddress);
		if (stemp != null) {
			if (stemp.getBluetoothAddress().equals(bluetoothAddress)) {
				limit = stemp.getBattLimitWarning();
			}
		}
		return limit;
	}

	public double getPacketReceptionRate(String bluetoothAddress) {
		// TODO Auto-generated method stub
		double rate = -1;
		Shimmer stemp = (Shimmer) mMultiShimmer.get(bluetoothAddress);
		if (stemp != null) {
			if (stemp.getBluetoothAddress().equals(bluetoothAddress)) {
				rate = stemp.getPacketReceptionRate();
			}
		}
		return rate;
	}

	public void disconnectShimmer(String bluetoothAddress) {
		Collection<Object> colS = mMultiShimmer.values();
		Iterator<Object> iterator = colS.iterator();
		while (iterator.hasNext()) {
			Shimmer stemp = (Shimmer) iterator.next();
			if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED
					&& stemp.getBluetoothAddress().equals(bluetoothAddress)) {
				stemp.stop();

			}
		}

		mLogShimmer.remove(bluetoothAddress);
		mMultiShimmer.remove(bluetoothAddress);

	}

	/**
	 * @param handler
	 *            Used by Shimmer class to pass data to main thread so that it
	 *            can be graphed
	 */
	public void setGraphHandler(Handler handler) {
		mHandlerGraph = handler;

	}

	public void enableGraphingHandler(boolean setting) {
		mGraphing = setting;
	}

	public boolean DevicesConnected(String bluetoothAddress) {
		boolean deviceConnected = false;
		Collection<Object> colS = mMultiShimmer.values();
		Iterator<Object> iterator = colS.iterator();
		while (iterator.hasNext()) {
			Shimmer stemp = (Shimmer) iterator.next();
			if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED
					&& stemp.getBluetoothAddress().equals(bluetoothAddress)) {
				deviceConnected = true;
			}
		}
		return deviceConnected;
	}

	public boolean DeviceIsStreaming(String bluetoothAddress) {
		boolean deviceStreaming = false;
		Collection<Object> colS = mMultiShimmer.values();
		Iterator<Object> iterator = colS.iterator();
		while (iterator.hasNext()) {
			Shimmer stemp = (Shimmer) iterator.next();
			if (stemp.getStreamingStatus() == true
					&& stemp.getBluetoothAddress().equals(bluetoothAddress)) {
				deviceStreaming = true;
			}
		}
		return deviceStreaming;
	}

	public boolean GetInstructionStatus(String bluetoothAddress) {
		boolean instructionStatus = false;
		Collection<Object> colS = mMultiShimmer.values();
		Iterator<Object> iterator = colS.iterator();
		while (iterator.hasNext()) {
			Shimmer stemp = (Shimmer) iterator.next();
			if (stemp.getBluetoothAddress().equals(bluetoothAddress)) {
				instructionStatus = stemp.getInstructionStatus();
			}
		}
		return instructionStatus;
	}

	public void setLoggingName(String name) {
		mLogFileName = name;
	}

	public void closeAndRemoveFile(String bluetoothAddress) {
		if (mEnableLogging == true && mLogShimmer.get(bluetoothAddress) != null) {
			mLogShimmer.get(bluetoothAddress).closeFile();
			mLogShimmer.remove(bluetoothAddress);
		}
	}

	public double getFWVersion(String bluetoothAddress) {
		double version = 0;
		Shimmer stemp = (Shimmer) mMultiShimmer.get(bluetoothAddress);
		if (stemp != null) {
			version = stemp.getFirmwareVersion();
		}
		return version;
	}

	public int getShimmerVersion(String bluetoothAddress) {
		int version = 0;
		Shimmer stemp = (Shimmer) mMultiShimmer.get(bluetoothAddress);
		if (stemp != null) {
			version = stemp.getShimmerVersion();
		}
		return version;
	}

	public int getAccelSmartMode(String bluetoothAddress) {
		int setting = 0;
		Shimmer stemp = (Shimmer) mMultiShimmer.get(bluetoothAddress);
		if (stemp != null) {
			setting = stemp.getAccelerometerSpecialMode();
		}
		return setting;
	}

	public Shimmer getShimmer(String bluetoothAddress) {
		// TODO Auto-generated method stub
		Shimmer shimmer = null;
		Collection<Object> colS = mMultiShimmer.values();
		Iterator<Object> iterator = colS.iterator();
		while (iterator.hasNext()) {
			Shimmer stemp = (Shimmer) iterator.next();
			if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED
					&& stemp.getBluetoothAddress().equals(bluetoothAddress)) {
				return stemp;
			}
		}
		return shimmer;
	}

	public void test() {
		Log.d("ShimmerTest", "Test");
	}

}
