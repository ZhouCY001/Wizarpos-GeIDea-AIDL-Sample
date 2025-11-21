package com.wizarpos.paymentrouterclient;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.speech.tts.TextToSpeech;
import android.text.InputFilter;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.wizarpos.payment.aidl.IPaymentPay;
import com.wizarpos.payment.aidl.IPaymentPayCallback;
import com.wizarpos.payment.aidl.PinpadCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class MainActivity extends Activity implements OnClickListener {

	public static String Purchase = "Purchase";
	public static String VoidSale = "VoidSale";
	public static String Refund = "Refund";
	public static String PreAuth = "PreAuth";
	public static String IncrementalAuth = "IncrementalAuth";
	public static String AuthCompletion = "AuthCompletion";
	public static String Reversal = "Reversal";
	public static String GetPosInfo = "GetPosInfo";
	public static String PrintLast = "PrintLast";


	private String param, response , callbackMSG;
	boolean appIconVisibility = true;

	private IPaymentPay mWizarPayment;
	final ServiceConnection mConnPayment = new PaymentConnection();

	String transAmount, oriTrace ,oriRRN;

	TextToSpeech textToSpeech = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		int[] btnIds = { R.id.bind, R.id.unbind , R.id.cancelTrans
			, R.id.setPayCallback, R.id.setPinpadCallback
			, R.id.Sale, R.id.VoidSale, R.id.Refund
			, R.id.PreAuth, R.id.AuthIncrement, R.id.AuthCompletion
			, R.id.Reversal, R.id.getPOSInfo, R.id.printlast
			, R.id.setParams,
		};
		for (int id : btnIds) {
			findViewById(id).setOnClickListener(this);
		}

		Switch sw = (Switch) findViewById(R.id.switch_icon_visible);
		sw.setChecked(appIconVisibility);
		sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				appIconVisibility = isChecked;
			}
		});

		String engineName = "com.google.android.tts";
		textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
			@Override
			public void onInit(int status) {
				if (status == TextToSpeech.SUCCESS) {
					Log.i("TTS", "TTS initialization SUCCESS with engine: " + engineName);

					Locale locale = Locale.getDefault();
					int check = textToSpeech.isLanguageAvailable(locale);
					if (check == TextToSpeech.LANG_MISSING_DATA || check == TextToSpeech.LANG_NOT_SUPPORTED) {
						Log.e("TTS", "Language " + locale.toString() + " not supported or missing data");
						Intent installIntent = new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
						startActivity(installIntent);
					} else {
						textToSpeech.setLanguage(locale);
						textToSpeech.setPitch(1.2f);
						textToSpeech.setSpeechRate(0.90f);
						textToSpeech.speak("TTS is ready", TextToSpeech.QUEUE_FLUSH, null, "PaymentRouterClient");
					}
				} else {
					Log.e("TTS", "TTS initialization FAILED (status=" + status + ")");
				}
			}
		}, engineName);

	}

	@Override
	public void onBackPressed() {
		System.exit(0);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindPaymentRouter();
	}

	class PaymentConnection implements ServiceConnection {
		@Override
		public void onServiceConnected(ComponentName compName, IBinder binder) {
			Log.d("onServiceConnected", "compName: " + compName);
			mWizarPayment = IPaymentPay.Stub.asInterface(binder);
			showResponse("Connect Success!");
		}

		@Override
		public void onServiceDisconnected(ComponentName compName) {
			Log.d("onServiceDisconnected", "compName: " + compName);
			mWizarPayment = null;
			showResponse("Disconnect Success!");
		}
	};

	private void bindPaymentRouter() {
		if (mWizarPayment == null) {
			Intent intent = new Intent("com.wizarpos.payment.aidl.pay");
			intent.setPackage("com.wizarpos.geidea");
			bindService(intent, mConnPayment, BIND_AUTO_CREATE);
		}
	}
	private void unbindPaymentRouter() {
		if (mWizarPayment != null) {
			unbindService(mConnPayment);
			mWizarPayment = null;
		}
	}

	public void showResponse(String response) {
		this.response = response;
		showResponse();
	}

	public void showCallbackMessage(String callbackMSG){
		this.callbackMSG = callbackMSG;
		showResponse();
	}

	public void showResponse() {
		runOnUiThread(()->
		{
			{
				setTextById(R.id.callBack, callbackMSG);
				setTextById(R.id.result, response);
			}
		});

	}
	private void setTextById(int id, CharSequence text) {
		((TextView)findViewById(id)).setText(text);
	}

	@Override
	public void onClick(final View view) {
		final int btnId = view.getId();
		setTextById(R.id.method, ((TextView)view).getText());

		param = "";
		response = "";
		switch(btnId) {
		case R.id.bind:				bindPaymentRouter();    break;
		case R.id.unbind:			unbindPaymentRouter();  break;
		case R.id.cancelTrans:
			new Thread(() -> {
				try {
					mWizarPayment.cancelRequest			("");
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}).start();

			break;
		default:
			if (mWizarPayment == null) {
				response = "Please click [Bind First]!";
				showResponse();
			} else {
				// 先获取输入，等输入完成后再执行transact
				handleTransactionWithInput(btnId);
			}
			break;
		}
	}

	private void handleTransactionWithInput(int btnId) {
		switch (btnId) {
		case R.id.setPayCallback:
			try {
				mWizarPayment.addProcedureCallback(paymentPayCallback);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
			break;
		case R.id.setPinpadCallback:
			try {
				mWizarPayment.addPinpadCallback(pinpadCallback);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
			break;
		case R.id.Sale:
		case R.id.PreAuth:
			showInputDialog("Input Trans Amount", 9, input -> {
				if (input == null) return; // input is cancelled
				transAmount = input;
				try {
					JSONObject json = new JSONObject();
					if (btnId == R.id.PreAuth)
						setParam4PreAuth(json);
					else
						setParam4PayCash(json);

					param = json.toString();
					createAsyncTask().execute(btnId);
				} catch (JSONException e) {
					e.printStackTrace();
					showResponse("JSON Error");
				}
			});
			break;
		case R.id.Refund:
			showInputDialog("Input Trans Amount", 9, amount -> {
				if (amount == null) return;
				transAmount = amount;
				showInputDialog("Input original RRN", 12, rrn -> {
					if (rrn == null) return;
					oriRRN = rrn;
					try {
						JSONObject json = new JSONObject();
						setparam4Refund(json);
						param = json.toString();
						createAsyncTask().execute(btnId);
					} catch (JSONException e) {
						e.printStackTrace();
						showResponse("JSON Error");
					}
				});
			});
			break;
		case R.id.AuthIncrement:
		case R.id.AuthCompletion:
			showInputDialog("Input Trans Amount", 9, amount -> {
				if (amount == null) return;
				transAmount = amount;

				showInputDialog("Input oriTrace", 6, trace -> {
					if (trace == null) return;
					oriTrace = trace;

					try {
						JSONObject json = new JSONObject();

						if(btnId == R.id.VoidSale)
							setparam4VoidSale(json);
						else if (btnId == R.id.AuthIncrement)
							setparam4AuthIncrement(json);
						else if (btnId == R.id.AuthCompletion)
							setparam4AuthComp(json);

						param = json.toString();
						createAsyncTask().execute(btnId);
					} catch (JSONException e) {
						e.printStackTrace();
						showResponse("JSON Error");
					}
				});
			});
			break;
		case R.id.VoidSale:
		case R.id.Reversal:
			showInputDialog("Input oriTrace", 6, trace -> {
				if (trace == null) return;
				oriTrace = trace;

				try {
					JSONObject json = new JSONObject();

					if(btnId == R.id.VoidSale)
						setparam4VoidSale(json);
					else if (btnId == R.id.Reversal)
						setparam4Reversal(json);

					param = json.toString();
					createAsyncTask().execute(btnId);
				} catch (JSONException e) {
					e.printStackTrace();
					showResponse("JSON Error");
				}
			});
			break;
		case R.id.printlast:
		case R.id.getPOSInfo:
			try {
				JSONObject json = new JSONObject();
				if (btnId == R.id.printlast)
					setParam4getPrintLast(json);
				else
					setParam4getPOSInfo(json);

				param = json.toString();
				createAsyncTask().execute(btnId);
			} catch (JSONException e) {
				e.printStackTrace();
				showResponse("JSON Error");
			}
			break;
		case R.id.setParams:
			try {
				JSONObject json = new JSONObject();
				setParam4setPaymentAPPParam(json);
				param = json.toString();
				createAsyncTask().execute(btnId);
			} catch (JSONException e) {
				e.printStackTrace();
				showResponse("JSON Error");
			}
			break;

		}
	}


	private AsyncTask<Integer, Void, String> createAsyncTask() {
		return new AsyncTask<Integer, Void, String>() {
			protected void onPreExecute() {
				showResponse("...");
			}
			protected String doInBackground(Integer...btnIds) {
				Log.d("doInBackground", "Request: " + param + " mWizarPayment: " + mWizarPayment);

				String result = "Skipped";
				try {
					switch(btnIds[0]) {
					case R.id.Sale:
					case R.id.VoidSale:
					case R.id.Refund:
					case R.id.PreAuth:
					case R.id.AuthIncrement:
					case R.id.AuthCompletion:
					case R.id.Reversal:
					case R.id.printlast:
					case R.id.getPOSInfo:
						result = mWizarPayment.transact			(param);
						break;
					case R.id.setParams:
						result = mWizarPayment.setParam			(param);
						break;
					}
				} catch (RemoteException e) {
					result = e.getMessage();
				}

				Log.d("doInBackground", "Response: " + result);
				return result;
			}
			protected void onPostExecute(String result) {
				showResponse(result);
			}
		};
	}


	private void setParam4PayCash(JSONObject jsonObject) throws JSONException {
		jsonObject.put("TransType", Purchase);
		jsonObject.put("CurrencyCode", "784");
		jsonObject.put("TransIndexCode", "1234561");//Third application transaction order ID，This must be not repeated

		if(notEmptyString(transAmount))
			jsonObject.put("TransAmount", transAmount);
	}

	private void setParam4PreAuth(JSONObject jsonObject) throws JSONException {
		jsonObject.put("TransType", PreAuth);
		jsonObject.put("CurrencyCode", "784");
		jsonObject.put("TransIndexCode", "1234570");//Third application transaction order ID，This must be not repeated

		if(notEmptyString(transAmount))
			jsonObject.put("TransAmount", transAmount);
	}

	private void setparam4VoidSale(JSONObject jsonObject) throws JSONException {
		jsonObject.put("TransType", VoidSale);
		jsonObject.put("CurrencyCode", "784");
		jsonObject.put("TransIndexCode", "1234561");//Third application transaction order ID，This must be not repeated

		jsonObject.put("OriTraceNum",oriTrace);
	}

	private void setparam4Reversal(JSONObject jsonObject) throws JSONException {
		jsonObject.put("TransType", Reversal);
		jsonObject.put("CurrencyCode", "784");
		jsonObject.put("TransIndexCode", "1234571");//Third application transaction order ID，This must be not repeated

		if(notEmptyString(oriTrace))
			jsonObject.put("OriTraceNum", oriTrace);	//This value should be same of the 'trace' in Sale response data.
	}




	private void setparam4Refund(JSONObject jsonObject) throws JSONException {
		jsonObject.put("TransType", Refund);
		jsonObject.put("CurrencyCode", "784");

		if(notEmptyString(transAmount))
			jsonObject.put("TransAmount", transAmount);

		if(notEmptyString(oriRRN))
			jsonObject.put("OriRrn", oriRRN);	//This value should be same of the 'trace' in Sale response data.

	}




	private void setparam4AuthIncrement(JSONObject jsonObject) throws JSONException {
		jsonObject.put("TransType", IncrementalAuth);
		jsonObject.put("CurrencyCode", "784");
		jsonObject.put("TransIndexCode", "1234571");//Third application transaction order ID，This must be not repeated

		if(notEmptyString(transAmount))
			jsonObject.put("TransAmount", transAmount);
		if(notEmptyString(oriTrace))
			jsonObject.put("OriTraceNum", oriTrace);	//This value should be same of the 'trace' in Sale response data.


	}


	private void setparam4AuthComp(JSONObject jsonObject) throws JSONException {
		jsonObject.put("TransType", AuthCompletion);
		jsonObject.put("CurrencyCode", "784");
		jsonObject.put("TransIndexCode", "1234571");//Third application transaction order ID，This must be not repeated


		if(notEmptyString(transAmount))
			jsonObject.put("TransAmount", transAmount);
		if(notEmptyString(oriTrace))
			jsonObject.put("OriTraceNum", oriTrace);	//This value should be same of the 'trace' in Sale response data.

	}


	private void setParam4getPOSInfo(JSONObject jsonObject) throws JSONException {
		jsonObject.put("TransType", GetPosInfo);
	}

	private void setParam4getPrintLast(JSONObject jsonObject) throws JSONException {
		jsonObject.put("TransType", PrintLast);
	}


	private void setParam4setPaymentAPPParam(JSONObject jsonObject) throws JSONException {
		jsonObject.put("AppIconVisible", appIconVisibility);
	}

	public interface InputCallback {
		void onInput(String input);
	}

	private void showInputDialog(String title,int maxInput,InputCallback callback) {
		final EditText editText = new EditText(MainActivity.this);
		InputFilter[] filters = {new InputFilter.LengthFilter(maxInput)};
		editText.setFilters(filters);
		AlertDialog.Builder inputDialog =
			new AlertDialog.Builder(MainActivity.this);
		inputDialog.setTitle(title).setView(editText);
		inputDialog.setPositiveButton("Confirm", (dialog, which) -> {
			String text = editText.getText().toString();
			callback.onInput(text);
		});
		inputDialog.setNegativeButton("Cancel", (dialog, which) -> {
			callback.onInput(null); // 或其他取消逻辑
		});


		AlertDialog alertDialog = inputDialog.create();
		alertDialog.setCanceledOnTouchOutside(false);
		alertDialog.show();
	}


	private boolean notEmptyString(String str){
		if(str!=null && !str.isEmpty())
			return true;

		return false;
	}


	public IPaymentPayCallback paymentPayCallback = new IPaymentPayCallback.Stub() {
		@Override
		public void process(int processCode, String processMsg)  {
			String str = "paymentPayCallback->Code:" + processCode + ",processMsg:" + processMsg;
			Log.w("paymentPayCallback", str);
			showCallbackMessage(str);

			if(textToSpeech!=null)
				textToSpeech.speak(processMsg, TextToSpeech.QUEUE_FLUSH, null, "PaymentRouterClient");


		}

	};

	public PinpadCallback pinpadCallback = new PinpadCallback.Stub() {


		@Override
		public void processCallbackOnlinePin(int data) throws RemoteException {
			String str = "online pin count " + data ;
			Log.w("paymentPayCallback", str);
			showCallbackMessage(str);

			if(textToSpeech!=null)
				textToSpeech.speak(str, TextToSpeech.QUEUE_FLUSH, null, "PaymentRouterClient");

		}

		@Override
		public void processCallbackOfflinePin(int nCount, int nExtra) throws RemoteException {
			String str = "offline pin count " + nCount ;
			Log.w("paymentPayCallback", str);
			showCallbackMessage(str);

			if(textToSpeech!=null)
				textToSpeech.speak(str, TextToSpeech.QUEUE_FLUSH, null, "PaymentRouterClient");
		}
	};
}


