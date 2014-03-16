package com.george.biosense.activity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.george.biosense.processing.QRSDetector;
import com.george.biosense.service.ShimmerService;
import com.george.biosense.service.ShimmerService.LocalBinder;
import com.george.delta.deltatest.R;
import com.google.common.collect.BiMap;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;
import com.shimmerresearch.driver.FormatCluster;
import com.shimmerresearch.driver.ObjectCluster;
import com.shimmerresearch.driver.Shimmer;
import com.shimmerresearch.tools.Logging;

/**
 * @author Joro Main UI thread and Activty. MainActivity extends ServiceActivity
 *         which contains some general mathods that are necessary to use
 *         ShimmerService.
 * 
 */
public class MainActivity extends ServiceActivity {

	// Requests that can be made to ShimmerService
	static final int REQUEST_ENABLE_BT = 1;
	static final int REQUEST_CONNECT_SHIMMER = 2;
	static final int REQUEST_CONFIGURE_SHIMMER = 3;
	static final int REQUEST_CONFIGURE_VIEW_SENSOR = 4;
	static final int REQUEST_COMMAND_SHIMMER = 5;
	static final int REQUEST_LOGFILE_SHIMMER = 6;
	static final int REQUEST_PROCESS_FILENAME = 7;

	// Views for titles
	private static TextView mTitle;
	private static TextView mTitleLogging;
	private static TextView mTextSensor1;
	private static TextView mTextSensor2;
	private static TextView mTextSensor3;
	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;
	// Name of the connected device
	private static String mBluetoothAddress = null;
	// Logging of received data enabled
	private static boolean mEnableLogging = false;
	public static String outputFolder = "DTU_Biosense";

	private static double processedCalValue;
	private static QRSDetector mQRSDetector;

	// Dialog for displaying available sensors on shimmer device
	Dialog mDialog;
	int dialogEnabledSensors = 0;
	// Graphing Views and Series holders
	private static GraphView graphViewLine1;
	private static GraphView graphViewLine2;
	private static GraphViewSeries sensorGraphSeries1;
	private static GraphViewSeries sensorGraphSeries2;
	private static GraphViewSeries sensorGraphSeries3;
	private static GraphViewSeries processedGraphSeries;

	private static int mGraphSubSamplingCount = 0; // 10
	private static int num;
	private static int visibleXRange = 1024;

	private static String mSensorView = ""; // The sensor device which should be
	// viewed on the graph
	private static String mFileName = "myFirstDataSet";
	static Logging log = new Logging(mFileName, " ,");
	// Context of application
	static Context context;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		context = this;
		// Dialog for pop up windows
		mDialog = new Dialog(this);
		// Set up the custom title bar
		mTitle = (TextView) findViewById(R.id.title_right_text);
		mTitleLogging = (TextView) findViewById(R.id.title_left_text);
		mTextSensor1 = (TextView) findViewById(R.id.LabelSensor1);
		mTextSensor2 = (TextView) findViewById(R.id.LabelSensor2);
		mTextSensor3 = (TextView) findViewById(R.id.LabelSensor3);
		num = 1;
		// Setting up graphs
		graphViewLine1 = new LineGraphView(this, "Raw Data");
		graphViewLine2 = new LineGraphView(this, "Processed Data");
		setupLineGraph(num, visibleXRange);
		
		mQRSDetector = new QRSDetector();
		// Check if service is already running
		if (!isMyServiceRunning()) {
			Log.d("ShimmerH", "Oncreate2");
			Intent intent = new Intent(this, ShimmerService.class);
			startService(intent);
			if (mServiceFirstTime == true) {
				Log.d("ShimmerH", "Oncreate3");
				getApplicationContext().bindService(intent,
						mTestServiceConnection, Context.BIND_AUTO_CREATE);
				mServiceFirstTime = false;
			}
			mTitle.setText(R.string.title_not_connected); // if no service is
															// running means no
															// devices are
															// connected
		}

		if (mBluetoothAddress != null) {
			mTitle.setText(R.string.title_connected_to);
			mTitle.append(mBluetoothAddress);
		}

