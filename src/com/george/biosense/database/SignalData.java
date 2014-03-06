package com.george.biosense.database;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SignalData {
	
	Context mContext;
	SQLiteOpenHelper dbOpenHelper;
	SQLiteDatabase db;
	
	class SignalDataOpenHelper extends SQLiteOpenHelper {

		private static final int DATABASE_VERSION = 2;
		private static final String DATABASE_NAME = "Signal Data";
	    private static final String DATA_TABLE_NAME = "signal";
	    private static final String KEY_ID = "id";
	    private static final String SIG_VALUE = "value";
	    private static final String DATA_TABLE_CREATE =
	                "CREATE TABLE " + DATA_TABLE_NAME + " (" +
	                KEY_ID + " INTEGER PRIMARY KEY, " +
	                SIG_VALUE + " REAL);";

	    public SignalDataOpenHelper(Context context) {
	        super(context, DATABASE_NAME, null, DATABASE_VERSION);
	    }

	    @Override
	    public void onCreate(SQLiteDatabase db) {
	        db.execSQL(DATA_TABLE_CREATE);
	    }
	    
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	public SignalData(Context context){
		mContext=context;
	}
	
	public void open() throws SQLException{
		dbOpenHelper = new SignalDataOpenHelper(mContext);
		db = dbOpenHelper.getWritableDatabase();
	}
	
	public void close() throws SQLException{
		dbOpenHelper.close();	
	}
}
