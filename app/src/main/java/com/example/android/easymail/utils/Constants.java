package com.example.android.easymail.utils;

/**
 * Created by Harshit Bansal on 6/21/2017.
 */

public class Constants {

    public static final String BROADCAST_ACTION =
            "com.example.android.thread.BROADCAST";

    public static final String EXTENDED_DATA_STATUS =
            "com.example.android.thread.STATUS";
    /**
     * Account type id
     */
    public static final String ACCOUNT_TYPE = "com.example.android.easymail.account";

    /**
     * Account name
     */
    public static final String ACCOUNT_NAME = "Easy Mail";

    /**
     * Auth token type
     */
    public static final String AUTHTOKEN_TYPE_FULL_ACCESS = "Full access";

    /**
     * Dismiss action type
     */
    public static final String ACTION_DISMISS = "action_dismiss";

    /**
     * Snooze action type
     */
    public static final String ACTION_SNOOZE = "action_snooze";

    /**
     * Sender information url
     */
    public static final String SENDER_INFO_URL = "http://picasaweb.google.com/data/entry/api/user/<hereYourUserIdOrYourEmail>?alt=json";

    /**
     * Retrieve original information url of sender
     * @param email The email of the sender
     * @return The original url
     */
    public static String getSenderInfoUrl(String email){
        return SENDER_INFO_URL.replace("<hereYourUserIdOrYourEmail>", email);
    }
}
