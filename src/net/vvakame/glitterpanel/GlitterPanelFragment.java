package net.vvakame.glitterpanel;

import java.util.HashMap;

import android.app.Activity;
import android.app.Fragment;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

public class GlitterPanelFragment extends Fragment implements ITag {

	public static interface GlitterPanelEventCallback {
		public void onConnected();

		public void onDisconnected();

		public void onRequestPermission();

		public void onPermissionGrant();

		public void onPermissionReject();
	}

	public static final int VENDOR_TAKARATOMY = 3888;
	public static final int PRODUCT_GLITTER_PANEL = 64;

	public static final int PANEL_HEIGHT = 16;
	public static final int PANEL_WIDTH = 16;
	public static final int PIXELS = PANEL_HEIGHT * PANEL_WIDTH;

	final GlitterPanelFragment self = this;

	GlitterPanelEventCallback mCallback;

	PendingIntent mPermissionIntent;
	PermissionGrantReceiver mGrantReceiver;
	UsbDetachedReceiver mDetachedReceiver;

	UsbManager mUsbManager;
	UsbDevice mDevice;
	UsbInterface mInterface;
	UsbDeviceConnection mConnection;
	UsbEndpoint mEndpointIntr;

	boolean mPermissionRequested = false;

	static final String ACTION_USB_PERMISSION = GlitterPanelFragment.class
			.getCanonicalName() + ".USB_PERMISSION";

	/** 輝度 */
	public static enum Brightness {
		PER_25, PER_50, PER_75, PER_100,
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		if (activity instanceof GlitterPanelEventCallback == false) {
			throw new IllegalArgumentException("activity not implement "
					+ GlitterPanelEventCallback.class.getSimpleName() + ".");
		}

		mCallback = (GlitterPanelEventCallback) activity;

		mUsbManager = (UsbManager) activity
				.getSystemService(Context.USB_SERVICE);

		mPermissionIntent = PendingIntent.getBroadcast(activity, 0, new Intent(
				ACTION_USB_PERMISSION), 0);

		IntentFilter filter;
		mGrantReceiver = new PermissionGrantReceiver();
		filter = new IntentFilter(ACTION_USB_PERMISSION);
		activity.registerReceiver(mGrantReceiver, filter);

		mDetachedReceiver = new UsbDetachedReceiver();
		filter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
		activity.registerReceiver(mDetachedReceiver, filter);
	}

	@Override
	public void onStart() {
		super.onStart();

		connect();
	}

	@Override
	public void onStop() {
		super.onStop();

		disconnect();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		getActivity().unregisterReceiver(mGrantReceiver);
		getActivity().unregisterReceiver(mDetachedReceiver);
	}

	public boolean connect() {
		if (isConnected()) {
			return true;
		}

		UsbDevice device = findGlitterPanel();
		if (device == null) {
			return false;
		}

		if (!isGlitterPanel(device)) {
			throw new IllegalStateException("device is not GlitterPanel.");
		}

		if (device.getInterfaceCount() != 1) {
			throw new IllegalStateException();
		}

		UsbInterface usbInterface = device.getInterface(0);
		if (usbInterface.getEndpointCount() != 2) {
			throw new IllegalStateException();
		}

		if (mUsbManager.hasPermission(device)) {
			// 権限あるなら別におｋ
		} else if (mPermissionRequested) {
			// 権限要求ループ停止用
			Log.d(TAG, "grant requested connect to glitter panel...");
			return false;
		} else {
			// デバイスがまだ接続されているかを一応チェック
			if (!mUsbManager.getDeviceList().containsValue(device)) {
				Log.d(TAG,
						"request permission... but device is disconnected...");
				return false;
			}

			// 権限なくて未リクエストなら要求する
			Log.d(TAG, "request permission that connect to glitter panel...");
			mPermissionRequested = true;
			mUsbManager.requestPermission(device, mPermissionIntent);
			mCallback.onRequestPermission();
			return false;
		}

		@SuppressWarnings("unused")
		UsbEndpoint endpoint0 = usbInterface.getEndpoint(0);
		UsbEndpoint endpoint1 = usbInterface.getEndpoint(1);

		mDevice = device;
		mInterface = usbInterface;
		mEndpointIntr = endpoint1;

		mConnection = mUsbManager.openDevice(mDevice);

		if (mConnection == null
				|| !mConnection.claimInterface(usbInterface, true)) {
			Log.d(TAG, "connection rejected...");
			return false;
		}

		mCallback.onConnected();

		return true;
	}

