package com.george.biosense.activity;

import java.util.ArrayList;
import java.util.Arrays;

import com.shimmerresearch.driver.Configuration;
import com.shimmerresearch.driver.Shimmer;
import com.george.delta.deltatest.R;

import com.george.biosense.service.ShimmerService;
import com.george.biosense.service.ShimmerService.LocalBinder;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

public class CommandsActivity extends ServiceActivity {
	public static String mDone = "Done";
	ListView listViewAccelRange;
	ListView listViewMagRange;
	ListView listViewGyroRange;
    ListView listViewGsrRange;
	String mBluetoothAddress;
	CheckBox cBoxLowPowerMag;
	CheckBox cBoxLowPowerAccel;
	CheckBox cBoxLowPowerGyro;
	CheckBox cBox5VReg;
	TextView textViewCurrentGsrRange;
	TextView textViewCurrentGyroRange;
	TextView textViewCurrentMagRange;
	TextView textViewGyroRange;
	TextView textViewMagRange;
	TextView textViewGsr;
	TextView textViewCurrentPressureResolution;
	TextView textViewPressureResolution;
	ListView listViewPressureResolution;
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
    	setContentView(R.layout.shimmer_commands);
        
    	Bundle extras = getIntent().getExtras();
        mBluetoothAddress = extras.getString("BluetoothAddress");
    	double mSamplingRateV = extras.getDouble("SamplingRate");
    	int mAccelerometerRangeV = extras.getInt("AccelerometerRange");
    	int mGSRRangeV = extras.getInt("GSRRange");
    	final double batteryLimit = extras.getDouble("BatteryLimit");
    	String[] samplingRate = new String [] {"10","51.2","102.4","128","170.7","204.8","256","512"};
    	final ListView listViewSamplingRate = (ListView) findViewById(R.id.listView1);
    	listViewAccelRange = (ListView) findViewById(R.id.listView2);
    	listViewGyroRange = (ListView) findViewById(R.id.listViewGyroRange);
    	listViewMagRange = (ListView) findViewById(R.id.listViewMagRange);
        listViewGsrRange = (ListView) findViewById(R.id.listView3);
        listViewPressureResolution = (ListView) findViewById(R.id.listViewPressureAccuracy);
        textViewCurrentPressureResolution = (TextView) findViewById(R.id.textViewPressureAccuracyCurrentSetting);
        textViewPressureResolution = (TextView) findViewById(R.id.textPressureAccuracy);
        final EditText editTextBattLimit = (EditText) findViewById(R.id.editTextBattLimit);
        final TextView textViewCurrentSamplingRate = (TextView) findViewById(R.id.TextViewSamplingRateCurrentSetting);
        final TextView textViewCurrentAccelRange = (TextView) findViewById(R.id.textViewAccelRangeCurrentSetting);
        textViewCurrentGsrRange = (TextView) findViewById(R.id.textViewGSRCurrentSetting);
        textViewCurrentGyroRange = (TextView) findViewById(R.id.TextViewGyroRangeCurrentSetting);
        textViewCurrentMagRange = (TextView) findViewById(R.id.textViewMagRangeCurrentSetting);
        textViewGyroRange = (TextView)  findViewById(R.id.TextViewGyroRange);
        textViewMagRange = (TextView)  findViewById(R.id.textViewMagRange);
        textViewGsr = (TextView) findViewById(R.id.textViewGSR);
        textViewCurrentSamplingRate.setTextColor(Color.rgb(0, 135, 202));
        textViewCurrentAccelRange.setTextColor(Color.rgb(0, 135, 202));
        textViewCurrentGsrRange.setTextColor(Color.rgb(0, 135, 202));
        textViewCurrentGyroRange.setTextColor(Color.rgb(0, 135, 202));
        textViewCurrentMagRange.setTextColor(Color.rgb(0, 135, 202));
        textViewCurrentPressureResolution.setTextColor(Color.rgb(0, 135, 202));
        
        textViewCurrentSamplingRate.setText(Double.toString(mSamplingRateV));
        
        editTextBattLimit.setText(Double.toString(batteryLimit));
        
