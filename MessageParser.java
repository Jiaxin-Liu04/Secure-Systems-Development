import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class MessageParser {

    public static int parseVersion(byte[] payload) {
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return buffer.getInt();
    }

    public static String parseCommand(byte[] header) {
        ByteBuffer buffer = ByteBuffer.wrap(header, 4, 12);
        return new String(buffer.array()).trim();
    }

    public static byte[] parsePayload(byte[] header, byte[] message) {
        ByteBuffer buffer = ByteBuffer.wrap(header, 16, 4);
        int length = buffer.getInt();
        byte[] payload = new byte[length];
        System.arraycopy(message, 24, payload, 0, length);
        return payload;
    }

    public static boolean parseVerack(byte[] payload) {
        // Verack message has no payload, just return true if received
        return true;
    }

    public static List<String> parseInv(byte[] payload) {
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        int count = (int) readVarInt(buffer);
        List<String> inventory = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            int type = buffer.getInt();
            byte[] hash = new byte[32];
            buffer.get(hash);
            inventory.add(bytesToHex(hash) + " (" + type + ")");
        }
        return inventory;
    }

    public static List<String> parseTx(byte[] payload) {
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        List<String> transactions = new ArrayList<>();
        // Simplified transaction parsing for demonstration
        while (buffer.hasRemaining()) {
            int length = buffer.getInt();
            byte[] tx = new byte[length];
            buffer.get(tx);
            transactions.add(bytesToHex(tx));
        }
        return transactions;
    }

    public static String parseBlock(byte[] payload) {
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        StringBuilder blockDetails = new StringBuilder();

        long version = buffer.getInt();
        byte[] prevBlockHash = new byte[32];
        buffer.get(prevBlockHash);
        byte[] merkleRoot = new byte[32];
        buffer.get(merkleRoot);
        long timestamp = buffer.getInt();
        long bits = buffer.getInt();
        long nonce = buffer.getInt();

        blockDetails.append("Version: ").append(version).append("\n");
        blockDetails.append("Previous Block Hash: ").append(bytesToHex(prevBlockHash)).append("\n");
        blockDetails.append("Merkle Root: ").append(bytesToHex(merkleRoot)).append("\n");
        blockDetails.append("Timestamp: ").append(timestamp).append("\n");
        blockDetails.append("Bits: ").append(bits).append("\n");
        blockDetails.append("Nonce: ").append(nonce).append("\n");

        return blockDetails.toString();
    }

    private static long readVarInt(ByteBuffer buffer) {
        int first = buffer.get() & 0xFF;
        if (first < 0xFD) {
            return first;
        } else if (first == 0xFD) {
            return buffer.getShort() & 0xFFFF;
        } else if (first == 0xFE) {
            return buffer.getInt() & 0xFFFFFFFFL;
        } else {
            return buffer.getLong();
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    
}