	void disconnect() {
		if (mConnection != null) {
			mConnection.releaseInterface(mInterface);
			mConnection.close();
			mConnection = null;

			mCallback.onDisconnected();
		}
	}

	public void display(boolean[] datas) {
		display(Brightness.PER_100, datas);
	}

	public void display(Brightness brightness, boolean[] datas) {
		if (!isConnected()) {
			return;
		}

		if (datas == null || datas.length != PIXELS) {
			throw new IllegalArgumentException();
		}

		byte[] message = new byte[6 + 32 + 2];
		int idx = 0;

		// control
		message[idx++] = castToUnsignedByte(0x80);
		message[idx++] = castToUnsignedByte(0x03);
		int brightnessByte;
		switch (brightness) {
		case PER_25:
			brightnessByte = 0x50;
			break;
		case PER_50:
			brightnessByte = 0x50 | (0x01 << 2);
			break;
		case PER_75:
			brightnessByte = 0x50 | (0x02 << 2);
			break;
		case PER_100:
			brightnessByte = 0x50 | (0x03 << 2);
			break;
		default:
			brightnessByte = 0x50;
			break;
		}
		message[idx++] = castToUnsignedByte(brightnessByte);
		message[idx++] = castToUnsignedByte(0x08);
		message[idx++] = castToUnsignedByte(0x00);
		message[idx++] = castToUnsignedByte(0x00);

		// data
		for (int i = 0; i < PIXELS / 8; i++) {
			int data8px = 0;
			for (int j = 0; j < 8; j++) {
				data8px <<= 1;
				if (datas[i * 8 + j] == true) {
					data8px += 1;
				}
			}
			message[idx++] = castToUnsignedByte(data8px);
		}

		// end 2byte
		message[idx++] = 0x00;
		message[idx++] = 0x00;

		synchronized (this) {
			mConnection.bulkTransfer(mEndpointIntr, message, message.length,
					100);
		}
	}

	public boolean isConnected() {
		return mConnection != null;
	}

	UsbDevice findGlitterPanel() {
		{ // まずIntentからの取得を試みる
			Intent intent = getActivity().getIntent();
			if (intent != null) {
				String action = intent.getAction();
				if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
					UsbDevice device = (UsbDevice) intent
							.getParcelableExtra(UsbManager.EXTRA_DEVICE);
					if (isGlitterPanel(device)) { // 他のはこないはずだけど一応
						Log.d(TAG, "GlitterPanel detected! by intent.");
						return device;
					}
				}
			}
		}

		{ // 次に接続済デバイス一覧からの取得を試みる
			HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
			for (String key : deviceList.keySet()) {
				UsbDevice device = deviceList.get(key);
				if (isGlitterPanel(device)) {
					Log.d(TAG, "GlitterPanel detected! by UsbManager.");
					return device;
				}
			}
		}

		return null;
	}

	static boolean isGlitterPanel(UsbDevice device) {
		if (device == null) {
			return false;
		}

		int productId = device.getProductId();
		int vendorId = device.getVendorId();

		if (vendorId == VENDOR_TAKARATOMY && productId == PRODUCT_GLITTER_PANEL) {
			return true;
		}

		return false;
	}

	class PermissionGrantReceiver extends BroadcastReceiver {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action)) {
				if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED,
						false)) {
					mPermissionRequested = false;
					mCallback.onPermissionGrant();
					connect();
				} else {
					mPermissionRequested = false;
					mCallback.onPermissionReject();
				}
			}
		}
	}

	class UsbDetachedReceiver extends BroadcastReceiver {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
				UsbDevice device = (UsbDevice) intent
						.getParcelableExtra(UsbManager.EXTRA_DEVICE);
				if (device != null) {
					disconnect();
				}
			}
		}
	}

	static byte castToUnsignedByte(int val) {
		return (byte) val;
	}
}
