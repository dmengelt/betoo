package ch.filecloud.betoo;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by domi on 6/4/14.
 */
public class NetworkType {

    private int key;
    private String name;

    public static final List<NetworkType> types = new LinkedList<NetworkType>();

    private NetworkType(int key, String name) {
        this.key = key;
        this.name = name;
    }

    public Integer getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public String setValue(String name) {
        this.name = name;
        return name;
    }

    // factory
    public static NetworkType create(int key, String value) {
        return new NetworkType(key, value);
    }

    public static String get(int networkMode) {
        if(types == null || types.isEmpty()) {
            setup();
        }

        return types.get(networkMode).getName();
    }

    private static void setup() {
        types.add(NetworkType.create(0, "GSM/WCDMA (WCDMA preferred)"));
        types.add(NetworkType.create(1, "GSM/2G only"));
        types.add(NetworkType.create(2, "WCDMA only"));
        types.add(NetworkType.create(3, "GSM/WCDMA (auto mode, according to PRL)"));
        types.add(NetworkType.create(4, "CDMA and EvDo (auto mode, according to PRL)"));
        types.add(NetworkType.create(5, "CDMA only"));
        types.add(NetworkType.create(6, "EvDo only"));
        types.add(NetworkType.create(7, "GSM/WCDMA, CDMA, and EvDo (auto mode, according to PRL)"));
        types.add(NetworkType.create(8, "LTE, CDMA and EvDo "));
        types.add(NetworkType.create(9, "LTE, GSM/WCDMA"));
        types.add(NetworkType.create(10, "LTE, GSM/WCDMA"));
        types.add(NetworkType.create(11, "LTE only"));
        types.add(NetworkType.create(12, "LTE/WCDMA"));
    }
}
