package cs451;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public class Host {

    private static final String IP_START_REGEX = "/";

    private short id;
    private String ip;
    private int port = -1;

    private InetSocketAddress hostsSocket;

    public boolean populate(String idString, String ipString, String portString) {
        try {
            id = Short.parseShort(idString);

            String ipTest = InetAddress.getByName(ipString).toString();
            if (ipTest.startsWith(IP_START_REGEX)) {
                ip = ipTest.substring(1);
            } else {
                ip = InetAddress.getByName(ipTest.split(IP_START_REGEX)[0]).getHostAddress();
            }

            port = Integer.parseInt(portString);
            if (port <= 0) {
                System.err.println("Port in the hosts file must be a positive number!");
                return false;
            }
        } catch (NumberFormatException e) {
            if (port == -1) {
                System.err.println("Id in the hosts file must be a number!");
            } else {
                System.err.println("Port in the hosts file must be a number!");
            }
            return false;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        this.hostsSocket = new InetSocketAddress(getInetAddress(), getPort());

        return true;
    }

    public short getId() {
        return id;
    }

    public String getIp() {
        return ip;
    }

    private InetAddress getInetAddress() {
        try {
            InetAddress addr = InetAddress.getByName(this.ip);
            return addr;
        } catch (UnknownHostException e) {
            // error should be detected earlier, should not happen
            e.printStackTrace();
            System.err.println("Unexpectedly unable to parse the IP address");
        }
        return null;
    }

    public InetSocketAddress getHostsSocket() {
        return hostsSocket;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return "Host [id=" + id + ", ip=" + ip + ", port=" + port + "]";
    }

}
