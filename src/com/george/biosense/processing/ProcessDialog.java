package com.george.biosense.processing;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.george.delta.deltatest.R;

public class ProcessDialog extends Activity{
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
	
		setContentView(R.layout.processing_dialog);
		final EditText editFilename = (EditText) findViewById(R.id.editTextFilename); 
		final Button confirm = (Button) findViewById(R.id.buttonFile);
		final Context mContext=this;
		
		editFilename.setText(".csv");
		
		confirm.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(editFilename.getText()!=null){
					Intent intent = new Intent();
					// Set result and finish this Activity
					intent.putExtra("processFileName",editFilename.getText().toString());
					setResult(Activity.RESULT_OK, intent);
					finish();
				}else{
					Toast.makeText(mContext, "You must name the file you want to process", Toast.LENGTH_SHORT).show();
				}
			}
		});
		
	}
	


}
