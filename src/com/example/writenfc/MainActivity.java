package com.example.writenfc;

import java.io.IOException;
import java.nio.charset.Charset;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.Ndef;

public class MainActivity extends Activity {

	private static final String SMART_POSTER_URL = "http://infoseek.co.jp/";
	private static final String TAG = "NFCTAG";
	private NfcAdapter nfcAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		nfcAdapter = NfcAdapter.getDefaultAdapter(getApplicationContext());

	}

	@Override
	public void onResume() {

		super.onResume();
		PendingIntent pi = createPendingIntent();
		nfcAdapter.enableForegroundDispatch(this, pi, null, null);

	}

	@Override
	protected void onPause() {

		super.onPause();
		nfcAdapter.disableForegroundDispatch(this); // 【4】

	}

	private PendingIntent createPendingIntent() {
		Intent i = new Intent(this, MainActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
				| Intent.FLAG_ACTIVITY_NEW_TASK); // 【5】
		return PendingIntent.getActivity(this, 0, i, 0);
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

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Tag tag = (Tag) intent.getParcelableExtra(NfcAdapter.EXTRA_TAG); // 【1】
		if (tag == null) {
			// NFCタグがかざされたのとは異なる理由でインテントが飛んできた
			Log.d(TAG, "想定外のIntent受信です: action = " + intent.getAction());
			return;
		}

		Ndef ndefTag = Ndef.get(tag); // 【2】
		if (ndefTag == null) {
			// NDEFフォーマットされていないタグがかざされた
			Log.d(TAG, "NDEF形式ではないタグがかざされました。");
			return;
		}

		NdefMessage ndefMessage = createSmartPoster(SMART_POSTER_URL); // 【3】
		if (writeNdefMessage(ndefTag, ndefMessage)) { // 【4】
			Toast.makeText(this, "書き込みに成功しました。", Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(this, "書き込みに失敗しました。", Toast.LENGTH_LONG).show();
		}
	}

	private NdefRecord createUriRecord(String url) {
		return NdefRecord.createUri(url); // 【1】
	}

	private NdefRecord createActionRecord() {
		byte[] typeField = "act".getBytes(Charset.forName("US-ASCII"));
		byte[] payload = { (byte) 0x00 };
		return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, typeField,
				new byte[0], payload); // 【2】
	}

	public NdefMessage createSmartPoster(String url) {
		NdefRecord[] rs = new NdefRecord[] { createUriRecord(url),
				createActionRecord() };
		NdefMessage spPayload = new NdefMessage(rs); // 【3】

		NdefRecord spRecord = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
				NdefRecord.RTD_SMART_POSTER, new byte[0],
				spPayload.toByteArray()); // 【4】
		return new NdefMessage(new NdefRecord[] { spRecord });
	}

	private boolean writeNdefMessage(Ndef ndefTag, NdefMessage ndefMessage) {
		if (!ndefTag.isWritable()) { // 【1】
			// このタグは書き込めないので、何もしないでreturnする。
			Log.d(TAG, "このタグはRead Onlyです。");
			return false;
		}
		int messageSize = ndefMessage.toByteArray().length;
		if (messageSize > ndefTag.getMaxSize()) { // 【2】
			// タグの書き込み可能サイズを超えているので、書き込めない。
			return false;

		}

		try {
			if (!ndefTag.isConnected()) {
				ndefTag.connect(); // 【3】
			}
			ndefTag.writeNdefMessage(ndefMessage); // 【4】
			return true;
		} catch (TagLostException e) {
			// Tagが途中で離された。
			Log.d(TAG, "タグが途中で離れてしまいました。", e);
			return false;
		} catch (IOException e) {
			// その他のIOエラー
			Log.d(TAG, "IOエラーです。", e);
			return false;
		} catch (FormatException e) {
			// 書き込もおうとしているNDEFメッセージが壊れている。
			Log.d(TAG, "書き込もうとしているNDEFメッセージが壊れています", e);
			return false;
		} finally {
			try {
				ndefTag.close(); // 【5】
			} catch (IOException e) {
				// ignore.
			}
		}
	}
}