        if (mAccelerometerRangeV==0){
        	if (mService.getShimmerVersion(mBluetoothAddress)!=Shimmer.SHIMMER_3){
        		textViewCurrentAccelRange.setText("+/- 1.5g");
        	} else {
        		textViewCurrentAccelRange.setText("+/- 2g");
        	}
        } else if (mAccelerometerRangeV==1){
        	textViewCurrentAccelRange.setText("+/- 4g");
        } else if (mAccelerometerRangeV==2){
        	textViewCurrentAccelRange.setText("+/- 8g");
        }
        else if (mAccelerometerRangeV==3){
        	if (mService.getShimmerVersion(mBluetoothAddress)!=Shimmer.SHIMMER_3){
        		textViewCurrentAccelRange.setText("+/- 6g");
        	} else {
        		textViewCurrentAccelRange.setText("+/- 16g");
        	}
        } 
        
        if (mGSRRangeV==0) {
        	textViewCurrentGsrRange.setText("10kOhm to 56kOhm");
        } else if (mGSRRangeV==1) {
        	textViewCurrentGsrRange.setText("56kOhm to 220kOhm");
        } else if (mGSRRangeV==2) {
        	textViewCurrentGsrRange.setText("220kOhm to 680kOhm");
        } else if (mGSRRangeV==3) {
        	textViewCurrentGsrRange.setText("680kOhm to 4.7MOhm"); 
        } else if (mGSRRangeV==4) {
        	textViewCurrentGsrRange.setText("Auto Range");
        }
          
    	ArrayList<String> samplingRateList = new ArrayList<String>();  
    	samplingRateList.addAll( Arrays.asList(samplingRate) );  
        ArrayAdapter<String> sR = new ArrayAdapter<String>(CommandsActivity.this, R.layout.commands_name,samplingRateList);
    	listViewSamplingRate.setAdapter(sR);
    	Button buttonToggleLED = (Button) findViewById(R.id.button1);
    	