		if (mEnableLogging == false) {
			mTitleLogging.setText("Logging Disabled");
		} else if (mEnableLogging == true) {
			mTitleLogging.setText("Logging Enabled");
		}

		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
			Toast.makeText(this,
					"Device does not support Bluetooth\nExiting...",
					Toast.LENGTH_LONG).show();
			finish();
		}

	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();

		// Make sure that Bluetooth is enabled
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		} else {

		}

		// Bind service to activity
		Log.d("Shimmer", "On Resume");
		Intent intent = new Intent(this, ShimmerService.class);
		Log.d("ShimmerH", "on Resume");
		getApplicationContext().bindService(intent, mTestServiceConnection,
				Context.BIND_AUTO_CREATE);

	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		Log.d("Shimmer", "On Pause");
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		// Unbind service when activity is destroyed
		if (mServiceBind == true) {
			getApplicationContext().unbindService(mTestServiceConnection);
		}
		Log.d("Shimmer", "On Pause");
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		// disable graph edit for sensors which are not enabled

		MenuItem scanItem = menu.findItem(R.id.menu_scan);
		MenuItem streamItem = menu.findItem(R.id.menu_stream);
		MenuItem settingsItem = menu.findItem(R.id.menu_settings);
		MenuItem commandsItem = menu.findItem(R.id.menu_commands);
		MenuItem viewItem = menu.findItem(R.id.menu_viewsensor);
		MenuItem processItem = menu.findItem(R.id.menu_process);
		if ((mService.DevicesConnected(mBluetoothAddress) == true)) {
			scanItem.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
			scanItem.setTitle(R.string.disconnect);
			streamItem.setIcon(R.drawable.ic_action_play);
			streamItem.setTitle(R.string.startstream);
			streamItem.setEnabled(true);
			settingsItem.setEnabled(true);
			commandsItem.setEnabled(true);
			viewItem.setEnabled(true);
		} else {
			scanItem.setIcon(android.R.drawable.ic_menu_search);
			scanItem.setTitle(R.string.connect);
			streamItem.setIcon(R.drawable.ic_action_play);
			streamItem.setEnabled(false);
			settingsItem.setEnabled(false);
			commandsItem.setEnabled(false);
			viewItem.setEnabled(false);
		}
		if (mService.DeviceIsStreaming(mBluetoothAddress) == true
				&& mService.DevicesConnected(mBluetoothAddress) == true) {
			streamItem.setIcon(R.drawable.ic_action_stop);
			streamItem.setTitle(R.string.stopstream);

		}
		if (mService.DeviceIsStreaming(mBluetoothAddress) == false
				&& mService.DevicesConnected(mBluetoothAddress) == true
				&& mService.GetInstructionStatus(mBluetoothAddress) == true) {
			streamItem.setIcon(R.drawable.ic_action_play);
			streamItem.setTitle(R.string.startstream);
		}
		if (mService.GetInstructionStatus(mBluetoothAddress) == false
				|| (mService.GetInstructionStatus(mBluetoothAddress) == false)) {
			streamItem.setEnabled(false);
			settingsItem.setEnabled(false);
			commandsItem.setEnabled(false);
		}
		if (mService.DeviceIsStreaming(mBluetoothAddress)) {
			settingsItem.setEnabled(false);
			commandsItem.setEnabled(false);
			processItem.setEnabled(false);
		} else {
			processItem.setEnabled(true);
		}
		if (mService.GetInstructionStatus(mBluetoothAddress) == false) {
			streamItem.setEnabled(false);
			settingsItem.setEnabled(false);
			commandsItem.setEnabled(false);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub

		switch (item.getItemId()) {
		case R.id.menu_scan:
			if ((mService.DevicesConnected(mBluetoothAddress) == true)) {
				mService.disconnectAllDevices();
			} else {
				Intent serverIntent = new Intent(this, DeviceListActivity.class);
				startActivityForResult(serverIntent, REQUEST_CONNECT_SHIMMER);
			}
			return true;
		case R.id.menu_stream:
			if (mService.DeviceIsStreaming(mBluetoothAddress) == true) {
				mService.stopStreaming(mBluetoothAddress);
			} else {
				sensorGraphSeries1.resetData(new GraphViewData[] {new GraphViewData(0,1)});
				sensorGraphSeries2.resetData(new GraphViewData[] {new GraphViewData(0,1)});
				processedGraphSeries.resetData(new GraphViewData[] {new GraphViewData(0,1)});
				mService.startStreaming(mBluetoothAddress);
				log = new Logging(mFileName, " ,");
			}
			return true;
		case R.id.menu_process:
			Intent processPromptIntent = new Intent(this,
					com.george.biosense.processing.ProcessDialog.class);
			startActivityForResult(processPromptIntent,
					REQUEST_PROCESS_FILENAME);
			return true;
		case R.id.menu_logfile:
			Intent logfileIntent = new Intent(this, LogFileActivity.class);
			startActivityForResult(logfileIntent, REQUEST_LOGFILE_SHIMMER);
			return true;
		case R.id.menu_settings:
			Shimmer shimmer = mService.getShimmer(mBluetoothAddress);
			showEnableSensors(shimmer.getListofSupportedSensors(),
					mService.getEnabledSensors(mBluetoothAddress));
			return true;
		case R.id.menu_viewsensor:
			showSelectSensorPlot();
			return true;
		case R.id.menu_commands:
			Intent commandIntent = new Intent(this, CommandsActivity.class);
			commandIntent.putExtra("BluetoothAddress", mBluetoothAddress);
			commandIntent.putExtra("SamplingRate",
					mService.getSamplingRate(mBluetoothAddress));
			commandIntent.putExtra("AccelerometerRange",
					mService.getAccelRange(mBluetoothAddress));
			commandIntent.putExtra("GSRRange",
					mService.getGSRRange(mBluetoothAddress));
			commandIntent.putExtra("BatteryLimit",
					mService.getBattLimitWarning(mBluetoothAddress));
			startActivityForResult(commandIntent, REQUEST_COMMAND_SHIMMER);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/*
	 * Called when an activity returns a results after it was started by
	 * startActivityForResult
	 */
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		switch (requestCode) {
		case REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {

				// setMessage("\nBluetooth is now enabled");
				Toast.makeText(this, "Bluetooth is now enabled",
						Toast.LENGTH_SHORT).show();
			} else {
				// User did not enable Bluetooth or an error occured
				Toast.makeText(this, "Bluetooth not enabled\nExiting...",
						Toast.LENGTH_SHORT).show();
				finish();
			}
			break;
		case REQUEST_CONNECT_SHIMMER:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				String address = data.getExtras().getString(
						DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				Log.d("Shimmer", address);
				mService.connectShimmer(address, "Device");
				mBluetoothAddress = address;
				mService.setGraphHandler(mHandler);
				// mShimmerDevice.connect(address,"gerdavax");
				// mShimmerDevice.setgetdatainstruction("a");
			}
			break;
		case REQUEST_COMMAND_SHIMMER:

			if (resultCode == Activity.RESULT_OK) {
				if (data.getExtras().getBoolean("ToggleLED", false) == true) {
					mService.toggleAllLEDS();
				}

				if (data.getExtras().getDouble("SamplingRate", -1) != -1) {
					mService.writeSamplingRate(mBluetoothAddress, data
							.getExtras().getDouble("SamplingRate", -1));
					Log.d("Shimmer",
							Double.toString(data.getExtras().getDouble(
									"SamplingRate", -1)));
					mGraphSubSamplingCount = 0;
				}

				if (data.getExtras().getInt("AccelRange", -1) != -1) {
					mService.writeAccelRange(mBluetoothAddress, data
							.getExtras().getInt("AccelRange", -1));
				}

				if (data.getExtras().getInt("GyroRange", -1) != -1) {
					mService.writeGyroRange(mBluetoothAddress, data.getExtras()
							.getInt("GyroRange", -1));
				}

				if (data.getExtras().getInt("PressureResolution", -1) != -1) {
					mService.writePressureResolution(mBluetoothAddress, data
							.getExtras().getInt("PressureResolution", -1));
				}

				if (data.getExtras().getInt("MagRange", -1) != -1) {
					mService.writeMagRange(mBluetoothAddress, data.getExtras()
							.getInt("MagRange", -1));
				}

				if (data.getExtras().getInt("GSRRange", -1) != -1) {
					mService.writeGSRRange(mBluetoothAddress, data.getExtras()
							.getInt("GSRRange", -1));
				}
				if (data.getExtras().getDouble("BatteryLimit", -1) != -1) {
					mService.setBattLimitWarning(mBluetoothAddress, data
							.getExtras().getDouble("BatteryLimit", -1));
				}

			}
			break;
		case REQUEST_LOGFILE_SHIMMER:
			Log.d("Shimmer", "iam");
			if (resultCode == Activity.RESULT_OK) {
				mEnableLogging = data.getExtras().getBoolean(
						"LogFileEnableLogging");
				if (mEnableLogging == true) {
					mService.setEnableLogging(mEnableLogging);
				}
				// set the filename in the LogFile
				mFileName = data.getExtras().getString("LogFileName");
				mService.setLoggingName(mFileName);

				if (mEnableLogging == false) {
					mTitleLogging.setText("Logging Disabled");
				} else if (mEnableLogging == true) {
					mTitleLogging.setText("Logging Enabled");
				}

			}
			break;
		case REQUEST_PROCESS_FILENAME:
			if (resultCode == Activity.RESULT_OK) {
				mFileName = data.getExtras().getString("processFileName");
				new ProcessTask().execute();
			}
			break;
		}

	}

	protected ServiceConnection mTestServiceConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName arg0, IBinder service) {
			// TODO Auto-generated method stub
			Log.d("ShimmerService", "service connected from main activity");
			LocalBinder binder = (LocalBinder) service;
			mService = binder.getService();
			mServiceBind = true;
			mService.setGraphHandler(mHandler);
		}

		public void onServiceDisconnected(ComponentName arg0) {
			// TODO Auto-generated method stub
			mServiceBind = false;
		}
	};

	/**
	 * Handler passed to ShimmerService so that it can communicate with main UI
	 * thread
	 */
	private static Handler mHandler = new Handler() {

		public void handleMessage(Message msg) {
			Log.d("Shimmer", "msg");
			switch (msg.what) {
			case Shimmer.MESSAGE_STATE_CHANGE:
				switch (msg.arg1) {
				case Shimmer.STATE_CONNECTED:
					// this has been deprecated
					/*
					 * Log.d("Shimmer","ms1");
					 * mTitle.setText(R.string.title_connected_to);
					 * mBluetoothAddress
					 * =((ObjectCluster)msg.obj).mBluetoothAddress;
					 * mTitle.append(mBluetoothAddress);
					 * mService.enableGraphingHandler(true);
					 */
					break;
				case Shimmer.MSG_STATE_FULLY_INITIALIZED:
					Log.d("Shimmer", "ms1");
					mTitle.setText(R.string.title_connected_to);
					mBluetoothAddress = ((ObjectCluster) msg.obj).mBluetoothAddress;
					mTitle.append(mBluetoothAddress);
					mService.enableGraphingHandler(true);
					break;
				case Shimmer.STATE_CONNECTING:
					Log.d("Shimmer", "msg2");
					mTitle.setText(R.string.title_connecting);
					break;
				case Shimmer.STATE_NONE:
					Log.d("Shimmer", "msg3");
					mTitle.setText(R.string.title_not_connected);
					mBluetoothAddress = null;
					// this also stops streaming
					break;
				}
				break;
			case Shimmer.MESSAGE_READ:

				if ((msg.obj instanceof ObjectCluster)) {
					ObjectCluster objectCluster = (ObjectCluster) msg.obj;
					Log.d("Shimmer", "MSGREAD");

					int[] dataArray = new int[0];
					double[] calibratedDataArray = new double[0];
					String[] sensorName = new String[0];
					String units = "";
					String calibratedUnits = "";
					String calibratedUnits2 = "";
					// mSensorView determines which sensor to graph
					if (mSensorView.equals("Accelerometer")) {
						sensorName = new String[3]; // for x y and z axis
						dataArray = new int[3];
						calibratedDataArray = new double[3];
						sensorName[0] = "Accelerometer X";
						sensorName[1] = "Accelerometer Y";
						sensorName[2] = "Accelerometer Z";
						if (mService.getShimmerVersion(mBluetoothAddress) == Shimmer.SHIMMER_3) {
							if (mService.getAccelRange(mBluetoothAddress) == 0) {
								units = "u12"; // units are just merely an
												// indicator to correct the
												// graph
							} else {
								units = "i16";
							}
						} else {
							units = "u12";
						}
					}
					if (mSensorView.equals("Gyroscope")) {
						sensorName = new String[3]; // for x y and z axis
						dataArray = new int[3];
						calibratedDataArray = new double[3];
						sensorName[0] = "Gyroscope X";
						sensorName[1] = "Gyroscope Y";
						sensorName[2] = "Gyroscope Z";
						units = "i16";
					}
					if (mSensorView.equals("Magnetometer")) {
						sensorName = new String[3]; // for x y and z axis
						dataArray = new int[3];
						calibratedDataArray = new double[3];
						sensorName[0] = "Magnetometer X";
						sensorName[1] = "Magnetometer Y";
						sensorName[2] = "Magnetometer Z";
						units = "i12";
					}
					if (mSensorView.equals("GSR")) {
						sensorName = new String[1];
						dataArray = new int[1];
						calibratedDataArray = new double[1];
						sensorName[0] = "GSR";
						units = "u16";
					}
					if (mSensorView.equals("EMG")) {
						sensorName = new String[1];
						dataArray = new int[1];
						calibratedDataArray = new double[1];
						sensorName[0] = "EMG";
						units = "u12";
					}
					if (mSensorView.equals("ECG")) {
						sensorName = new String[2];
						dataArray = new int[2];
						calibratedDataArray = new double[2];
						sensorName[0] = "ECG RA-LL";
						sensorName[1] = "ECG LA-LL";
						units = "u12";
					}
					if (mSensorView.equals("Strain Gauge")) {
						sensorName = new String[2];
						dataArray = new int[2];
						calibratedDataArray = new double[2];
						sensorName[0] = "Strain Gauge High";
						sensorName[1] = "Strain Gauge Low";
						units = "u12";
					}
					if (mSensorView.equals("Heart Rate")) {
						sensorName = new String[1];
						dataArray = new int[1];
						calibratedDataArray = new double[1];
						sensorName[0] = "Heart Rate";
						units = "u8";
						if (mService.getFWVersion(mBluetoothAddress) > 0.1) {
							units = "u16";
						}
					}
					if (mSensorView.equals("ExpBoardA0")) {
						sensorName = new String[1];
						dataArray = new int[1];
						calibratedDataArray = new double[1];
						sensorName[0] = "ExpBoard A0";
						units = "u12";
					}
					if (mSensorView.equals("ExpBoardA7")) {
						sensorName = new String[1];
						dataArray = new int[1];
						calibratedDataArray = new double[1];
						sensorName[0] = "ExpBoard A7";
						units = "u12";
					}
					if (mSensorView.equals("Battery Voltage")) {
						sensorName = new String[2];
						dataArray = new int[2];
						calibratedDataArray = new double[2];
						sensorName[0] = "VSenseReg";
						sensorName[1] = "VSenseBatt";
						units = "u12";
					}
					if (mSensorView.equals("Timestamp")) {
						sensorName = new String[1];
						dataArray = new int[1];
						calibratedDataArray = new double[1];
						sensorName[0] = "Timestamp";
						units = "u16";
					}
					if (mSensorView.equals("External ADC A7")) {
						sensorName = new String[1];
						dataArray = new int[1];
						calibratedDataArray = new double[1];
						sensorName[0] = "External ADC A7";
						units = "u16";
					}
					if (mSensorView.equals("External ADC A6")) {
						sensorName = new String[1];
						dataArray = new int[1];
						calibratedDataArray = new double[1];
						sensorName[0] = "External ADC A6";
						units = "u16";
					}
					if (mSensorView.equals("External ADC A15")) {
						sensorName = new String[1];
						dataArray = new int[1];
						calibratedDataArray = new double[1];
						sensorName[0] = "External ADC A15";
						units = "u16";
					}
					if (mSensorView.equals("Internal ADC A1")) {
						sensorName = new String[1];
						dataArray = new int[1];
						calibratedDataArray = new double[1];
						sensorName[0] = "Internal ADC A1";
						units = "u16";
					}
					if (mSensorView.equals("Internal ADC A12")) {
						sensorName = new String[1];
						dataArray = new int[1];
						calibratedDataArray = new double[1];
						sensorName[0] = "Internal ADC A12";
						units = "u16";
					}
					if (mSensorView.equals("Internal ADC A13")) {
						sensorName = new String[1];
						dataArray = new int[1];
						calibratedDataArray = new double[1];
						sensorName[0] = "Internal ADC A13";
						units = "u16";
					}
					if (mSensorView.equals("Internal ADC A14")) {
						sensorName = new String[1];
						dataArray = new int[1];
						calibratedDataArray = new double[1];
						sensorName[0] = "Internal ADC A14";
						units = "u16";
					}
					if (mSensorView.equals("Pressure")) {
						sensorName = new String[2];
						dataArray = new int[2];
						calibratedDataArray = new double[2];
						sensorName[0] = "Pressure";
						sensorName[1] = "Temperature";
						units = "u16";
					}
					String deviceName = objectCluster.mMyName;
					// log data

					if (sensorName.length != 0) { // Device 1 is the assigned
													// user id, see constructor
													// of the Shimmer
						if (sensorName.length > 0) {

							Collection<FormatCluster> ofFormats = objectCluster.mPropertyCluster
									.get(sensorName[0]); // first retrieve all
															// the possible
															// formats for the
															// current sensor
															// device
							FormatCluster formatCluster = ((FormatCluster) ObjectCluster
									.returnFormatCluster(ofFormats, "CAL"));
							if (formatCluster != null) {
								// Obtain data for text view
								calibratedDataArray[0] = formatCluster.mData;
								calibratedUnits = formatCluster.mUnits;
								Log.d("Shimmer", "MSGREAD2");
								// Obtain data for graph

								dataArray[0] = (int) ((FormatCluster) ObjectCluster
										.returnFormatCluster(ofFormats, "RAW")).mData;

							}
						}
						if (sensorName.length > 1) {
							Collection<FormatCluster> ofFormats = objectCluster.mPropertyCluster
									.get(sensorName[1]); // first retrieve all
															// the possible
															// formats for the
															// current sensor
															// device
							FormatCluster formatCluster = ((FormatCluster) ObjectCluster
									.returnFormatCluster(ofFormats, "CAL"));
							if (formatCluster != null) {
								calibratedDataArray[1] = formatCluster.mData;
								// Obtain data for text view
								calibratedUnits2 = formatCluster.mUnits;
								// Obtain data for graph
								dataArray[1] = (int) ((FormatCluster) ObjectCluster
										.returnFormatCluster(ofFormats, "RAW")).mData;

							}
						}
						if (sensorName.length > 2) {

							Collection<FormatCluster> ofFormats = objectCluster.mPropertyCluster
									.get(sensorName[2]); // first retrieve all
															// the possible
															// formats for the
															// current sensor
															// device
							FormatCluster formatCluster = ((FormatCluster) ObjectCluster
									.returnFormatCluster(ofFormats, "CAL"));
							if (formatCluster != null) {
								calibratedDataArray[2] = formatCluster.mData;

								// Obtain data for graph
								dataArray[2] = (int) ((FormatCluster) ObjectCluster
										.returnFormatCluster(ofFormats, "RAW")).mData;

							}

						}

						// in order to prevent LAG the number of data points
						// plotted is REDUCED
						int maxNumberofSamplesPerSecond = 100; // Change this to
																// increase/decrease
																// the number of
																// samples which
																// are graphed

						// Process received calibrated value of calibrate data
						// **** Depending on what type of signal you are
						// processing you might want to change
						// the member of the calibratedDataArray you are using.
						// This implementation is specific for ECGs.
						processedCalValue = mQRSDetector.isQRS(calibratedDataArray[0]);
						int subSamplingCount = 0;

						if (mService.getSamplingRate(mBluetoothAddress) > maxNumberofSamplesPerSecond) {
							subSamplingCount = (int) (mService
									.getSamplingRate(mBluetoothAddress) / maxNumberofSamplesPerSecond);
							mGraphSubSamplingCount++;
							Log.d("SSC", Integer.toString(subSamplingCount));
						}
						if (mGraphSubSamplingCount == subSamplingCount) {
							// if (!mSensorView.equals("Timestamp")) {
							// graphViewLine1.setManualYAxisBounds(1, -1);
							// graphViewLine2.setManualYAxisBounds(1, -1);
							// } else {
							// graphViewLine1.setManualYAxis(false);
							// graphViewLine2.setManualYAxis(false);
							// }
							graphViewLine1.setManualYAxis(false);
							graphViewLine2.setManualYAxis(false);

							if (calibratedDataArray.length > 0) {
								sensorGraphSeries1.appendData(
										new GraphViewData(num,
												calibratedDataArray[0]), true,
										1024);
							}
							if (calibratedDataArray.length > 1) {
								sensorGraphSeries2.appendData(
										new GraphViewData(num,
												calibratedDataArray[1]), true,
										1024);
							}
							processedGraphSeries.appendData(new GraphViewData(
									num, processedCalValue), true, 1024);
							// Test to avoid overflow of counter
							if (num < Integer.MAX_VALUE) {
								num++;
							} else {
								num = 1;
								sensorGraphSeries1
										.resetData(new GraphViewData[] { new GraphViewData(
												0, 1) });
								sensorGraphSeries2
										.resetData(new GraphViewData[] { new GraphViewData(
												0, 1) });
								processedGraphSeries
										.resetData(new GraphViewData[] { new GraphViewData(
												0, 1) });
							}

							// mGraph.setDataWithAdjustment(dataArray,
							// "Shimmer : " + deviceName, units);
							// mTVPRR.setText("PRR : "
							// + String.format(
							// "%.2f",
							// mService.getPacketReceptionRate(mBluetoothAddress))
							// + "%");
							if (calibratedDataArray.length > 0) {
								// mValueSensor1.setText(String.format("%.4f",
								// calibratedDataArray[0]));
								mTextSensor1.setText(sensorName[0] + "("
										+ calibratedUnits + ")");
							}
							if (calibratedDataArray.length > 1) {
								// mValueSensor2.setText(String.format("%.4f",
								// calibratedDataArray[1]));
								mTextSensor2.setText(sensorName[1] + "("
										+ calibratedUnits2 + ")");
							}
							if (calibratedDataArray.length > 2) {
								// mValueSensor3.setText(String.format("%.4f",
								// calibratedDataArray[2]));
								mTextSensor3.setText(sensorName[2] + "("
										+ calibratedUnits + ")");
							}

							mGraphSubSamplingCount = 0;
						}
					}
				}

				break;
			case Shimmer.MESSAGE_ACK_RECEIVED:

				break;
			case Shimmer.MESSAGE_DEVICE_NAME:
				// save the connected device's name
				Toast.makeText(context, "Connected to " + mBluetoothAddress,
						Toast.LENGTH_SHORT).show();
				break;
			case Shimmer.MESSAGE_TOAST:
				Toast.makeText(context, msg.getData().getString(Shimmer.TOAST),
						Toast.LENGTH_SHORT).show();
				break;
			}
		};
	};

	/**
	 * @param num
	 *            number of points of a sinusoid to be plotted at startup (used
	 *            in initial tests of plot, so keep at 1 when you do not want
	 *            the sinusoid to be visble at startup).
	 * @param viewPortSize
	 *            Size of visible data on the plot (in number of samples)
	 */
	private void setupLineGraph(int num, int viewPortSize) {
		// draw random curve
		GraphViewData[] data = new GraphViewData[num];
		double v = 0;
		for (int i = 0; i < num; i++) {
			v += 0.2;
			data[i] = new GraphViewData(i, 2000 * Math.sin(v));
		}

		// graph with dynamically genereated horizontal and vertical label
		// add data
		sensorGraphSeries1 = new GraphViewSeries(data);
		sensorGraphSeries1.getStyle().color = Color.RED;
		GraphViewData[] data2 = new GraphViewData[1];
		data2[0] = new GraphViewData(num - 1, 0.5);
		sensorGraphSeries2 = new GraphViewSeries(data2);
		sensorGraphSeries2.getStyle().color = Color.GREEN;
		sensorGraphSeries3 = new GraphViewSeries(data2);
		sensorGraphSeries3.getStyle().color = Color.YELLOW;

		graphViewLine1.addSeries(sensorGraphSeries1);
		graphViewLine1.addSeries(sensorGraphSeries2);
		graphViewLine1.addSeries(sensorGraphSeries3);
		graphViewLine1.setViewPort(2, viewPortSize);
		graphViewLine1.getGraphViewStyle()
				.setHorizontalLabelsColor(Color.BLACK);
		graphViewLine1.getGraphViewStyle().setVerticalLabelsColor(Color.GRAY);
		graphViewLine1.getGraphViewStyle().setGridColor(Color.LTGRAY);
		graphViewLine1.getGraphViewStyle().setTextSize(
				graphViewLine1.getGraphViewStyle().getTextSize() / 2);
		// graphView.setTitle("Processed Data");
		graphViewLine1.setScalable(true);

		LinearLayout layout = (LinearLayout) findViewById(R.id.graph1);
		layout.addView(graphViewLine1);

		processedGraphSeries = new GraphViewSeries(data);
		graphViewLine2.addSeries(processedGraphSeries);
		graphViewLine2.setViewPort(2, viewPortSize);
		graphViewLine2.getGraphViewStyle()
				.setHorizontalLabelsColor(Color.BLACK);
		graphViewLine2.getGraphViewStyle().setVerticalLabelsColor(Color.GRAY);
		graphViewLine2.getGraphViewStyle().setGridColor(Color.LTGRAY);
		graphViewLine2.getGraphViewStyle().setTextSize(
				graphViewLine2.getGraphViewStyle().getTextSize() / 2);
		// graphView.setTitle("Processed Data");
		graphViewLine2.setScalable(true);

		layout = (LinearLayout) findViewById(R.id.graph2);
		layout.addView(graphViewLine2);

	}

	class ProcessTask extends AsyncTask<Void, Integer, Void> {

		ProgressDialog mProgress;
		File folder;
		FileWriter fw;
		FileReader fr;
		BufferedReader br;
		String filename_w;
		String filename_r;
		String val;
		Boolean proceedWithProcessing = true;
		double outValue;
		double inValue;
		String output;
		long fsize;
		int temp;
		GraphViewData[] graphViewDataBuffer1;
		GraphViewData[] graphViewDataBuffer2;
		GraphViewData[] graphViewDataBufferTemp;
		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
			Log.i("ProcessTask", "Starting ProcessTask");

			// Progress dialog
			mProgress = new ProgressDialog(context);
			// Saving directory (Permission to write needed in Manifest)
			folder = new File(Environment.getExternalStorageDirectory() + "/"
					+ outputFolder);

			// Make directory if it doesn't exist
			if (!folder.exists()) {
				folder.mkdir();
				Log.i("ProcessTask", "Making folder");
			}

			// Set filename to write
			filename_w = folder.toString() + "/output_" + mFileName;
			// Set filename to read
			filename_r = folder.toString() + "/" + mFileName;

			// Initialize the FileWriter with the name and directory
			try {
				fw = new FileWriter(filename_w, true);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.i("ProcessTask", "WriteFile not Created/Opened: "
						+ "mFileName");
				Toast.makeText(context, "Cannot create/open file to write to",
						Toast.LENGTH_SHORT).show();
				proceedWithProcessing = false;
				e.printStackTrace();
			}
			try {
				fr = new FileReader(filename_r);
				br = new BufferedReader(fr);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				Log.i("ProcessTask", "ReadFile not Created/Opened");
				Toast.makeText(context,
						"Cannot find specified file to process",
						Toast.LENGTH_SHORT).show();
				proceedWithProcessing = false;
				e.printStackTrace();
			}

		}

		@Override
		protected Void doInBackground(Void... params) {
			int i = 0;
			String[] lineValues = null;

			// Initialize QRS detector
			QRSDetector mOfflineQRSDetector = new QRSDetector();

			fsize = new File(filename_r).length();
			graphViewDataBuffer1=new GraphViewData[(int) (fsize/4)];
			graphViewDataBuffer2=new GraphViewData[(int) (fsize/4)];
			temp=0;
			// Check if files were detected before trying to read data
			if (proceedWithProcessing == true) {
				do {
					// Log.i("ProcessTask", "Processing");

					// Read value from .csv file
					try {
						val = br.readLine();
						lineValues = val.split(" ,");

					} catch (IOException e1) {
						Log.i("ProcessTask", "Not saving");
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

					// String header contains 3 rows. Make sure you are past it.
					if (temp > 3) {
						// Get QRS detection
						// Pass in ECG LA_LL calibrated value
						inValue = Double.parseDouble(lineValues[1]);
						outValue = mOfflineQRSDetector.isQRS(inValue);
						output = String.valueOf(outValue);
					} else {
						output = lineValues[1];
					}
					
					if (temp > 3) {
						graphViewDataBuffer1[temp]=new GraphViewData(temp,inValue);
						graphViewDataBuffer2[temp]=new GraphViewData(temp,outValue);
					}else{
						graphViewDataBuffer1[temp]=new GraphViewData(temp,0);
						graphViewDataBuffer2[temp]=new GraphViewData(temp,0);
					}
					
					// Write output from file
					try {
						fw.append(output + '\n');
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					publishProgress(i);
					i = i + val.length() + 1;
					temp++;

				} while (i < fsize);
				
				// Assign saved input data to an GraphViewData array that can be graphed
				graphViewDataBufferTemp=new GraphViewData[temp-1];
				for(i=0;i<temp-1;i++){
					graphViewDataBufferTemp[i]=graphViewDataBuffer1[i];
					Log.i("Check Conversion B1",String.valueOf(graphViewDataBuffer1[i].valueY));
				}
				graphViewDataBuffer1=new GraphViewData[temp-1];
				graphViewDataBuffer1=graphViewDataBufferTemp;
				
				// Assign saved output data to an GraphViewData array that can be graphed
				graphViewDataBufferTemp=new GraphViewData[temp-1];
				for(i=0;i<temp-1;i++){
					graphViewDataBufferTemp[i]=graphViewDataBuffer2[i];
					Log.i("Check Conversion B2",String.valueOf(graphViewDataBuffer2[i].valueY));
				}
				graphViewDataBuffer2=new GraphViewData[temp-1];
				graphViewDataBuffer2=graphViewDataBufferTemp;
			}
			return null;
		}

		// Changes to the UI must be done from this part of the AsyncTask
		@Override
		protected void onProgressUpdate(Integer... values) {
			// TODO Auto-generated method stub
			super.onProgressUpdate(values);


			// Update progress bar
			if (values[0] % (fsize / 40) < val.length()) {
				// Set progress percentage
				mProgress
						.setMessage("Processing: "
								+ Integer
										.toString((int) ((((double) values[0]) / fsize) * 100))
								+ " % Completed");
				mProgress.show();
				Log.d("ProcessTask", String.valueOf(values[0]) + " of "
						+ String.valueOf(fsize));
			}
		}

		@Override
		protected void onPostExecute(Void result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			
			// Reset graph series
			sensorGraphSeries1
					.resetData(graphViewDataBuffer1);
			//sensorGraphSeries2.removeGraphView(graphViewLine2);
			processedGraphSeries
					.resetData(graphViewDataBuffer2);
			graphViewLine1.setViewPort(temp-1024, 1024);
			graphViewLine2.setViewPort(temp-1024, 1024);
			mProgress.dismiss();
			try {
				fw.close();
				fr.close();
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NullPointerException e) {
				e.printStackTrace();

			}
			
			// Update Media Scanner
			File f = new File(filename_w);
			Uri contentUri = Uri.fromFile(f);
		    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,contentUri);
		    sendBroadcast(mediaScanIntent);
		    Toast.makeText(context, "Signal filter and saved in "+filename_w, Toast.LENGTH_LONG).show();
		}

	}

	public void showEnableSensors(final String[] sensorNames, int enabledSensors) {
		dialogEnabledSensors = enabledSensors;
		mDialog.setContentView(R.layout.dialog_enable_sensor_dialog);
		TextView title = (TextView) mDialog.findViewById(android.R.id.title);
		title.setText("Select Signal");
		final ListView listView = (ListView) mDialog
				.findViewById(android.R.id.list);
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		ArrayAdapter<String> adapterSensorNames = new ArrayAdapter<String>(
				this, android.R.layout.simple_list_item_multiple_choice,
				android.R.id.text1, sensorNames);
		listView.setAdapter(adapterSensorNames);
		final BiMap<String, String> sensorBitmaptoName;
		sensorBitmaptoName = Shimmer.generateBiMapSensorIDtoSensorName(
				mService.getShimmerVersion(mBluetoothAddress),
				mService.getAccelSmartMode(mBluetoothAddress));
		for (int i = 0; i < sensorNames.length; i++) {
			int iDBMValue = Integer.parseInt(sensorBitmaptoName.inverse().get(
					sensorNames[i]));
			if ((iDBMValue & enabledSensors) > 0) {
				listView.setItemChecked(i, true);
			}
		}

		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1,
					int clickIndex, long arg3) {
				int sensorIdentifier = Integer.parseInt(sensorBitmaptoName
						.inverse().get(sensorNames[clickIndex]));
				// check and remove any old daughter boards (sensors) which will
				// cause a conflict with sensorIdentifier
				dialogEnabledSensors = mService
						.sensorConflictCheckandCorrection(mBluetoothAddress,
								dialogEnabledSensors, sensorIdentifier);
				// update the checkbox accordingly
				for (int i = 0; i < sensorNames.length; i++) {
					int iDBMValue = Integer.parseInt(sensorBitmaptoName
							.inverse().get(sensorNames[i]));
					if ((iDBMValue & dialogEnabledSensors) > 0) {
						listView.setItemChecked(i, true);
					} else {
						listView.setItemChecked(i, false);
					}
				}
			}

		});

		Button mDoneButton = (Button) mDialog
				.findViewById(R.id.buttonEnableSensors);

		mDoneButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				mService.setEnabledSensors(dialogEnabledSensors,
						mBluetoothAddress);
				mDialog.dismiss();
			}
		});

		mDialog.show();

	}

	public void showSelectSensorPlot() {
		mDialog.setContentView(R.layout.dialog_sensor_view);
		TextView title = (TextView) mDialog.findViewById(android.R.id.title);
		title.setText("Select Signal");
		final ListView listView = (ListView) mDialog
				.findViewById(android.R.id.list);
		listView.setChoiceMode(ListView.CHOICE_MODE_NONE);
		List<String> sensorList = mService
				.getListofEnabledSensors(mBluetoothAddress);
		sensorList.add("Timestamp");
		final String[] sensorNames = sensorList.toArray(new String[sensorList
				.size()]);
		ArrayAdapter<String> adapterSensorNames = new ArrayAdapter<String>(
				this, android.R.layout.simple_list_item_multiple_choice,
				android.R.id.text1, sensorNames);
		listView.setAdapter(adapterSensorNames);

		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// TODO Auto-generated method stub
				mSensorView = sensorNames[arg2];
				// mTextSensor1.setText("");
				// mTextSensor2.setText("");
				// mTextSensor3.setText("");
				// if (mSensorView.equals("Accelerometer")) {
				// mTextSensor1.setText("AccelerometerX");
				// mTextSensor2.setText("AccelerometerY");
				// mTextSensor3.setText("AccelerometerZ");
				// }
				// if (mSensorView.equals("Gyroscope")) {
				// mTextSensor1.setText("GyroscopeX");
				// mTextSensor2.setText("GyroscopeY");
				// mTextSensor3.setText("GyroscopeZ");
				// }
				// if (mSensorView.equals("Magnetometer")) {
				// mTextSensor1.setText("MagnetometerX");
				// mTextSensor2.setText("MagnetometerY");
				// mTextSensor3.setText("MagnetometerZ");
				// }
				// if (mSensorView.equals("GSR")) {
				// mTextSensor1.setText("GSR");
				// }
				// if (mSensorView.equals("EMG")) {
				// mTextSensor1.setText("EMG");
				// }
				// if (mSensorView.equals("ECG")) {
				// mTextSensor1.setText("ECGRALL");
				// mTextSensor2.setText("ECGLALL");
				// }
				// if (mSensorView.equals("Strain Gauge")) {
				// mTextSensor1.setText("Strain Gauge High");
				// mTextSensor2.setText("Strain Gauge Low");
				// }
				// if (mSensorView.equals("Heart Rate")) {
				// mTextSensor1.setText("Heart Rate");
				// }
				// if (mSensorView.equals("ExpBoardA0")) {
				// mTextSensor1.setText("ExpBoardA0");
				// }
				// if (mSensorView.equals("ExpBoardA7")) {
				// mTextSensor1.setText("ExpBoardA7");
				// }
				// if (mSensorView.equals("Timestamp")) {
				// mTextSensor1.setText("TimeStamp");
				// }
				// if (mSensorView.equals("Battery Voltage")) {
				// mTextSensor1.setText("VSenseReg");
				// mTextSensor2.setText("VSenseBatt");
				// }
				// if (mSensorView.equals("External ADC A7")) {
				// mTextSensor1.setText("External ADC A7");
				// }
				// if (mSensorView.equals("External ADC A6")) {
				// mTextSensor1.setText("External ADC A6");
				// }
				// if (mSensorView.equals("External ADC A15")) {
				// mTextSensor1.setText("External ADC A15");
				// }
				// if (mSensorView.equals("Internal ADC A1")) {
				// mTextSensor1.setText("Internal ADC A1");
				// }
				// if (mSensorView.equals("Internal ADC A12")) {
				// mTextSensor1.setText("Internal ADC A12");
				// }
				// if (mSensorView.equals("Internal ADC A13")) {
				// mTextSensor1.setText("Internal ADC A13");
				// }
				// if (mSensorView.equals("Internal ADC A14")) {
				// mTextSensor1.setText("Internal ADC A14");
				// }
				// if (mSensorView.equals("Pressure")) {
				// mTextSensor1.setText("Pressure");
				// mTextSensor2.setText("Temperature");
				// }

				mDialog.dismiss();
			}

		});

		mDialog.show();

	}

}
