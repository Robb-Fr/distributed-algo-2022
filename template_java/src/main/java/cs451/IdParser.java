package cs451;

public class IdParser {

    private static final String ID_KEY = "--id";

    private byte id;

    public boolean populate(String key, String value) {
        if (!key.equals(ID_KEY)) {
            return false;
        }

        try {
            int parsed_id = Integer.parseInt(value);
            if (parsed_id <= 0) {
                System.err.println("Id must be a positive number!");
                return false;
            }
            if (parsed_id > Byte.MAX_VALUE) {
                System.err.println("Id must be a smaller than byte max value!");
                return false;
            }
            id = (byte) parsed_id;
        } catch (NumberFormatException e) {
            System.err.println("Id must be a number!");
            return false;
        }

        return true;
    }

    public int getId() {
        return id;
    }

    public byte getByteId() {
        return id;
    }

}
