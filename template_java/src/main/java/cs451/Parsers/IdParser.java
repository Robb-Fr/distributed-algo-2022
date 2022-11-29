package cs451.Parsers;

public class IdParser {

    private static final String ID_KEY = "--id";

    private short id;

    public boolean populate(String key, String value) {
        if (!key.equals(ID_KEY)) {
            return false;
        }

        try {
            id = Short.parseShort(value);
            if (id <= 0) {
                System.err.println("Id must be a positive number!");
            }
        } catch (NumberFormatException e) {
            System.err.println("Id must be a number!");
            return false;
        }

        return true;
    }

    public short getId() {
        return id;
    }

}
