package cs451;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class ConfigParser {

    private String path;

    public boolean populate(String value) {
        File file = new File(value);
        path = file.getPath();
        return true;
    }

    public String getPath() {
        return path;
    }

    /**
     * Returns the perfect link config object associated with the given found config
     * path, or null if the config cannot be parsed.
     * 
     * @return
     */
    public PerfectLinkConfig getPerfectLinkConfig() {
        try {
            BufferedReader myReader = new BufferedReader(new FileReader(path));
            String data = myReader.readLine();
            myReader.close();
            if (data != null) {
                String[] parameters = data.split(" ");
                int nbMessages = Integer.parseInt(parameters[0]);
                int parsed_id = Integer.parseInt(parameters[1]);
                if (parsed_id >= 0 && parsed_id <= Byte.MAX_VALUE) {
                    byte id = (byte) parsed_id;
                    return new PerfectLinkConfig(nbMessages, id);
                }
            }
        } catch (NumberFormatException e) {
            System.err.println("Error occurred parse the parameters of the config");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("An error occurred while reading the config file");
            e.printStackTrace();
        }
        return null;
    }

    public class PerfectLinkConfig {
        private final int nbMessages;
        private final byte receiverId;

        public PerfectLinkConfig(int nbMessages, byte receiverId) {
            this.nbMessages = nbMessages;
            this.receiverId = receiverId;
        }

        public int getNbMessages() {
            return nbMessages;
        }

        public byte getReceiverId() {
            return receiverId;
        }

        @Override
        public String toString() {
            return "PerfectLinkConfig [nbMessages=" + nbMessages + ", receiverId=" + receiverId + "]";
        }

    }

}
