package com.example.android.easymail;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.example.android.easymail.utils.Constants;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.api.services.gmail.GmailScopes;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.ResponseTypeValues;
import net.openid.appauth.TokenResponse;

import org.json.JSONException;


public class MainActivity extends AccountAuthenticatorActivity implements GoogleApiClient.OnConnectionFailedListener {

    public final static String ARG_ACCOUNT_TYPE = "ACCOUNT_TYPE";
    public final static String ARG_AUTH_TYPE = "AUTH_TYPE";
    public final static String ARG_ACCOUNT_NAME = "ACCOUNT_NAME";
    public final static String ARG_IS_ADDING_NEW_ACCOUNT = "IS_ADDING_ACCOUNT";
    public static final String KEY_ERROR_MESSAGE = "ERR_MSG";
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String mAuthEndPoint = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String mTokenEndPoint = "https://www.googleapis.com/oauth2/v4/token";
    private static final String clientId = "449830779939-3bedim4r2ospjnbvuieofa7kv2513l3t.apps.googleusercontent.com";
    private static final String reverseDomainName = "com.example.android.easymail:/oauth2redirect";
    private static final String scope = "https://www.googleapis.com/auth/gmail.readonly";
    private static final int REQUEST_SIGN_IN = 1001;
    private static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    public static final String[] scopes = {GmailScopes.GMAIL_READONLY};
    Context context = this;
    SignInButton loginButton;
    TextView outputText;
    private SharedPreferences sharedPref;
    GoogleApiClient googleApiClient;
    AccountManager accountManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPref =getPreferences(Context.MODE_PRIVATE);
        Scope scope = new Scope("https://www.googleapis.com/auth/gmail.readonly");

        initViews();
        regListeners();
        checkRequiredAvailability();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(scope)
                .build();

/*
        googleApiClient = new GoogleApiClient.Builder(MainActivity.this)
                .enableAutoManage(new FragmentActivity(), this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
*/
        accountManager = AccountManager.get(this);
        final AuthorizationResponse response = AuthorizationResponse.fromIntent(getIntent());
        final AuthorizationException exception = AuthorizationException.fromIntent(getIntent());

        AuthorizationService service = new AuthorizationService(context);

