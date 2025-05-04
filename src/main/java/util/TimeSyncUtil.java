package util;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import java.net.InetAddress;

public class TimeSyncUtil {
    private static final String NTP_SERVER = "pool.ntp.org";
    private static long offsetMillis = 0;

    public static void syncNtpOffset() {
        try {
            NTPUDPClient client = new NTPUDPClient();
            client.setDefaultTimeout(1000);
            InetAddress hostAddress = InetAddress.getByName(NTP_SERVER);
            TimeInfo info = client.getTime(hostAddress);
            info.computeDetails();
            Long offset = info.getOffset();
            if (offset != null) {
                offsetMillis = offset;
                System.out.println("[NTP] Offset synced: " + offset + " ms");
            }
        } catch (Exception e) {
            System.out.println("[NTP] Failed to sync offset: " + e.getMessage());
        }
    }

    public static long getNtpAdjustedTime() {
        return System.currentTimeMillis() + offsetMillis;
    }
}
