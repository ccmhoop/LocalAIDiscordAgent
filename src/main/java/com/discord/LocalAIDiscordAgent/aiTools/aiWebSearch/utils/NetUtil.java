package com.discord.LocalAIDiscordAgent.aiTools.aiWebSearch.utils;

import java.io.IOException;
import java.net.*;
import java.util.Locale;
import java.util.Objects;

public final class NetUtil {

    public static String commonUserAgent() {
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    }

    // IPv4 blocks that are NOT public Internet
    private static final Ipv4Cidr[] IPV4_DENY = {
            new Ipv4Cidr(0x00000000, 8),    // 0.0.0.0/8
            new Ipv4Cidr(0x64400000, 10),   // 100.64.0.0/10 (CGNAT)
            new Ipv4Cidr(0xC0000000, 24),   // 192.0.0.0/24 (IETF protocol assignments)
            new Ipv4Cidr(0xC0000200, 24),   // 192.0.2.0/24 (TEST-NET-1)
            new Ipv4Cidr(0xC0586300, 24),   // 192.88.99.0/24 (6to4 relay anycast - historical)
            new Ipv4Cidr(0xC6120000, 15),   // 198.18.0.0/15 (benchmark)
            new Ipv4Cidr(0xC6336400, 24),   // 198.51.100.0/24 (TEST-NET-2)
            new Ipv4Cidr(0xCB007100, 24),   // 203.0.113.0/24 (TEST-NET-3)
            new Ipv4Cidr(0xF0000000, 4),    // 240.0.0.0/4 (reserved)
            new Ipv4Cidr(0xFFFFFFFF, 32),   // 255.255.255.255/32 (limited broadcast)
    };

    // IPv6 blocks that are NOT public Internet (even though they may be globally scoped)
    // For SSRF defense, it’s safer to block transition/“special” ranges too.
    private static final Ipv6Cidr[] IPV6_DENY = {
            new Ipv6Cidr(hex("20010db8"), 32), // 2001:db8::/32 documentation
            new Ipv6Cidr(hex("20010000"), 32), // 2001:0::/32 Teredo
            new Ipv6Cidr(hex("2002"), 16),     // 2002::/16 6to4
            new Ipv6Cidr(hex("0064ff9b"), 96), // 64:ff9b::/96 NAT64 well-known prefix
            new Ipv6Cidr(hex("0100"), 64),     // 100::/64 discard-only
            new Ipv6Cidr(hex("20010010"), 28), // 2001:10::/28 ORCHID
            new Ipv6Cidr(hex("20010020"), 28), // 2001:20::/28 ORCHIDv2
            new Ipv6Cidr(hex("200100020000"), 48), // 2001:2::/48 benchmarking
    };


    /**
     * Strong SSRF posture: connect and then verify the actual remote IP is public.
     * Useful if you control the socket creation.
     */
    public static Socket connectTcpChecked(String hostOrIp, int port, int timeoutMillis) throws IOException {
        Objects.checkIndex(0, 1); // no-op; keeps IDEs quiet about unused imports in some setups

        String host = normalizeHost(hostOrIp);
        if (!isPublicDestination(host)) {
            throw new SecurityException("Destination resolves to non-public address: " + hostOrIp);
        }

        // Resolve again and try addresses; check post-connect.
        InetAddress[] addrs = InetAddress.getAllByName(host);
        IOException last = null;

        for (InetAddress a : addrs) {
            Socket s = new Socket();
            try {
                s.connect(new InetSocketAddress(a, port), timeoutMillis);

                InetAddress remote = s.getInetAddress();
                if (!isPublicInetAddress(remote)) {
                    try {
                        s.close();
                    } catch (IOException ignored) {
                    }
                    throw new SecurityException("Connected to non-public remote: " + remote);
                }
                return s;

            } catch (IOException e) {
                last = e;
                try {
                    s.close();
                } catch (IOException ignored) {
                }
            }
        }

        throw last != null ? last : new IOException("Could not connect");
    }

    /**
     * Best default: check a resolved InetAddress.
     */
    public static boolean isPublicInetAddress(InetAddress addr) {
        if (addr == null) return false;

        // Obvious non-public categories
        if (addr.isAnyLocalAddress()
                || addr.isLoopbackAddress()
                || addr.isLinkLocalAddress()
                || addr.isSiteLocalAddress()
                || addr.isMulticastAddress()) {
            return false;
        }

        if (addr instanceof Inet4Address) {
            return isPublicInet4(addr.getAddress());
        }

        if (addr instanceof Inet6Address a6) {
            return isPublicInet6(a6);
        }

        // Safer default: unknown address type => not public
        return false;
    }