    	buttonToggleLED.setOnClickListener(new OnClickListener(){

			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				Intent intent = new Intent();
	            intent.putExtra("ToggleLED", true);
	            // Set result and finish this Activity
	            setResult(Activity.RESULT_OK, intent);
	            finish();
			}
        	
        });
    	
    	
    	Button buttonBattVoltLimit = (Button) findViewById(R.id.buttonBattLimit);
    	
    	buttonBattVoltLimit.setOnClickListener(new OnClickListener(){

			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				// TODO Auto-generated method stub
				Intent intent = new Intent();
	            intent.putExtra("BatteryLimit", Double.parseDouble(editTextBattLimit.getText().toString()));
	            // Set result and finish this Activity
	            setResult(Activity.RESULT_OK, intent);
	            finish();
			}
    		
    	});
    	
    	buttonToggleLED.setOnClickListener(new OnClickListener(){

			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				Intent intent = new Intent();
	            intent.putExtra("ToggleLED", true);
	            // Set result and finish this Activity
	            setResult(Activity.RESULT_OK, intent);
	            finish();
			}
        	
        });
    	
    	listViewSamplingRate.setOnItemClickListener(new AdapterView.OnItemClickListener() {

    		  public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {

    		    Object o = listViewSamplingRate.getItemAtPosition(position);
    		    Log.d("Shimmer",o.toString());
    		    Intent intent = new Intent();
	            intent.putExtra("SamplingRate",Double.valueOf(o.toString()).doubleValue());
	            // Set result and finish this Activity
	            setResult(Activity.RESULT_OK, intent);
	            finish();
    		    
    		  }
    		});
    	
    	listViewAccelRange.setOnItemClickListener(new AdapterView.OnItemClickListener() {

  		  public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {

  		    Object o = listViewAccelRange.getItemAtPosition(position);
  		    Log.d("Shimmer",o.toString());
  		    int accelRange=0;
  		    if (mService.getShimmerVersion(mBluetoothAddress)!=Shimmer.SHIMMER_3){
	  		    if (o.toString()=="+/- 1.5g"){
	  		    	accelRange=0;
	  		    } else if (o.toString()=="+/- 6g"){
	  		    	accelRange=3;
	  		    }
  		    } else {
  		    	if (o.toString()=="+/- 2g"){
	  		    	accelRange=0;
	  		    } else if (o.toString()=="+/- 4g"){
	  		    	accelRange=1;
	  		    } else if (o.toString()=="+/- 8g"){
	  		    	accelRange=2;
	  		    } else if (o.toString()=="+/- 16g"){
	  		    	accelRange=3;
	  		    }
  		    }
  		    Intent intent = new Intent();
	            intent.putExtra("AccelRange",accelRange);
	            // Set result and finish this Activity
	            setResult(Activity.RESULT_OK, intent);
	            finish();
  		    
  		  }
  		});
    	
    	listViewGyroRange.setOnItemClickListener(new AdapterView.OnItemClickListener() {
    		  public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
    		    Object o = listViewGyroRange.getItemAtPosition(position);
    		    Log.d("Shimmer",o.toString());
    		    int gyroRange=0;
    		  
    		    if (o.toString()=="250dps"){
  	  		    	gyroRange=0;
  	  		    } else if (o.toString()=="500dps"){
  	  		    	gyroRange=1;
  	  		    } else if (o.toString()=="1000dps"){
  	  		    	gyroRange=2;
  	  		    } else if (o.toString()=="2000dps"){
  	  		    	gyroRange=3;
  	  		    }
    		    Intent intent = new Intent();
  	            intent.putExtra("GyroRange",gyroRange);
  	            // Set result and finish this Activity
  	            setResult(Activity.RESULT_OK, intent);
  	            finish();
    		  }
		});
      	
    	listViewMagRange.setOnItemClickListener(new AdapterView.OnItemClickListener() {
  		  public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
  		    Object o = listViewMagRange.getItemAtPosition(position);
  		    Log.d("Shimmer",o.toString());
  		    int magRange=0;
  		  
  		    if (mService.getShimmerVersion(mBluetoothAddress)==Shimmer.SHIMMER_3){
  		    	if (o.toString()=="+/- 1.3Ga"){
  		    		magRange=1;
  		    	} else if (o.toString()=="+/- 1.9Ga"){
  		    		magRange=2;
  		    	} else if (o.toString()=="+/- 2.5Ga"){
  		    		magRange=3;
  		    	} else if (o.toString()=="+/- 4.0Ga"){
  		    		magRange=4;
  		    	} else if (o.toString()=="+/- 4.7Ga"){
  		    		magRange=5;
  		    	} else if (o.toString()=="+/- 5.6Ga"){
  		    		magRange=6;
  		    	} else if (o.toString()=="+/- 8.1Ga"){
  		    		magRange=7;
  		    	}
  		    } else {
  		    	if (o.toString()=="+/- 0.8Ga"){
  		    		magRange=0;
  		    	} else if (o.toString()=="+/- 1.3Ga"){
  		    		magRange=1;
  		    	} else if (o.toString()=="+/- 1.9Ga"){
  		    		magRange=2;
  		    	} else if (o.toString()=="+/- 2.5Ga"){
  		    		magRange=3;
  		    	} else if (o.toString()=="+/- 4.0Ga"){
  		    		magRange=4;
  		    	} else if (o.toString()=="+/- 4.7Ga"){
  		    		magRange=5;
  		    	} else if (o.toString()=="+/- 5.6Ga"){
  		    		magRange=6;
  		    	} else if (o.toString()=="+/- 8.1Ga"){
  		    		magRange=7;
  		    	}
  		    }
  		    Intent intent = new Intent();
	            intent.putExtra("MagRange",magRange);
	            // Set result and finish this Activity
	            setResult(Activity.RESULT_OK, intent);
	            finish();
  		  }
		});
    	
    	listViewPressureResolution.setOnItemClickListener(new AdapterView.OnItemClickListener() {
    		  public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
    		    Object o = listViewPressureResolution.getItemAtPosition(position);
    		    Log.d("Shimmer",o.toString());
    		    int pressureRes=0;
    		  
    		    if (o.toString()=="Low"){
  	  		    	pressureRes=0;
  	  		    } else if (o.toString()=="Standard"){
  	  		    	pressureRes=1;
  	  		    } else if (o.toString()=="High"){
  	  		    	pressureRes=2;
  	  		    } else if (o.toString()=="Ultra High"){
  	  		    	pressureRes=3;
  	  		    } 
    		    Intent intent = new Intent();
  	            intent.putExtra("PressureResolution",pressureRes);
  	            // Set result and finish this Activity
  	            setResult(Activity.RESULT_OK, intent);
  	            finish();
    		  }
  		});
    	
    	listViewGsrRange.setOnItemClickListener(new AdapterView.OnItemClickListener() {

    		  public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
    			
    		    Object o = listViewGsrRange.getItemAtPosition(position);
    		    Log.d("Shimmer",o.toString());
    		    int gsrRange=0;
    		    if (o.toString()=="10kOhm to 56kOhm"){
    		    	gsrRange=0;
    		    } else if (o.toString()=="56kOhm to 220kOhm"){
    		    	gsrRange=1;
    		    } else if (o.toString()=="220kOhm to 680kOhm"){
    		    	gsrRange=2;
    		    } else if (o.toString()=="680kOhm to 4.7MOhm"){
    		    	gsrRange=3;
    		    } else if (o.toString()=="Auto Range"){
    		    	gsrRange=4;
    		    }
    		    Intent intent = new Intent();
  	            intent.putExtra("GSRRange",gsrRange);
  	            // Set result and finish this Activity
  	            setResult(Activity.RESULT_OK, intent);
  	            finish();
    		    
    		  }
    		});
	}
    protected ServiceConnection mTestServiceConnection = new ServiceConnection() {

      	public void onServiceConnected(ComponentName arg0, IBinder service) {
      		// TODO Auto-generated method stub
      		Log.d("ShimmerService", "srvice connected");
      		LocalBinder binder = (ShimmerService.LocalBinder) service;
      		mService = binder.getService();
      		cBox5VReg = (CheckBox) findViewById(R.id.checkBox5VReg);
    		cBoxLowPowerMag = (CheckBox) findViewById(R.id.checkBoxLowPowerMag);
    		cBoxLowPowerAccel = (CheckBox) findViewById(R.id.checkBoxLowPowerAccel);
    		cBoxLowPowerGyro = (CheckBox) findViewById(R.id.checkBoxLowPowerGyro);
    		Shimmer stemp = mService.getShimmer(mBluetoothAddress);
    		String[] accelRange = new String [] {"+/- 1.5g","+/- 6g"};
        	String[] gsrRange = new String [] {"10kOhm to 56kOhm","56kOhm to 220kOhm","220kOhm to 680kOhm","680kOhm to 4.7MOhm","Auto Range"};
      		final Shimmer shimmer = mService.getShimmer(mBluetoothAddress);
        	if (shimmer.isLowPowerMagEnabled()){
        		cBoxLowPowerMag.setChecked(true);
        	}
        	
        	if (shimmer.isLowPowerAccelEnabled()){
        		cBoxLowPowerAccel.setChecked(true);
        	}
        	
        	if (shimmer.isLowPowerGyroEnabled()){
        		cBoxLowPowerGyro.setChecked(true);
        	}
        	
        	
        	if (mService.getShimmerVersion(mBluetoothAddress)==Shimmer.SHIMMER_3){
        		accelRange = Configuration.Shimmer3.ListofAccelRange;
        		gsrRange = new String[]{""};
        		ArrayList<String> accelRangeList = new ArrayList<String>();  
            	accelRangeList.addAll( Arrays.asList(accelRange) );  
                ArrayAdapter<String> sR2 = new ArrayAdapter<String>(CommandsActivity.this, R.layout.commands_name,accelRangeList);
            	listViewAccelRange.setAdapter(sR2);
            	textViewCurrentGsrRange.setVisibility(View.INVISIBLE);
            	textViewGsr.setVisibility(View.INVISIBLE);
            	listViewGsrRange.setVisibility(View.INVISIBLE);
            	cBox5VReg.setVisibility(View.INVISIBLE);
            	textViewCurrentGyroRange.setText(Configuration.Shimmer3.ListofGyroRange[stemp.getAccelRange()]);
        		textViewCurrentMagRange.setText(Configuration.Shimmer3.ListofMagRange[stemp.getMagRange()-1]);
        		//textViewPressureResolution.setText(Configuration.Shimmer3.ListofPressureResolution[stemp.getPressureResolution()]);
            	//Gyro
            	String[] range = Configuration.Shimmer3.ListofGyroRange;
            	ArrayAdapter<String> sR3 = new ArrayAdapter<String>(CommandsActivity.this, R.layout.commands_name,range);
            	listViewGyroRange.setAdapter(sR3);
            	//Mag
            	range = Configuration.Shimmer3.ListofMagRange;
            	ArrayAdapter<String> sR4 = new ArrayAdapter<String>(CommandsActivity.this, R.layout.commands_name,range);
            	listViewMagRange.setAdapter(sR4);
            	
            	//range = Configuration.Shimmer3.ListofPressureResolution;
            	ArrayAdapter<String> sR5 = new ArrayAdapter<String>(CommandsActivity.this, R.layout.commands_name,range);
            	listViewPressureResolution.setAdapter(sR5);
            	
            	if (shimmer.getAccelRange()==0){
            		cBoxLowPowerAccel.setVisibility(View.INVISIBLE);	
            	}
            	
            	//currently not supported for the moment 
            	textViewCurrentPressureResolution.setVisibility(View.VISIBLE);
        		listViewPressureResolution.setVisibility(View.VISIBLE);
        		textViewPressureResolution.setVisibility(View.VISIBLE);
        	} else {
        		textViewCurrentPressureResolution.setVisibility(View.INVISIBLE);
        		textViewPressureResolution.setVisibility(View.INVISIBLE);
        		listViewPressureResolution.setVisibility(View.INVISIBLE);
        		textViewGyroRange.setVisibility(View.INVISIBLE);
        		textViewCurrentGyroRange.setVisibility(View.INVISIBLE);
        		cBoxLowPowerAccel.setVisibility(View.INVISIBLE);
        		cBoxLowPowerGyro.setVisibility(View.INVISIBLE);
        		listViewGyroRange.setVisibility(View.INVISIBLE);
	      	  	ArrayList<String> accelRangeList = new ArrayList<String>();  
	        	accelRangeList.addAll( Arrays.asList(accelRange) );  
	            ArrayAdapter<String> sR2 = new ArrayAdapter<String>(CommandsActivity.this, R.layout.commands_name,accelRangeList);
	        	listViewAccelRange.setAdapter(sR2);
	        	ArrayList<String> gsrRangeList = new ArrayList<String>();  
	        	gsrRangeList.addAll( Arrays.asList(gsrRange) );  
	            ArrayAdapter<String> sR3 = new ArrayAdapter<String>(CommandsActivity.this, R.layout.commands_name,gsrRangeList);
	        	listViewGsrRange.setAdapter(sR3);
	        	
	          	//Mag
	        	String[] range = Configuration.Shimmer2.ListofMagRange;
            	ArrayAdapter<String> sR4 = new ArrayAdapter<String>(CommandsActivity.this, R.layout.commands_name,range);
            	textViewCurrentMagRange.setText(Configuration.Shimmer2.ListofMagRange[stemp.getMagRange()]);
            	listViewMagRange.setAdapter(sR4);
        	}
      		
      		mServiceBind = true;
      		//update the view
      		
      		if (mService.get5VReg(mBluetoothAddress)==1){
      			cBox5VReg.setChecked(true);
      		}
      		
      		cBox5VReg.setOnCheckedChangeListener(new OnCheckedChangeListener(){

    			public void onCheckedChanged(CompoundButton arg0, boolean checked) {
    				// TODO Auto-generated method stub
    				if (checked){
    					mService.write5VReg(mBluetoothAddress, 1);
    				} else {
    					mService.write5VReg(mBluetoothAddress, 0);
    				}
    				finish();
    			}
        		
        	});
        	
      		
      		cBoxLowPowerAccel.setOnCheckedChangeListener(new OnCheckedChangeListener(){

    			public void onCheckedChanged(CompoundButton arg0, boolean checked) {
    				// TODO Auto-generated method stub
    				if (checked){
    					shimmer.enableLowPowerAccel(true);
    				} else {
    					shimmer.enableLowPowerAccel(false);
    				}
    				finish();
    			}
        		
        	});
      		
      		cBoxLowPowerGyro.setOnCheckedChangeListener(new OnCheckedChangeListener(){

    			public void onCheckedChanged(CompoundButton arg0, boolean checked) {
    				// TODO Auto-generated method stub
    				if (checked){
    					shimmer.enableLowPowerGyro(true);
    				} else {
    					shimmer.enableLowPowerGyro(false);
    				}
    				finish();
    			}
        		
        	});
      		
  
      		cBoxLowPowerMag.setChecked(mService.isLowPowerMagEnabled(mBluetoothAddress));
      		
      		cBoxLowPowerMag.setOnCheckedChangeListener(new OnCheckedChangeListener(){

    			public void onCheckedChanged(CompoundButton arg0, boolean checked) {
    				// TODO Auto-generated method stub
    				if (checked){
    					mService.enableLowPowerMag(mBluetoothAddress, true);
    				} else {
    					mService.enableLowPowerMag(mBluetoothAddress, false);
    				}
    				finish();
    			}
        		
        	});
      		
      		}

      	public void onServiceDisconnected(ComponentName arg0) {
      		// TODO Auto-generated method stub
      		mServiceBind = false;
      	}
    };
	
	
	public void onPause() {
		super.onPause();
		Log.d("Shimmer","On Pause");
		//finish();
		if(mServiceBind == true){
  		  getApplicationContext().unbindService(mTestServiceConnection);
  	  }
	}

	public void onResume() {
		super.onResume();
		Log.d("Shimmer","On Resume");
		Intent intent=new Intent(this, ShimmerService.class);
  	  	Log.d("ShimmerH","on Resume");
  	  	getApplicationContext().bindService(intent,mTestServiceConnection, Context.BIND_AUTO_CREATE);
	}
	
}
