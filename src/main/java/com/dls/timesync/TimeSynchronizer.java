package com.dls.timesync;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

public class TimeSynchronizer {

    private static final String NTP_SERVER = "pool.ntp.org"; // You can later configure this
    private static final int TIMEOUT_MS = 3000; // Timeout for NTP request

    /**
     * Fetches the current time from an NTP server.
     *
     * @return Instant representing the synchronized time, or local time if NTP fails.
     */
    public static Instant getSynchronizedTime() {
        NTPUDPClient client = new NTPUDPClient();
        client.setDefaultTimeout(TIMEOUT_MS);

        try {
            InetAddress hostAddr = InetAddress.getByName(NTP_SERVER);
            TimeInfo info = client.getTime(hostAddr);
            info.computeDetails();
            long offset = info.getOffset(); // Time offset between local clock and NTP clock

            return Instant.now().plusMillis(offset);
        } catch (Exception e) {
            System.err.println("[TimeSynchronizer] Failed to fetch NTP time. Falling back to local time. Reason: " + e.getMessage());
            return Instant.now();
        } finally {
            client.close();
        }
    }

    /**
     * Measures clock offset (how much local clock deviates from NTP).
     *
     * @return offset in milliseconds (positive if local clock is behind, negative if ahead)
     */
    public static long getClockOffset() {
        NTPUDPClient client = new NTPUDPClient();
        client.setDefaultTimeout(TIMEOUT_MS);

        try {
            InetAddress hostAddr = InetAddress.getByName(NTP_SERVER);
            TimeInfo info = client.getTime(hostAddr);
            info.computeDetails();
            Long offset = info.getOffset();
            return (offset != null) ? offset : 0L;
        } catch (Exception e) {
            System.err.println("[TimeSynchronizer] Failed to measure clock offset. Reason: " + e.getMessage());
            return 0L;
        } finally {
            client.close();
        }
    }
}