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
		nfcAdapter.disableForegroundDispatch(this); // �y4�z

	}

	private PendingIntent createPendingIntent() {
		Intent i = new Intent(this, MainActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
				| Intent.FLAG_ACTIVITY_NEW_TASK); // �y5�z
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
		Tag tag = (Tag) intent.getParcelableExtra(NfcAdapter.EXTRA_TAG); // �y1�z
		if (tag == null) {
			// NFC�^�O���������ꂽ�̂Ƃ͈قȂ闝�R�ŃC���e���g�����ł���
			Log.d(TAG, "�z��O��Intent��M�ł�: action = " + intent.getAction());
			return;
		}

		Ndef ndefTag = Ndef.get(tag); // �y2�z
		if (ndefTag == null) {
			// NDEF�t�H�[�}�b�g����Ă��Ȃ��^�O���������ꂽ
			Log.d(TAG, "NDEF�`���ł͂Ȃ��^�O����������܂����B");
			return;
		}

		NdefMessage ndefMessage = createSmartPoster(SMART_POSTER_URL); // �y3�z
		if (writeNdefMessage(ndefTag, ndefMessage)) { // �y4�z
			Toast.makeText(this, "�������݂ɐ������܂����B", Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(this, "�������݂Ɏ��s���܂����B", Toast.LENGTH_LONG).show();
		}
	}

	private NdefRecord createUriRecord(String url) {
		return NdefRecord.createUri(url); // �y1�z
	}

	private NdefRecord createActionRecord() {
		byte[] typeField = "act".getBytes(Charset.forName("US-ASCII"));
		byte[] payload = { (byte) 0x00 };
		return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, typeField,
				new byte[0], payload); // �y2�z
	}

	public NdefMessage createSmartPoster(String url) {
		NdefRecord[] rs = new NdefRecord[] { createUriRecord(url),
				createActionRecord() };
		NdefMessage spPayload = new NdefMessage(rs); // �y3�z

		NdefRecord spRecord = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
				NdefRecord.RTD_SMART_POSTER, new byte[0],
				spPayload.toByteArray()); // �y4�z
		return new NdefMessage(new NdefRecord[] { spRecord });
	}

	private boolean writeNdefMessage(Ndef ndefTag, NdefMessage ndefMessage) {
		if (!ndefTag.isWritable()) { // �y1�z
			// ���̃^�O�͏������߂Ȃ��̂ŁA�������Ȃ���return����B
			Log.d(TAG, "���̃^�O��Read Only�ł��B");
			return false;
		}
		int messageSize = ndefMessage.toByteArray().length;
		if (messageSize > ndefTag.getMaxSize()) { // �y2�z
			// �^�O�̏������݉\�T�C�Y�𒴂��Ă���̂ŁA�������߂Ȃ��B
			return false;

		}

		try {
			if (!ndefTag.isConnected()) {
				ndefTag.connect(); // �y3�z
			}
			ndefTag.writeNdefMessage(ndefMessage); // �y4�z
			return true;
		} catch (TagLostException e) {
			// Tag���r���ŗ����ꂽ�B
			Log.d(TAG, "�^�O���r���ŗ���Ă��܂��܂����B", e);
			return false;
		} catch (IOException e) {
			// ���̑���IO�G���[
			Log.d(TAG, "IO�G���[�ł��B", e);
			return false;
		} catch (FormatException e) {
			// �������������Ƃ��Ă���NDEF���b�Z�[�W�����Ă���B
			Log.d(TAG, "�����������Ƃ��Ă���NDEF���b�Z�[�W�����Ă��܂�", e);
			return false;
		} finally {
			try {
				ndefTag.close(); // �y5�z
			} catch (IOException e) {
				// ignore.
			}
		}
	}
}
