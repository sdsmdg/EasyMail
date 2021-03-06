package com.example.android.easymail;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.android.easymail.adapters.EmailTilesAdapter;
import com.example.android.easymail.interactor.ResponseInteractorImpl;
import com.example.android.easymail.interfaces.CurrentDayMessageClickListener;
import com.example.android.easymail.interfaces.SenderNameInitialClickListener;
import com.example.android.easymail.models.CurrentDayMessageSendersList;
import com.example.android.easymail.models.CurrentDayMessageSendersRealmList;
import com.example.android.easymail.presenter.ResponsePresenterImpl;
import com.example.android.easymail.services.MessagesPullService;
import com.example.android.easymail.utils.Constants;
import com.example.android.easymail.view.ResponseActivityView;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationService;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import com.example.android.easymail.models.Message;

public class ResponseActivity extends AppCompatActivity implements
        SenderNameInitialClickListener, CurrentDayMessageClickListener, ResponseActivityView,
        NavigationView.OnNavigationItemSelectedListener {

    private Context context = this;
    private LinearLayout linearLayout;
    private EmailTilesAdapter emailTilesAdapter;
    private ResponsePresenterImpl responsePresenter;
    private ProgressDialog dialog;
    private DrawerLayout drawerLayout;
    private com.example.android.easymail.models.Message message;
    private NavigationView leftNavigationView, rightNavigationView;
    private AccountManager accountManager;
    private SharedPreferences preferences;
    private boolean isAutoDownloadAttachment;
    private static final int GET_ACCOUNTS_PERMISSION = 100;
    // Content provider authority
    public static final String AUTHORITY = "com.example.android.easymail.provider";
    // Account
    public static Account ACCOUNT = null;
    // Sync interval constants
    public static final long SECONDS_PER_MINUTE = 60L;
    public static final long SYNC_INTERVAL_IN_MINUTES = 60L;
    public static final long SYNC_INTERVAL =
            SYNC_INTERVAL_IN_MINUTES *
                    SECONDS_PER_MINUTE;
    // A content resolver for accessing the provider
    ContentResolver mResolver;
    public String token;
    public String lastSyncMessageId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_response);

        initViews();
        regListeners();
        getSavedPreferences();

        // ask for the dangerous permission of adding accounts
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
            // permission not granted, thus request again for the permissions
            ActivityCompat.requestPermissions(ResponseActivity.this,
                    new String[]{Manifest.permission.GET_ACCOUNTS},
                    GET_ACCOUNTS_PERMISSION);
        } else {
            // permission granted,
            // acquire the list of accounts from account manager
            final Account accountList[] = accountManager.getAccountsByType(Constants.ACCOUNT_TYPE);
            if (accountList.length == 0) {
                // no account is present, thus add a new account
                accountManager.addAccount(Constants.ACCOUNT_TYPE, Constants.AUTHTOKEN_TYPE_FULL_ACCESS, null
                        , null, this, null, null);
            } else {
                // first get the offline available messages
                responsePresenter.getOfflineMessages();
                // account is present, thus get the deserialized auth state
                // get the account from account manager in async state
                new RequestAccessToken(accountList[0]).execute();
            }
        }
        mResolver = getContentResolver();
        // mResolver.addPeriodicSync(ACCOUNT, AUTHORITY, Bundle.EMPTY, SYNC_INTERVAL);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch(requestCode){
            case GET_ACCOUNTS_PERMISSION:
                if(grantResults.length >0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    final Account accountList[] = accountManager.getAccountsByType(Constants.ACCOUNT_TYPE);
                }
                break;
        }
    }

    /**
     * initialise the views of the layout
     */
    private void initViews(){

        linearLayout = (LinearLayout) findViewById(R.id.layout);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        leftNavigationView = (NavigationView) findViewById(R.id.left_drawer);
        rightNavigationView = (NavigationView) findViewById(R.id.right_drawer);
        accountManager = AccountManager.get(this);
        responsePresenter = new ResponsePresenterImpl
                (this, new ResponseInteractorImpl(new Handler()), ResponseActivity.this, getApplication());

        //set the listeners for the left and right navigation views
        leftNavigationView.setNavigationItemSelectedListener(this);
        rightNavigationView.setNavigationItemSelectedListener(this);

        //restrict the swiping of the right drawer
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.END);

        //restrict the swiping of the right drawer on closing of left or right drawer
        drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
            }
            @Override
            public void onDrawerOpened(View drawerView) {
            }
            @Override
            public void onDrawerClosed(View drawerView) {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.END);
            }
            @Override
            public void onDrawerStateChanged(int newState) {
            }
        });
    }

    /**
     * method to register listeners
     */
    private void regListeners() {
    }

    @Override
    public void onSenderNameInitialClick(int day, int row, int column, int isExpanded) {

        ArrayList<Integer> ids = new ArrayList<>();
        int layoutId = Integer.parseInt("1" + Integer.toString(day) + Integer.toString(row));
        for (int m = 1; m <= 4; m++) {
            if (m != column)
                ids.add(Integer.parseInt("2" + Integer.toString(day) +Integer.toString(row) + Integer.toString(m)));
        }
        if (isExpanded == 1) {
            for (int id : ids) {
                RecyclerView recyclerView = (RecyclerView) findViewById(id);
                if (recyclerView != null)
                    recyclerView.setVisibility(View.VISIBLE);
            }
        } else {
            for (int id : ids) {
                RecyclerView recyclerView = (RecyclerView) findViewById(id);
                if (recyclerView != null)
                    recyclerView.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void showAutoSignedInDialog() {

        dialog = new ProgressDialog(ResponseActivity.this);
        dialog.setMessage("Auto Signing In!");
        dialog.show();
    }

    @Override
    public void showTokenRequestDialog() {

        dialog = new ProgressDialog(ResponseActivity.this);
        dialog.setMessage("Making Token Request!");
        dialog.show();
    }

    @Override
    public void showExchangeSucceddedToast() {
        Toast.makeText(context, "Token Request Completed!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showExchangeFailedToast() {
        Toast.makeText(context, "Token Request Failed!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showAuthorizationFailedToast() {
        Toast.makeText(context, "Authorization Request Failed!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void formRecyclerView(List<CurrentDayMessageSendersRealmList> list, int day, int i, int j, RecyclerView recyclerView) {

        emailTilesAdapter = new EmailTilesAdapter(this, this, this, list, day, i + 1, j + 1);
        recyclerView.setAdapter(emailTilesAdapter);
    }

    @Override
    public void addLinearLayoutToDisplay(LinearLayout currentLinearLayout) {

        linearLayout.addView(currentLinearLayout);
    }

    @Override
    public void showZeroMessagesReceivedToast() {
        Toast.makeText(ResponseActivity.this, "No Messages Received!", Toast.LENGTH_LONG).show();
    }

    @Override
    public void hideDialog() {
        dialog = new ProgressDialog(ResponseActivity.this);
        if (dialog.isShowing())
            dialog.hide();
    }

    @Override
    public void getCredential(String accessToken) {

        Intent serviceIntent = new Intent(ResponseActivity.this, MessagesPullService.class);
        serviceIntent.putExtra("token", accessToken);
        //startService(serviceIntent);
    }

    @Override
    public void appendLinearLayout(int linearLayoutId) {
        LinearLayout layout = (LinearLayout) findViewById(linearLayoutId);
        if (layout != null) {
            linearLayout.addView(layout);
        }
    }

    @Override
    public void onCurrentDayMessageClickListener(View v, com.example.android.easymail.models.Message child) {

        message = child;
        RealmConfiguration configuration = new RealmConfiguration.Builder().build();
        drawerLayout.openDrawer(GravityCompat.END);
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.END);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Make different intents for different click events
        Intent editMessageIntent = new Intent(ResponseActivity.this, EditMessageActivity.class);
        Intent customListMessagesIntent = new Intent(ResponseActivity.this, CustomListMessagesActivity.class);
        Intent mailClassifierIntent = new Intent(ResponseActivity.this, AllMessagesActivity.class);
        Intent searchByDateIntent = new Intent(ResponseActivity.this, SearchByDateActivity.class);
        Intent allAttachmentsIntent = new Intent(ResponseActivity.this, AllAttachmentsActivity.class);
        Intent settingsIntent =  new Intent(ResponseActivity.this, SettingsActivity.class);

        switch (item.getItemId()){
            // On click for left navigation view
            case R.id.left_nav_to_do:
                editMessageIntent.putExtra("listName", "To-Do");
                startActivity(customListMessagesIntent);
                break;
            case R.id.left_nav_follow_up:
                customListMessagesIntent.putExtra("listName", "Follow Up");
                startActivity(customListMessagesIntent);
                break;
            case R.id.left_nav_launch_events:
                customListMessagesIntent.putExtra("listName", "Launch Events");
                startActivity(customListMessagesIntent);
                break;
            case R.id.left_nav_business_events:
                customListMessagesIntent.putExtra("listName", "Business Events");
                startActivity(customListMessagesIntent);
                break;
            case R.id.left_nav_mail_classifier:
                mailClassifierIntent.putExtra("token", token);
                startActivity(mailClassifierIntent);
                break;
            case R.id.left_nav_search_by_date:
                searchByDateIntent.putExtra("token", token);
                startActivity(searchByDateIntent);
                break;
            case R.id.left_nav_attachments:
                startActivity(allAttachmentsIntent);
                break;
            case R.id.left_nav_settings:
                startActivity(settingsIntent);
                break;
            // On click for right navigation view
            case R.id.right_nav_to_do:
                editMessageIntent.putExtra("listName", "To-Do");
                editMessageIntent.putExtra("messageId", message.getId());
                startActivity(editMessageIntent);
                break;
            case R.id.right_nav_follow_up:
                editMessageIntent.putExtra("listName", "Follow Up");
                editMessageIntent.putExtra("messageId", message.getId());
                startActivity(editMessageIntent);
                break;
            case R.id.right_nav_launch_events:
                editMessageIntent.putExtra("listName", "Launch Events");
                editMessageIntent.putExtra("messageId", message.getId());
                startActivity(editMessageIntent);
                break;
            case R.id.right_nav_business_events:
                editMessageIntent.putExtra("listName", "Business Events");
                editMessageIntent.putExtra("messageId", message.getId());
                startActivity(editMessageIntent);
                break;
        }
        return true;
    }

    public class RequestAccessToken extends AsyncTask<Void, Bundle, Bundle> {

        private Account account;

        public RequestAccessToken(Account account) {
            this.account = account;
        }

        @Override
        protected Bundle doInBackground(Void... params) {
            final AccountManagerFuture<Bundle> future = accountManager.getAuthToken
                    (account, Constants.AUTHTOKEN_TYPE_FULL_ACCESS, null, ResponseActivity.this, null, null);
            Bundle bnd = null;
            try {
                bnd = future.getResult();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return bnd;
        }

        @Override
        protected void onPostExecute(Bundle bundle) {
            super.onPostExecute(bundle);
            performTokenRequest(bundle);
        }
    }

    /**
     * To get new messages using fresh access token
     * @param bundle Used to obtain deserialized auth state
     */
    private void performTokenRequest(Bundle bundle) {
        try {
            final String deserializedAuthState = bundle.getString(AccountManager.KEY_AUTHTOKEN);
            AuthState state = AuthState.jsonDeserialize(deserializedAuthState);
            AuthorizationService service = new AuthorizationService(context);
            // obtain the fresh access token from the auth state
            state.performActionWithFreshTokens(service, new AuthState.AuthStateAction() {
                @Override
                public void execute(@Nullable String accessToken, @Nullable String idToken, @Nullable AuthorizationException ex) {
                    if (ex == null) {
                        token = accessToken;
                        responsePresenter.performTokenRequest(null, token);
                    } else {
                        Log.e("auth token exception", ex.toString());
                    }
                }
            });
            Log.d("easymail", "GetToken Bundle is ");
        }catch (Exception e){
            e.printStackTrace();
            accountManager.removeAccount(ACCOUNT, this, null, null);
            accountManager.addAccount(Constants.ACCOUNT_TYPE, Constants.AUTHTOKEN_TYPE_FULL_ACCESS, null
                    , null, ResponseActivity.this, null, null);
        }
    }

    /**
     * get saved preferences including lastSyncMessageId which gives the id of the
     * last synchronized message
     */
    public void getSavedPreferences() {
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        isAutoDownloadAttachment = preferences.getBoolean("auto_download_attachment", false);
        lastSyncMessageId = preferences.getString("last_sync_message_id", null);
    }
}
