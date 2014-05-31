package ch.filecloud.betoo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;


import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

/**
 * Created by domi on 5/30/14.
 */
public class Betoo implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    private Object mPhone;

    private static Context mContext;
    private static Class<?> mPhoneFactoryClass;

    private static int NETWORK_MODE_WCDMA_PREF = 0;
    private static int NETWORK_MODE_GSM_ONLY = 1;

    public static final String ACTION_CHANGE_NETWORK_TYPE = "betoo.intent.action.CHANGE_NETWORK_TYPE";

    enum NetworkType {
        wifi, gsm
    }

    private NetworkType currentNetworkTyp;

    private static BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_CHANGE_NETWORK_TYPE)) {

                XposedBridge.log("[Betoo] received ACTION_CHANGE_NETWORK_TYPE broadcast!");
                setPreferredNetworkType();
            }
        }
    };

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {

        mPhoneFactoryClass = XposedHelpers.findClass("com.android.internal.telephony.PhoneFactory", null);
        XposedBridge.log("[Betoo] PhoneFactory class is: " + mPhoneFactoryClass);

        XposedHelpers.findAndHookMethod(mPhoneFactoryClass, "makeDefaultPhone",
                Context.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                        mContext = (Context) param.args[0];

                        XposedBridge.log("[Betoo] Initialized phone wrapper. Context is: " + mContext);

                        onInitialize();
                    }
                }
        );

    }

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        if (!lpparam.packageName.contains("android.")) {
            return;
        }
        findAndHookMethod("android.net.wifi.WifiStateMachine", lpparam.classLoader, "setNetworkDetailedState", "android.net.NetworkInfo.DetailedState", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        // do nothing
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {


                        NetworkInfo.DetailedState state = (NetworkInfo.DetailedState) param.args[0];

                        if (state.equals(NetworkInfo.DetailedState.CONNECTED)) {
                            XposedBridge.log("[Betoo] WIFI connected!");
                            Context context = (Context) getObjectField(param.thisObject, "mContext");

                            Intent i = new Intent(Betoo.ACTION_CHANGE_NETWORK_TYPE);
                            context.sendBroadcast(i);
                        }

                    }
                }
        );
    }

    private static void onInitialize() {
        if (mContext != null) {
            // TODO add currentNetworkType to intent
            IntentFilter intentFilter = new IntentFilter(ACTION_CHANGE_NETWORK_TYPE);
            mContext.registerReceiver(mBroadcastReceiver, intentFilter);
        }
    }

    private static void setPreferredNetworkType() {
        Object defPhone = XposedHelpers.callStaticMethod(mPhoneFactoryClass, "getDefaultPhone");
        if (defPhone == null) {
            XposedBridge.log("[Betoo] default phone was null!");
            return;
        }

        try {
            Class<?>[] paramArgs = new Class<?>[2];
            paramArgs[0] = int.class;
            paramArgs[1] = Message.class;
            XposedHelpers.callMethod(defPhone, "setPreferredNetworkType", paramArgs, NETWORK_MODE_GSM_ONLY, null);

        } catch (Throwable t) {
            XposedBridge.log("[Betoo] setPreferredNetworkType failed: " + t.getMessage());
            XposedBridge.log(t);
        }
    }


}
