package com.example.graphtest1;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.sunb.lib.SunGraph.GraphView;
import com.sunb.lib.SunGraph.LineInfo;
import com.sunb.lib.SunGraph.XYAxisInfo;


public class MainActivity extends ActionBarActivity {

	private GraphView mWidget1 = null;
	private LineInfo mLine1 = null;
	private final float[] humidityValues = new float[20];
	
	static final int REQUEST_ENABLE_BT = 10;
	int mPairedDeviceCount = 0;
	Set<BluetoothDevice> mDevices;
	BluetoothAdapter mBluetoothAdapter;
	BluetoothDevice mRemoteDevice;
	BluetoothSocket mSocket = null;
	OutputStream mOutputStream = null;
	InputStream mInputStream = null;
	String mStrDelimiter = "\n";
	char mCharDelimiter = '\n';
	Thread mWorkerThread = null;
	byte[] readBuffer;
	int readBufferPosition;
	
	private int ipos = 0;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
		mWidget1 = (GraphView) findViewById(R.id.graph_for_PH);
		XYAxisInfo axis1 = new XYAxisInfo("�ð�", "A0��", 0, 1024, 20, new Point(3, 4), Color.WHITE);
		mLine1 = new LineInfo("A0 ��", Color.RED, 5, humidityValues);
		mWidget1.CreateXYAxis(axis1);
		
		checkBluetooth();
    }
    
	@Override
	protected void onDestroy() {
		try {
			mWorkerThread.interrupt();
			mInputStream.close();
			mOutputStream.close();
			mSocket.close();
		} catch (Exception e) {
		}
		super.onDestroy();
	}
	
	BluetoothDevice getDeviceFromBondedList(String name) {
		BluetoothDevice selectedDevice = null;
		for (BluetoothDevice device : mDevices) {
			if (name.equals(device.getName())) {
				selectedDevice = device;
				break;
			}
		}
		return selectedDevice;
	}

	void sendData(String msg) {
		msg += mStrDelimiter;
		try {
			mOutputStream.write(msg.getBytes());
		} catch (Exception e) {
			Toast.makeText(getApplicationContext(), "������ ���� �� ������ �߻��߽��ϴ�.",
					Toast.LENGTH_LONG).show();
			finish();
		}
	}

	void connectToSelectedDevice(String selectedDeviceName) {
		mRemoteDevice = getDeviceFromBondedList(selectedDeviceName);
		UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

		try {
			mSocket = mRemoteDevice.createRfcommSocketToServiceRecord(uuid);
			mSocket.connect();
			mOutputStream = mSocket.getOutputStream();
			mInputStream = mSocket.getInputStream();
			beginListenForData();
		} catch (Exception e) {
			Toast.makeText(getApplicationContext(), "������� ���� �� ������ �߻��߽��ϴ�.",
					Toast.LENGTH_LONG).show();
			finish();
		}
	}

	void beginListenForData() {
		final Handler handler = new Handler();
		readBufferPosition = 0;
		readBuffer = new byte[1024];
		mWorkerThread = new Thread(new Runnable() {
			public void run() {

				while (!Thread.currentThread().isInterrupted()) {

					try {
						int bytesAvailable = mInputStream.available();
						
						Log.d("test111", "avail = " + bytesAvailable);
						if (bytesAvailable > 0) {
							byte[] packetBytes = new byte[bytesAvailable];
							mInputStream.read(packetBytes);
							for (int i = 0; i < bytesAvailable; i++) {
								byte b = packetBytes[i];
								if (b == mCharDelimiter) {
									byte[] encodedBytes = new byte[readBufferPosition];
									System.arraycopy(readBuffer, 0,
											encodedBytes, 0,
											encodedBytes.length);
									final String data = new String(
											encodedBytes, "US-ASCII");
									readBufferPosition = 0;

									Log.d("test111", "t222");

									handler.post(new Runnable() {
										public void run() {
											Log.d("test111", "test");
											Log.d("test111", data);
											
											if (humidityValues.length > ipos) {
												humidityValues[ipos] = Float.parseFloat(data);
												ipos++;
											} else {
												for (int k = 0; k < 19; k++) {
													humidityValues[k] = humidityValues[k + 1];
												}
												humidityValues[19] = Float.parseFloat(data);
											}
											
											mLine1.setLineInnerData(humidityValues);
											mWidget1.UpdateLine(mLine1);
											mWidget1.UpdateAll();
											mWidget1.postInvalidate();

										}
									});
								} else {
									readBuffer[readBufferPosition++] = b;
								}
							}
						}
					} catch (IOException ex) {
						Toast.makeText(getApplicationContext(),
								"������ ���� �� ������ �߻��߽��ϴ�.", Toast.LENGTH_LONG)
								.show();
						finish();
					}
				}
			}
		});
		mWorkerThread.start();
	}

	

	void selectDevice() {
		mDevices = mBluetoothAdapter.getBondedDevices();
		mPairedDeviceCount = mDevices.size();
		if (mPairedDeviceCount == 0) {
			Toast.makeText(getApplicationContext(), "���� ��ġ�� �����ϴ�.",
					Toast.LENGTH_LONG).show();
			finish();
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("������� ��ġ ����");
		List<String> listItems = new ArrayList<String>();
		for (BluetoothDevice device : mDevices) {
			listItems.add(device.getName());
		}
		listItems.add("���");
		final CharSequence[] items = listItems
				.toArray(new CharSequence[listItems.size()]);
		builder.setItems(items, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				if (item == mPairedDeviceCount) {
					Toast.makeText(getApplicationContext(),
							"������ ��ġ�� �������� �ʾҽ��ϴ�.", Toast.LENGTH_LONG).show();
					finish();
				} else {
					connectToSelectedDevice(items[item].toString());
				}
			}
		});
		builder.setCancelable(false);
		AlertDialog alert = builder.create();
		alert.show();
	}


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
	void checkBluetooth() {
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
			Toast.makeText(getApplicationContext(), "��Ⱑ ��������� �������� �ʽ��ϴ�.",
					Toast.LENGTH_LONG).show();
			finish();
		} else {
			if (!mBluetoothAdapter.isEnabled()) {
				Toast.makeText(getApplicationContext(), "���� ��������� ��Ȱ�� �����Դϴ�.",
						Toast.LENGTH_LONG).show();
				Intent enableBtIntent = new Intent(
						BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			} else
				selectDevice();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_ENABLE_BT:
			if (resultCode == RESULT_OK) {
				selectDevice();
			} else if (resultCode == RESULT_CANCELED) {
				Toast.makeText(getApplicationContext(),
						"��������� ����� �� ���� ���α׷��� �����մϴ�.", Toast.LENGTH_LONG)
						.show();
				finish();
			}
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	

}