        if (response != null) {
            service.performTokenRequest(
                    response.createTokenExchangeRequest(),
                    new AuthorizationService.TokenResponseCallback() {
                        @Override
                        public void onTokenRequestCompleted(TokenResponse resp, AuthorizationException ex) {
                            if (resp != null) {
                                //exchange succeeded
                                final String accountType = Constants.ACCOUNT_TYPE;
                                AuthState state = new AuthState(response, resp, ex);
                                String deserializedAuthState = state.jsonSerializeString();
                                String userName = "me";
                                Bundle data = new Bundle();
                                data.putString(AccountManager.KEY_ACCOUNT_NAME, userName);
                                data.putString(AccountManager.KEY_ACCOUNT_TYPE, accountType);
                                data.putString(AccountManager.KEY_AUTHTOKEN, deserializedAuthState);
                                final Intent res = new Intent();
                                res.putExtras(data);
                                final Account account = new Account(userName, res.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE));
                                accountManager.addAccountExplicitly(account, "password", null);
                                accountManager.setAuthToken(account, Constants.AUTHTOKEN_TYPE_FULL_ACCESS, deserializedAuthState);
                                setAccountAuthenticatorResult(res.getExtras());
                                setResult(RESULT_OK, res);
                                Intent i = new Intent(MainActivity.this, ResponseActivity.class);
                                startActivity(i);
                                finish();
                            }
                        }
                    });
        }

    /*
        OptionalPendingResult<GoogleSignInResult> pendingResult =
                Auth.GoogleSignInApi.silentSignIn(googleApiClient);
        if (pendingResult.isDone()) {
            Intent responseActivityIntent = new Intent(MainActivity.this, ResponseActivity.class);
            responseActivityIntent.putExtra("is_auto_signed_in", "yes");
            startActivity(responseActivityIntent);
        } else{
            final ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setMessage("Auto Signing In...");
            progressDialog.show();
            pendingResult.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                @Override
                public void onResult(@NonNull GoogleSignInResult googleSignInResult) {
                    if ( googleSignInResult.isSuccess() ) {
                        Intent responseActivityIntent = new Intent(MainActivity.this, ResponseActivity.class);
                        responseActivityIntent.putExtra("is_auto_signed_in", "yes");
                        startActivity(responseActivityIntent);
                    }
                    progressDialog.dismiss();
                }
            });
        }
     */
    }

    private void regListeners() {
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                configurationDiscoveryAndRequest();
            }
        });
    }

    private void initViews() {
        loginButton = (SignInButton)findViewById(R.id.login_button);
        loginButton.setSize(SignInButton.SIZE_STANDARD);
        outputText = (TextView) findViewById(R.id.output);
    }

    private void signIn(){
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
        startActivityForResult(signInIntent, REQUEST_SIGN_IN);
    }

    private void configurationDiscoveryAndRequest(){
        Uri mAuthEndPointUri = Uri.parse(mAuthEndPoint);
        Uri mTokenEndPointURi = Uri.parse(mTokenEndPoint);
        Uri redirectUri = Uri.parse(reverseDomainName);

        AuthorizationServiceConfiguration config = new AuthorizationServiceConfiguration(mAuthEndPointUri,mTokenEndPointURi,null);

        AuthorizationRequest req = new AuthorizationRequest.Builder(
                config,
                clientId,
                ResponseTypeValues.CODE,
                redirectUri
        ).setScope(scope).build();

        AuthorizationService service = new AuthorizationService(context);
        Intent postAuthIntent = new Intent(MainActivity.this, MainActivity.class);
        postAuthIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Intent authCanceledIntent = new Intent(MainActivity.this, MainActivity.class);
        authCanceledIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        authCanceledIntent.putExtra("test","Cancel");
        service.performAuthorizationRequest(
                req,
                PendingIntent.getActivity(MainActivity.this, req.hashCode(), postAuthIntent, 0),
                PendingIntent.getActivity(MainActivity.this, req.hashCode(), authCanceledIntent, 0)
        );
    }

    //  Check the desired conditions of internet and google play services
    private void checkRequiredAvailability() {
        if (! isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (! isDeviceOnline()) {
            CoordinatorLayout coordinatorLayout = new CoordinatorLayout(MainActivity.this);
            Snackbar snackbar = Snackbar.make(coordinatorLayout, "No network connection available.", Snackbar.LENGTH_LONG);
            snackbar.show();
        }
    }

    // Check whether the device is connected to the internet
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    // Check that Google Play services APK is installed and up to date.
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    // Method to acquire google play services
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }

    // Display an error dialog showing that Google Play Services is missing or out of date.
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                MainActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == REQUEST_SIGN_IN){
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
        if(requestCode == REQUEST_GOOGLE_PLAY_SERVICES){
            if (resultCode != RESULT_OK) {
                outputText.setText(
                        context.getResources().getString(R.string.RequestGooglePlayServices));
            }
        }
    }

    private void handleSignInResult(GoogleSignInResult result) {

        if (result.isSuccess()){
            Intent responseActivityIntent =new Intent(MainActivity.this, ResponseActivity.class);
            responseActivityIntent.putExtra("is_auto_signed_in", "yes");
            startActivity(responseActivityIntent);
        }
    }

    public AuthState readAuthState() {

        SharedPreferences authPrefs = getSharedPreferences(context.getResources().getString(R.string.AuthSharedPref), MODE_PRIVATE);
        String stateJson = authPrefs.getString(context.getResources().getString(R.string.StateJson), null);

        if (stateJson != null) {
            try {
                return AuthState.jsonDeserialize(stateJson);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            return new AuthState();
        }
        return null;
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
