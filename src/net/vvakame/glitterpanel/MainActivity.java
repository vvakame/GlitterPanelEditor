package net.vvakame.glitterpanel;

import net.vvakame.glitterpanel.GlitterPanelFragment.Brightness;
import net.vvakame.glitterpanel.GlitterPanelFragment.GlitterPanelEventCallback;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Toast;

public class MainActivity extends Activity implements ITag, OnClickListener,
		OnItemSelectedListener, GlitterPanelEventCallback {

	GlitterPanelView mGlitterView;
	CheckBox mAutoSync;
	Spinner mBrightnessSpinner;

	PollingHandler mPoller = new PollingHandler();

	GlitterPanelFragment mGlitter;

	Brightness mCurrentBrightness = Brightness.PER_25;
	boolean mRandomBrightness = false;

	UsbManager mUsbManager;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		{
			mGlitter = new GlitterPanelFragment();
			FragmentManager manager = getFragmentManager();
			FragmentTransaction transaction = manager.beginTransaction();
			transaction.add(mGlitter, "GlitterPanel");
			transaction.commit();
		}

		findViewById(R.id.clear).setOnClickListener(this);
		findViewById(R.id.send).setOnClickListener(this);
		mGlitterView = (GlitterPanelView) findViewById(R.id.panel);
		mAutoSync = (CheckBox) findViewById(R.id.autosync);
		mAutoSync.setOnClickListener(this);
		mBrightnessSpinner = (Spinner) findViewById(R.id.brightness);
		{
			ArrayAdapter<CharSequence> adapter = ArrayAdapter
					.createFromResource(this, R.array.brightness,
							android.R.layout.simple_spinner_item);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			mBrightnessSpinner.setAdapter(adapter);
			mBrightnessSpinner.setOnItemSelectedListener(this);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		mPoller.setEnabled();
		mPoller.start();
	}

	@Override
	protected void onPause() {
		super.onPause();

		mPoller.setDisabled();
	}

	void sendDisplayData() {
		boolean[] datas = mGlitterView.getDisplayPattern();

		if (mRandomBrightness) {
			switch (mCurrentBrightness) {
			case PER_25:
				mCurrentBrightness = Brightness.PER_50;
				break;
			case PER_50:
				mCurrentBrightness = Brightness.PER_75;
				break;
			case PER_75:
				mCurrentBrightness = Brightness.PER_100;
				break;
			case PER_100:
				mCurrentBrightness = Brightness.PER_25;
				break;
			}
		}

		mGlitter.display(mCurrentBrightness, datas);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.clear: {
			if (!mGlitter.isConnected()) {
				mGlitter.connect();
			}

			mGlitterView.clearDisplayPattern();
			if (mAutoSync.isChecked()) {
				sendDisplayData();
			}
		}

			break;

		case R.id.send: {
			if (!mGlitter.isConnected()) {
				mGlitter.connect();
			}

			sendDisplayData();
		}
			break;

		case R.id.autosync: {
			if (mAutoSync.isChecked() && !mGlitter.isConnected()) {
				mGlitter.connect();
			}
			sendDisplayData();
		}
			break;

		default:
			if (mAutoSync.isChecked()) {
				sendDisplayData();
			}

			break;
		}
	}

	@Override
	public void onConnected() {
		Toast.makeText(this, R.string.glitter_panel_connected,
				Toast.LENGTH_SHORT).show();

		sendDisplayData();
	}

	@Override
	public void onDisconnected() {
		Toast.makeText(this, R.string.glitter_panel_disconnected,
				Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onRequestPermission() {
		Toast.makeText(this, R.string.requested_permission, Toast.LENGTH_SHORT)
				.show();
	}

	@Override
	public void onPermissionGrant() {
		Toast.makeText(this, R.string.grant_permission_request,
				Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onPermissionReject() {
		Toast.makeText(this, R.string.reject_permission_request,
				Toast.LENGTH_SHORT).show();
	}

	class PollingHandler extends Handler {

		boolean mEnabled = true;

		@Override
		public void handleMessage(Message msg) {
			if (mAutoSync.isChecked()) {
				sendDisplayData();
			}

			if (mEnabled) {
				sendEmptyMessageDelayed(0, 50);
			}
		}

		public void start() {
			sendEmptyMessage(0);
		}

		public void setEnabled() {
			mEnabled = true;
		}

		public void setDisabled() {
			mEnabled = false;
		}
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position,
			long id) {

		switch (position) {
		case 0:
			mCurrentBrightness = Brightness.PER_25;
			mRandomBrightness = false;
			break;

		case 1:
			mCurrentBrightness = Brightness.PER_50;
			mRandomBrightness = false;
			break;

		case 2:
			mCurrentBrightness = Brightness.PER_75;
			mRandomBrightness = false;
			break;

		case 3:
			mCurrentBrightness = Brightness.PER_100;
			mRandomBrightness = false;
			break;

		case 4:
			mCurrentBrightness = Brightness.PER_25;
			mRandomBrightness = true;
			break;

		default:
			break;
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
	}
}