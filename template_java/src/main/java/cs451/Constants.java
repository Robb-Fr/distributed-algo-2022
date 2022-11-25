package cs451;

public class Constants {
    public static final int ARG_LIMIT_CONFIG = 7;

    // indexes for id
    public static final int ID_KEY = 0;
    public static final int ID_VALUE = 1;

    // indexes for hosts
    public static final int HOSTS_KEY = 2;
    public static final int HOSTS_VALUE = 3;

    // indexes for output
    public static final int OUTPUT_KEY = 4;
    public static final int OUTPUT_VALUE = 5;

    // indexes for config
    public static final int CONFIG_VALUE = 6;

    public static final int SERIALIZED_MSG_SIZE = 9;

    public static final int PL_SLEEP_BEFORE_RESEND = 8;

    public static final int URB_SLEEP_BEFORE_RESEND = 10;

    public static final int FIFO_SLEEP_BEFORE_RESEND = 20;

    public static final int URB_SLEEP_BEFORE_NEXT_POLL = 20;

    public static final int RECEIVER_SLEEP_BEFORE_NEXT_POLL = 20;

    public static final int THRESHOLD_NB_HOST_FOR_BACK_OFF = 8;

    public static final int SOCKET_TIMEOUT = 1;

    public static final long TIME_BEFORE_FLUSH = 2000;

    public static final int MAX_OUT_OF_ORDER_DELIVERY = 7;

    public static final byte[] EMPTY_MESSAGE = new byte[SERIALIZED_MSG_SIZE];
}
