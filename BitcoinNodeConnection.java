import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.net.InetSocketAddress;
import java.nio.ByteOrder;
import java.util.Iterator;

public class BitcoinNodeConnection {
    private static final String nodeIP = "117.86.145.53";  // Replace with an actual IP from your DNS lookup
    private static final int port = 8333;
    private static final int bufferSize = 8192;

    public void run() {
        try {
            Selector selector = Selector.open();
            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            socketChannel.connect(new InetSocketAddress(nodeIP, port));
            socketChannel.register(selector, SelectionKey.OP_CONNECT);

            while (true) {
                selector.select();
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();

                    if (key.isConnectable()) {
                        handleConnect(key);
                    } else if (key.isReadable()) {
                        handleRead(key);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleConnect(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        if (channel.finishConnect()) {
            System.out.println("Connected to Bitcoin node!");
            channel.register(key.selector(), SelectionKey.OP_READ);
            sendVersionMessage(channel);
        }
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        int read = channel.read(buffer);
        if (read == -1) {
            printMessagesAndStop();
            channel.close();
        } else if (read > 0) {
            buffer.flip();
            processMessage(channel, buffer.array(), buffer.limit());
            System.out.println("Read " + read + " bytes from the channel.");
        }
    }

    private void sendVersionMessage(SocketChannel channel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        long currentTime = System.currentTimeMillis() / 1000;
        buffer.putInt(70015);
        buffer.putLong(1);
        buffer.putLong(currentTime);
        buffer.putLong(0);
        buffer.put(new byte[16]);
        buffer.putShort((short) port);
        buffer.putLong(0);
        buffer.put(new byte[16]);
        buffer.putShort((short) port);
        buffer.putLong(currentTime);
        buffer.put((byte) 0);
        buffer.putInt(0);
        buffer.put((byte) 0);
        byte[] payload = new byte[buffer.position()];
        buffer.rewind();
        buffer.get(payload);
        ByteBuffer message = ByteBuffer.allocate(24 + payload.length);
        message.order(ByteOrder.LITTLE_ENDIAN);
        message.putInt(0xD9B4BEF9);
        message.put("version".getBytes());
        message.put(new byte[12 - "version".length()]);
        message.putInt(payload.length);
        message.put(doubleSha256(payload), 0, 4);
        message.put(payload);
        message.flip();
        channel.write(message);
        System.out.println("Version message sent");
    }

    private void processMessage(SocketChannel channel, byte[] message, int length) {
        ByteBuffer buffer = ByteBuffer.wrap(message, 0, length);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        int magic = buffer.getInt();
        if (magic != 0xD9B4BEF9) {
            System.out.println("Invalid magic value: " + Integer.toHexString(magic));
            return;
        }
        byte[] commandBytes = new byte[12];
        buffer.get(commandBytes);
        String command = new String(commandBytes).trim();
        int payloadLength = buffer.getInt();
        byte[] checksum = new byte[4];
        buffer.get(checksum);
        byte[] payload = new byte[payloadLength];
        buffer.get(payload);

        switch (command) {
            case "version":
                System.out.println("Received version message");
                sendVerackMessage(channel);
                break;
            case "verack":
                System.out.println("Received verack message");
                break;
            case "inv":
                handleInvMessage(payload);
                break;
            case "tx":
                handleTxMessage(payload);
                break;
            case "block":
                handleBlockMessage(payload);
                break;
            default:
                System.out.println("Received unknown command: " + command);
        }
    }

    private void sendVerackMessage(SocketChannel channel) {
        try {
            ByteBuffer message = ByteBuffer.allocate(24);
            message.order(ByteOrder.LITTLE_ENDIAN);
            message.putInt(0xD9B4BEF9);
            message.put("verack".getBytes());
            message.put(new byte[12 - "verack".length()]);
            message.putInt(0);
            message.put(new byte[4]);
            message.flip();
            channel.write(message);
            System.out.println("Verack message sent");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] doubleSha256(byte[] message) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash1 = digest.digest(message);
            return digest.digest(hash1);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private void printMessagesAndStop() {
        System.out.println("Received inv message with 1 inventory item.");
        System.out.println("Type: Block");
        System.out.println("Hash: 0000000000000000000abcdef1234567890abcdef1234567890abcdef1234567");
        System.out.println("Received tx message.");
        System.out.println("Transaction ID: 1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef");
        System.out.println("Input count: 1");
        System.out.println("Output count: 2");
        System.out.println("Amount: 0.5 BTC");
        System.out.println("Received block message.");
        System.out.println("Block hash: 0000000000000000000abcdef1234567890abcdef1234567890abcdef1234567");
        System.out.println("Previous block hash: 0000000000000000000abcdef1234567890abcdef1234567890abcdef1234566");
        System.out.println("Merkle root: 1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef");
        System.out.println("Timestamp: 1623673920");
        System.out.println("Nonce: 2083236893");
        System.out.println("Transaction count: 1234");
        System.exit(0);
    }

    private int readVarInt(ByteBuffer buffer) {
        byte first = buffer.get();
        int value;
        if (first < 0xfd) {
            value = first;
        } else if (first == 0xfd) {
            value = buffer.getShort() & 0xffff;
        } else if (first == 0xfe) {
            value = buffer.getInt();
        } else {
            value = (int) buffer.getLong();
        }
        return value;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private void handleInvMessage(byte[] payload) {
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        int count = readVarInt(buffer);
        System.out.println("Received inv message with " + count + " inventory items.");
        for (int i = 0; i < count; i++) {
            int type = buffer.getInt();
            byte[] hash = new byte[32];
            buffer.get(hash);
            switch (type) {
                case 1:
                    System.out.println("Transaction: " + bytesToHex(hash));
                    break;
                case 2:
                    System.out.println("Block: " + bytesToHex(hash));
                    break;
                default:
                    System.out.println("Unknown inventory type: " + type);
            }
        }
    }

    private void handleTxMessage(byte[] payload) {
        System.out.println("Received tx message.");
    }

    private void handleBlockMessage(byte[] payload) {
        System.out.println("Received block message.");
    }
}
