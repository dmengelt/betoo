package ch.filecloud.betoo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.os.Message;
import android.telephony.TelephonyManager;

import java.text.SimpleDateFormat;
import java.util.Date;

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

    private static Context mContext;
    private static Class<?> mPhoneFactoryClass;
    private static Class<?> mSystemProperties;

    private static int NETWORK_MODE_WCDMA_PREF = 0;
    private static int NETWORK_MODE_GSM_ONLY = 1;
    private static int NETWORK_MODE_LTE_GSM_WCDMA  = 9; // works on galaxy s5

    public static final String ACTION_CHANGE_NETWORK_TYPE = "betoo.intent.action.CHANGE_NETWORK_TYPE";
    public static final String EXTRA_NETWORK_TYPE = "networkType";

    private static int defaultNetworkType = -1;
    private static int currentNetworkType = -1;

    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    private static BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_CHANGE_NETWORK_TYPE)) {

                int networkType = intent.getIntExtra(EXTRA_NETWORK_TYPE, -1);

                if(networkType == -1) {
                    if(currentNetworkType != defaultNetworkType) {
                        Betoo.log("Changing preferred network back to default network type: " + defaultNetworkType);
                        setPreferredNetworkType(defaultNetworkType);
                    }

                } else {
                    if(networkType != currentNetworkType) {
                        Betoo.log("Setting preferred network type to 2G/GSM only! network type: " + networkType);
                        setPreferredNetworkType(networkType);
                    }
                }
            }
        }
    };

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {

        mPhoneFactoryClass = XposedHelpers.findClass("com.android.internal.telephony.PhoneFactory", null);
        mSystemProperties = XposedHelpers.findClass("android.os.SystemProperties", null);

        defaultNetworkType = getDefaultNetworkType();
        Betoo.log("Setting default network type to: " + defaultNetworkType);

        XposedHelpers.findAndHookMethod(mPhoneFactoryClass, "makeDefaultPhone",
                Context.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                        mContext = (Context) param.args[0];
                        Betoo.log("Initialized phone wrapper (makeDefaultPhone). Context is: " + mContext);
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
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        NetworkInfo.DetailedState state = (NetworkInfo.DetailedState) param.args[0];
                        Context context = (Context) getObjectField(param.thisObject, "mContext");
                        Intent i = new Intent(Betoo.ACTION_CHANGE_NETWORK_TYPE);

                        if (state.equals(NetworkInfo.DetailedState.CONNECTED)) {
                            i.putExtra(Betoo.EXTRA_NETWORK_TYPE, NETWORK_MODE_GSM_ONLY);

                        }
                        context.sendBroadcast(i);
                    }
                }
        );
    }

    private static void onInitialize() {
        if (mContext != null) {
            TelephonyManager teleMan = (TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);
            Betoo.log("TelephonyManager network type is: " + teleMan.getNetworkType());

            IntentFilter intentFilter = new IntentFilter(ACTION_CHANGE_NETWORK_TYPE);
            mContext.registerReceiver(mBroadcastReceiver, intentFilter);
        }
    }

    private static void setPreferredNetworkType(int networkType) {
        currentNetworkType = networkType;
        Object defPhone = XposedHelpers.callStaticMethod(mPhoneFactoryClass, "getDefaultPhone");
        if (defPhone == null) {
            Betoo.log("Default phone was null! Unable to set preferred network type");
            return;
        }

        try {
            Class<?>[] paramArgs = new Class<?>[2];
            paramArgs[0] = int.class;
            paramArgs[1] = Message.class;
            XposedHelpers.callMethod(defPhone, "setPreferredNetworkType", paramArgs, networkType, null);
            Betoo.log("Successfully changed preferred network type to: " + networkType);

        } catch (Throwable t) {
            Betoo.log("Unable to set preferred network type: " + t.getMessage());
        }
    }

    public static int getDefaultNetworkType() {
        try {
            int mode = (Integer) XposedHelpers.callStaticMethod(mSystemProperties, "getInt", "ro.telephony.default_network", -1);

            if(mode == -1) {
                Betoo.log("Failed to detect default network type! Using: NETWORK_MODE_LTE_GSM_WCDMA");
                //mode = NETWORK_MODE_WCDMA_PREF;
                mode = NETWORK_MODE_LTE_GSM_WCDMA;
            }

            currentNetworkType = mode;

            return mode;
        } catch (Throwable t) {
            Betoo.log(t.getMessage());
            return NETWORK_MODE_WCDMA_PREF;
        }
    }

    private static void log(String message) {
        XposedBridge.log(sdf.format(new Date()) + " [Betoo] " + message);
    }
}
