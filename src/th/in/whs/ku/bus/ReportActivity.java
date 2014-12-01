package th.in.whs.ku.bus;

import java.io.IOException;

import th.in.whs.ku.bus.api.API;
import th.in.whs.ku.bus.util.ExitMessageFragment;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.joshdholtz.sentry.Sentry;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Request.Builder;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

public class ReportActivity extends ActionBarActivity {
	
	public static String REPORT_BUS = "REPORT_BUS";
	
	private int[] reportTypesInt = null;
	
	private String[] reportType = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.report);
		
		Intent intent = getIntent();
		if(intent.hasExtra(REPORT_BUS)){
			((TextView) findViewById(R.id.line)).setText(intent.getStringExtra(REPORT_BUS));
		}else{
			findViewById(R.id.reportLine).setVisibility(View.GONE);
			((TextView) findViewById(R.id.textView3)).setText(R.string.report_line_map);
		}
		
		Spinner type = (Spinner) findViewById(R.id.spinner1);
		type.setAdapter(getTypeAdapter());
		
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		
		EditText text = (EditText) findViewById(R.id.editText1);
		text.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
			}
			
			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
					int arg3) {
			}
			
			@Override
			public void afterTextChanged(Editable arg0) {
				supportInvalidateOptionsMenu();
			}
		});
		
		((KuBusApplication) getApplication()).report("ReportActivity");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.submit, menu);
		
		menu.findItem(R.id.submit).setEnabled(getBody().length() > 0);
		
		return super.onCreateOptionsMenu(menu);
	}

	private SpinnerAdapter getTypeAdapter() {
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, getReportTypes());
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		return adapter;
	}
	
	private String[] getReportTypes(){
		if(reportType != null){
			return reportType;
		}
		reportType = buildReportTypes();
		return reportType;
	}
	
	private String[] buildReportTypes(){
		Intent intent = getIntent();
		if(intent.hasExtra(REPORT_BUS)){
			reportTypesInt = new int[]{
				R.string.report_type_line,
				R.string.report_type_driver,
				R.string.report_type_comment,
				R.string.report_type_bug,
			};
		}else{
			reportTypesInt = new int[]{
				R.string.report_type_comment,
				R.string.report_type_bug
			};
		}
		
		String[] out = new String[reportTypesInt.length];
		for(int i = 0; i < reportTypesInt.length; i++){
			out[i] = getString(reportTypesInt[i]);
		}
		return out;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.submit:
			submit();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	protected void submit(){
		Builder request = new Request.Builder()
			.url(getEndpoint() + "/issues/new");
		FormEncodingBuilder formBody = new FormEncodingBuilder();
		int type = getSelectedType();
		formBody.add("format", "json");
		formBody.add("title", getString(type));
		formBody.add("description", getDescription());
		switch(type){
		case R.string.report_type_bug:
			formBody.add("issuetype", "bugreport");
			break;
		case R.string.report_type_line:
		case R.string.report_type_driver:
			formBody.add("issuetype", "enhancement");
			break;
		case R.string.report_type_comment:
			formBody.add("issuetype", "idea");
			break;
		}
		
		switch(type){
		case R.string.report_type_line:
			formBody.add("category", "Line");
			break;
		case R.string.report_type_driver:
			formBody.add("category", "Driver");
			break;
		}
		
		RequestBody body = formBody.build();
		request.post(body);
		
		final ProgressDialog progress = ProgressDialog.show(this, "", getString(R.string.loading), true);
		API.client.newCall(request.build()).enqueue(new Callback(){

			@Override
			public void onFailure(Request arg0, IOException arg1) {
				progress.dismiss();
				Toast.makeText(ReportActivity.this, R.string.internet_error, Toast.LENGTH_LONG).show();
			}

			@Override
			public void onResponse(Response resp) throws IOException {
				Log.d("ReportActivity", resp.body().string());
				progress.dismiss();
				
				Bundle bundle = new Bundle();
				bundle.putString("message", getString(R.string.report_thankyou));
				
				DialogFragment fragment = new ExitMessageFragment();
				fragment.setArguments(bundle);
				fragment.show(getSupportFragmentManager(), "thankyou");
			}
		
		});
	}
	
	protected String getEndpoint(){
		ApplicationInfo ai;
		try {
			ai = getPackageManager().getApplicationInfo(this.getPackageName(), PackageManager.GET_META_DATA);
		} catch (NameNotFoundException e) {
			Sentry.captureException(e);
			return "http://issue.whs.in.th/kusmartbus";
		}
	    Bundle bundle = ai.metaData;
	    return bundle.getString("issue_server");
	}
	
	protected int getSelectedType(){
		Spinner type = (Spinner) findViewById(R.id.spinner1);
		return reportTypesInt[type.getSelectedItemPosition()];
	}
	
	protected CharSequence getBody(){
		EditText text = (EditText) findViewById(R.id.editText1);
		return text.getText();
	}
	
	protected String getDescription(){
		StringBuilder out = new StringBuilder();
		out.append("A user has reported issue with KUSmartBus Android:\n\n----\n");
		out.append(getBody());
		out.append("\n----\n\n");
		
		Intent intent = getIntent();
		if(intent.hasExtra(REPORT_BUS)){
			out.append("'''Selected bus:''' ");
			out.append(intent.getStringExtra(REPORT_BUS));
			out.append("\n");
		}
		
		out.append("== Environment ==\n* '''App version:''' ");
		try {
			PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			out.append(pInfo.versionName);
			out.append(" (");
			out.append(pInfo.versionCode);
			out.append(")\n");
		} catch (NameNotFoundException e) {
			out.append("N/A\n");
		}
		out.append("* '''Device:''' ");
		out.append(Build.DEVICE);
		out.append("\n** '''Device name:''' ");
		out.append(Build.MODEL);
		out.append("\n** '''Device brand:''' ");
		out.append(Build.BRAND);
		out.append("\n* '''Android version:''' ");
		out.append(Build.VERSION.SDK_INT);
		out.append(" ");
		out.append(Build.VERSION.RELEASE);
		return out.toString();
	}
	
}