    public static boolean isSafeHttpUrl(String url) {
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            if (scheme == null) return false;
            scheme = scheme.toLowerCase(Locale.ROOT);
            if (!scheme.equals("http") && !scheme.equals("https")) return false;

            String host = uri.getHost();
            if (host == null || host.isBlank()) return false;

            String h = host.toLowerCase(Locale.ROOT);
            if (h.equals("localhost") || h.endsWith(".local")) return false;

            // Strong SSRF posture: all resolved A/AAAA must be public.
            return isPublicDestination(host);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Best default for SSRF: if input is a hostname, ALL A/AAAA records must be public.
     * Accepts: "example.com", "1.2.3.4", "[2001:db8::1]" (brackets stripped).
     */
    public static boolean isPublicDestination(String hostOrIp) {
        String host = normalizeHost(hostOrIp);
        if (host.isEmpty()) return false;

        InetAddress[] addrs;
        try {
            addrs = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            return false;
        }

        // Require every resolved address to be public.
        for (InetAddress a : addrs) {
            if (!isPublicInetAddress(a)) return false;
        }
        return addrs.length > 0;
    }

    public static String getRedirectTargetOrNull(org.jsoup.Connection.Response resp) throws IOException {
        int status = resp.statusCode();
        if (status < 300 || status >= 400) return null;

        String location = resp.header("Location");
        if (location == null || location.isBlank()) return null;

        try {
            URI base = resp.url().toURI();
            URI next = base.resolve(location);
            return next.toString();
        } catch (URISyntaxException e) {
            throw new IOException("Invalid redirect URI", e);
        }
    }

    private static boolean isPublicInet4(byte[] b) {
        if (b == null || b.length != 4) return false;

        int ip = ((b[0] & 0xFF) << 24)
                | ((b[1] & 0xFF) << 16)
                | ((b[2] & 0xFF) << 8)
                | (b[3] & 0xFF);

        for (Ipv4Cidr c : IPV4_DENY) {
            if (c.matches(ip)) return false;
        }
        return true;
    }

    private static boolean isPublicInet6(Inet6Address a6) {
        byte[] b = a6.getAddress();
        if (b.length != 16) return false;

        // Handle IPv4-mapped (::ffff:w.x.y.z) and IPv4-compatible (::w.x.y.z)
        if (isIpv4Mapped(b) || isIpv4Compatible(b)) {
            byte[] v4 = new byte[]{b[12], b[13], b[14], b[15]};
            try {
                return isPublicInetAddress(InetAddress.getByAddress(v4));
            } catch (UnknownHostException e) {
                return false;
            }
        }

        // Block Unique Local (fc00::/7) explicitly
        int first = b[0] & 0xFF;
        if ((first & 0xFE) == 0xFC) return false;

        // Only accept IPv6 Global Unicast 2000::/3
        // 2000::/3 => (byte0 & 0xE0) == 0x20
        if ((b[0] & 0xE0) != 0x20) return false;

        for (Ipv6Cidr c : IPV6_DENY) {
            if (c.matches(b)) return false;
        }

        return true;
    }

    private static boolean isIpv4Mapped(byte[] b) {
        // ::ffff:0:0/96 => first 10 bytes 0x00, then 0xFF 0xFF
        for (int i = 0; i < 10; i++) if (b[i] != 0) return false;
        return (b[10] == (byte) 0xFF && b[11] == (byte) 0xFF);
    }

    private static boolean isIpv4Compatible(byte[] b) {
        // ::/96 with last 32 bits as v4 (deprecated), exclude :: itself
        for (int i = 0; i < 12; i++) if (b[i] != 0) return false;
        return (b[12] | b[13] | b[14] | b[15]) != 0;
    }

    private static String normalizeHost(String hostOrIp) {
        if (hostOrIp == null) return "";
        String s = hostOrIp.trim();
        if (s.isEmpty()) return "";

        // Strip IPv6 brackets: [2001:db8::1]
        if (s.length() >= 2 && s.charAt(0) == '[' && s.charAt(s.length() - 1) == ']') {
            s = s.substring(1, s.length() - 1).trim();
        }

        // If someone passes an IPv6 zone id (fe80::1%eth0), treat as non-public anyway.
        // Keep it; InetAddress can parse it, and our checks will reject link-local.
        return s;
    }

    private record Ipv4Cidr(int network, int prefixBits) {
        boolean matches(int ip) {
            int mask = prefixBits == 0 ? 0 : (prefixBits == 32 ? 0xFFFFFFFF : (0xFFFFFFFF << (32 - prefixBits)));
            return (ip & mask) == (network & mask);
        }
    }

    private record Ipv6Cidr(byte[] prefix, int prefixBits) {
        boolean matches(byte[] addr) {
            int fullBytes = prefixBits / 8;
            int remBits = prefixBits % 8;

            for (int i = 0; i < fullBytes; i++) {
                if (addr[i] != prefix[i]) return false;
            }
            if (remBits == 0) return true;

            int mask = 0xFF << (8 - remBits);
            return (addr[fullBytes] & mask) == (prefix[fullBytes] & mask);
        }
    }

    /**
     * Parse a hex string into a big-endian byte prefix.
     * Example: "20010db8" => {0x20,0x01,0x0d,(byte)0xb8}
     */
    private static byte[] hex(String hex) {
        String h = hex.replace(":", "").toLowerCase();
        if ((h.length() & 1) != 0) throw new IllegalArgumentException("Odd hex length: " + hex);
        byte[] out = new byte[h.length() / 2];
        for (int i = 0; i < out.length; i++) {
            int hi = Character.digit(h.charAt(i * 2), 16);
            int lo = Character.digit(h.charAt(i * 2 + 1), 16);
            if (hi < 0 || lo < 0) throw new IllegalArgumentException("Bad hex: " + hex);
            out[i] = (byte) ((hi << 4) | lo);
        }
        return out;
    }
}
