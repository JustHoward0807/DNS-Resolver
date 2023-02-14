import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;

public class DNSServer {


    /**
     * 1. Receiving data from client
     * 2. Decode the data
     * 3. If the data has seen before (in cache) ?
     *      Yes - Build the response then send it back
     *      No  - I. Send the data to google and receive the data.
     *            II. Store in cache.
     *            III. Decond the data.
     *            IV. Send back to client.
     * <p>
     *     Ref: <br>
     *     <a href = "https://www.ietf.org/rfc/rfc1035.txt">https://www.ietf.org/rfc/rfc1035.txt</a> <br>
     *     <a href = "https://levelup.gitconnected.com/dns-request-and-response-in-java-acbd51ad3467">https://levelup.gitconnected.com/dns-request-and-response-in-java-acbd51ad3467</a> <br>
     *     <a href = "https://cabulous.medium.com/dns-message-how-to-read-query-and-response-message-cfebcb4fe817">https://cabulous.medium.com/dns-message-how-to-read-query-and-response-message-cfebcb4fe817</a> <br>
     *     <a href = "https://mislove.org/teaching/cs4700/spring11/handouts/project1-primer.pdf">https://mislove.org/teaching/cs4700/spring11/handouts/project1-primer.pdf</a> <br>
     * </p>
     * @param args No usages
     */

    public static void main(String[] args) {
        try {
            DatagramSocket socket = new DatagramSocket(8053);
            byte[] in = new byte[512];

            while (true) {
                DatagramPacket userPacket = new DatagramPacket(in, in.length);

                socket.receive(userPacket);

                InetAddress userAddress = userPacket.getAddress();
                int userPort = userPacket.getPort();
                System.out.println(userAddress + " " + userPort + "UserAddress + UserPort");
                System.out.println("Waiting to receive a client data...");
                //Decode msg from client
                byte[] receivedClientData = userPacket.getData();
                DNSMessage clientMsg = DNSMessage.decodeMessage(receivedClientData);

                boolean inCache = DNSRecord.isExpired(clientMsg);

                if (inCache) {
                    System.out.println("It is in cache");
                    DNSRecord dnsRecord = DNSCache.querying(clientMsg);
                    DNSMessage dnsMessage = DNSMessage.buildResponse(clientMsg, dnsRecord);

                    byte[] responseData = dnsMessage.toBytes();

                    DatagramPacket cacheToUser = new DatagramPacket(responseData, responseData.length, userAddress, userPort);
                    socket.send(cacheToUser);


                } else {
                    System.out.println("It is not in cache");
                    DatagramPacket toGoogle = new DatagramPacket(receivedClientData, receivedClientData.length, Inet4Address.getByName("8.8.8.8"), 53);
                    socket.send(toGoogle);
                    //Received the data from Google
                    DatagramPacket receivedFromGoogle = new DatagramPacket(in, in.length, toGoogle.getAddress(), 53);
                    System.out.println("Waiting to receive Google data...");
                    socket.receive(receivedFromGoogle);
                    byte[] receivedGoogleData = receivedFromGoogle.getData();
                    DNSMessage googleMsg = DNSMessage.decodeMessage(receivedGoogleData);
                    DNSRecord dnsRecord = new DNSRecord();

                    DNSCache.inserting(googleMsg);


                    //The reason why not just send google response back. Its bc the byte array would be too big and have extra 0, so the terminal client would received a warning that extra bytes.
                    byte[] responseGoogleData = googleMsg.toBytes();

                    //Sending data back to client
                    DatagramPacket toUser = new DatagramPacket(responseGoogleData, responseGoogleData.length, userPacket.getAddress(), userPacket.getPort());
//                    DatagramPacket toUser = new DatagramPacket(receivedGoogleData, receivedGoogleData.length, userPacket.getAddress(), userPacket.getPort());
                    socket.send(toUser);
                }

            }
        } catch (Exception ioe) {
            System.out.println(ioe);
        }

    }
}



