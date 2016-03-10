package cActivity;


import java.lang.ref.WeakReference;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Presence;
import org.videolan.vlc.R;
import org.videolan.vlc.gui.VLCMainActivity;

import aOpenFireService.ServiceException;
import aOpenFireService.UserService;
import aOpenFireService.UserServiceImpl;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo.State;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends Activity implements OnClickListener {

	public static final int LOGIN_SUCCESS = 1;
	public final static String LOGIN_SUCCESSFULL = "login success";
	public static final String URL_DISCONNECTION = "Application Error! Response isn't ok!";
	public static final String USER_PASSWORD_NULL = "username or password is null!";
	public static final String USERNAME_EMAIL_NOT_EXIST = "username or email is not exist (registed)!";
	public static final String LOGIN_FAILED = "Connect time out,check network and login again!";
	public static final String PASSWORD_IS_WRONG = "password is wrong !";
	public static final String UNKNOW_ERROR = "unknow error!";
	public static final String SERVER_ERROR = "remote server error (not start)!";
	public static final String CONNECT_TIME_OUT = "Connect time out, please check the network!";
	public static final String REQUEST_TIME_OUT = "Request time out, try again later!!";
	public static final String HTTP_CONNECTION_REFUSED = "http://... refused, please login later.";
	
	private Button loginBtn;
	private Button registerBtn;
	private EditText inputUsername;
	private EditText inputPassword;
	private CheckBox cheRempwd;
	private static ProgressDialog mDialog;
	public SharedPreferences sp;
	private UserService userService ;

	boolean registerFlag;
	public static XMPPConnection connection;

	private String username;
	private String password;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login);
		initView();
		sp = this.getSharedPreferences("userInfo", Context.MODE_PRIVATE);
		userService = new UserServiceImpl();
		username = inputUsername.getText().toString();
		password = inputPassword.getText().toString();
		
		if (sp.getBoolean("ISCHECK", false)) {
			// set checked default is true
			cheRempwd.setChecked(true);
			inputUsername.setText(sp.getString("USERNAME", username));
			inputPassword.setText(sp.getString("PASSWORD", password));
		}

	}

	public void initView() {

		loginBtn = (Button) findViewById(R.id.btn_login);
		registerBtn = (Button) findViewById(R.id.btn_register);
		inputUsername = (EditText) findViewById(R.id.edit_username);
		inputPassword = (EditText) findViewById(R.id.edit_password);
		cheRempwd = (CheckBox) findViewById(R.id.cb_savepwd);
		loginBtn.setOnClickListener(this);
		registerBtn.setOnClickListener(this);
		cheRempwd.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (cheRempwd.isChecked()) {
					sp.edit().putBoolean("ISCHECK", true).commit();
				} else {
					sp.edit().putBoolean("ISCHECK", false).commit();
				}
			}
		});

		CheckNetworkState();

		/** register success, get the username & password. */
		if (!registerFlag) {
			Intent intent = getIntent();
			inputUsername.setText(intent.getStringExtra("username"));
			inputPassword.setText(intent.getStringExtra("password"));
		}

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_login:
			login();
			break;
		case R.id.btn_register:
			register();
			break;
		default:
			break;
		}
	}

	private void register() {
		sp.edit().putBoolean("firstStart", true);
		sp.edit().commit();
		Intent intent = new Intent(this, RegisterActivity.class);
		startActivity(intent);
		intent = null;
	}

	private void login() {

		username = inputUsername.getText().toString();
		password = inputPassword.getText().toString();

		mDialog = new ProgressDialog(LoginActivity.this);
		mDialog.setTitle("login");
		mDialog.setMessage("loading......");
		mDialog.show();

		Thread loginThread = new Thread(new LoginThread());
		loginThread.start();

	}

	// LoginThread
	private class LoginThread implements Runnable {
		@Override
		public void run() {

			try {
				connection = userService.userLogin(username, password);
			} catch (ServiceException e) {
				e.printStackTrace();
				Message msg = new Message();
				Bundle data = new Bundle();
				data.putSerializable("ErrorMsg", e.getMessage());
				msg.setData(data);
				handler.sendMessage(msg);
			} catch (Exception e) {
				e.printStackTrace();

			}
			if (connection!=null) {
				handler.sendEmptyMessage(LOGIN_SUCCESS);
				if (cheRempwd.isChecked()) {
					// remember username & pwd
					Editor editor = sp.edit();
					editor.putString("USERNAME", username);
					editor.putString("PASSWORD", password);
					editor.commit();
				}
				// Set the status to available (online)
				Presence presence = new Presence(Presence.Type.available);
				connection.sendPacket(presence);

				Intent intent = new Intent();
				// for [VideoPlayerActivity] reconnect to the XMPP server
				intent.putExtra("username", username);
				intent.putExtra("password", password);
//				intent.putExtra("entries", entries);
				intent.setClass(LoginActivity.this, VLCMainActivity.class);
				startActivity(intent);
				LoginActivity.this.finish();
			}

		}

	}

	private static class IHandler extends Handler {
		private final WeakReference<Activity> mActivity;

		public IHandler(LoginActivity loginActivity) {
			mActivity = new WeakReference<Activity>(loginActivity);
		}

		@Override
		public void handleMessage(Message msg) {
			if (mDialog != null)
				mDialog.dismiss();
			switch (msg.what) {
			case 0:
				String message = (String) msg.getData().getSerializable(
						"ErrorMsg");
				((LoginActivity) mActivity.get()).showInfo(message);
				break;
			case LOGIN_SUCCESS:
				// ((RegisterActivity)mActivity.get()).showInfo(REGISTER_SUCCESSFULL);
				Toast.makeText((LoginActivity) mActivity.get(),
						LOGIN_SUCCESSFULL, Toast.LENGTH_SHORT).show();

				break;

			default:
				break;
			}
		}
	};

	private IHandler handler = new IHandler(this);

	private void showInfo(String str) {
		Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
	}

	// check network state function
	private void CheckNetworkState() {
		// /boolean flag = false;
		ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		State mobile = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
				.getState();
		State wifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
				.getState();
		// if 3G, wifi or 2G network is connected, return; else setting network
		if (mobile == State.CONNECTED || mobile == State.CONNECTING)
			return;
		if (wifi == State.CONNECTED || wifi == State.CONNECTING)
			return;
		showNeworkTips();
	}

	private void showNeworkTips() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setIcon(android.R.drawable.ic_dialog_alert);
		builder.setTitle("Network Disconnected");
		builder.setMessage("Current network is unavailable, set network?");
		builder.setPositiveButton("yes", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				// setting network activity
				startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
			}
		});
		builder.setNegativeButton("cancel",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
						// LoginActivity.this.finish();
					}
				});
		builder.create();
		builder.show();
	}
}
