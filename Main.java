public class Main {
    public static void main(String[] args) {
        System.out.println("Starting Bitcoin Node Connection...");

        BitcoinNodeConnection bitcoinNodeConnection = new BitcoinNodeConnection();

        System.out.println("Initializing connection to Bitcoin network...");
        bitcoinNodeConnection.run();

        System.out.println("Bitcoin Node Connection has started.");
        System.out.println("This application will now continuously receive and process messages from the Bitcoin network.");
        System.out.println("Press Ctrl+C to exit.");

        System.out.println("This line will not be reached unless the run method is modified to exit the loop.");
    }
}
