import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;

public class DNSRecord {
    String[] url;
    short type;
    short RClass;
    int TTL;
    long expiry;
    long timestamp;
    short RLength;
    byte[] address;

    @Override
    public String toString() {
        return "DNSRecord{" +
                "url=" + Arrays.toString(url) +
                ", type=" + type +
                ", RClass=" + RClass +
                ", TTL=" + TTL +
                ", expiry=" + expiry +
                ", timestamp=" + timestamp +
                ", RLength=" + RLength +
                ", address=" + Arrays.toString(address) +
                '}';
    }


    /**
     * Check if it compressed, takes the compression offset pointer and get the domain name, otherwise, get the domain full name
     * Decode the rest
     *
     * @param inputStream InputStream
     * @param msg         DNSMessage
     * @return DNSRecord
     * @throws IOException IOException
     */
    static DNSRecord decodeRecord(InputStream inputStream, DNSMessage msg) throws IOException {

        DNSRecord dnsRecord = new DNSRecord();
        byte checkByte;
        byte[] inBytes;
        inputStream.mark(1);

        checkByte = (byte) inputStream.read();

        boolean isCompressed = (checkByte & 0xC0) == 0xC0;
//        C00C
        if (isCompressed) {
            int tmp = inputStream.read() & 0x3F;
            int tmp2 = inputStream.read() & 0xFF;
            System.out.println("tmp: " + tmp);
            System.out.println("tmp2: " + tmp2);

            int offSet = tmp | tmp2;

            System.out.println("Offset: " + offSet);
            dnsRecord.url = msg.readDomainName(offSet);

        } else {
            inputStream.reset();
            dnsRecord.url = msg.readDomainName(inputStream);
        }


        inBytes = inputStream.readNBytes(1);

        dnsRecord.type = (short) (inBytes[0]);

        inBytes = inputStream.readNBytes(2);
        dnsRecord.RClass = (short) (inBytes[0] << 8);
        dnsRecord.RClass = (short) (dnsRecord.RClass | (inBytes[1] & 0xFF));

        inBytes = inputStream.readNBytes(4);
        dnsRecord.TTL = (inBytes[0] & 0xFF) << 24;
        dnsRecord.TTL = (dnsRecord.TTL | ((inBytes[1] & 0xFF) << 16));
        dnsRecord.TTL = (dnsRecord.TTL | ((inBytes[2] & 0xFF) << 8));
        dnsRecord.TTL = (dnsRecord.TTL | ((inBytes[3] & 0xFF)));
        dnsRecord.timestamp = Calendar.getInstance().getTime().getTime();
        dnsRecord.expiry = dnsRecord.timestamp + (dnsRecord.TTL * 1000L);
        System.out.println("Time: " + dnsRecord.timestamp);
        System.out.println("TTL: " + dnsRecord.TTL);
        System.out.println("Expiry: " + dnsRecord.expiry);


        inBytes = inputStream.readNBytes(2);
        dnsRecord.RLength = (short) ((inBytes[0] & 0xFF) << 8);
        dnsRecord.RLength = (short) (dnsRecord.RLength | ((inBytes[1] & 0xFF)));


        inBytes = inputStream.readNBytes(dnsRecord.RLength);
        dnsRecord.address = inBytes;

        System.out.println(dnsRecord.toString());
        return dnsRecord;

    }

    /**
     * Write everything decoded into outputStream
     *
     * @param byteArrayOutputStream ByteArrayOutputStream
     * @param stringIntegerHashMap  HashMap<String, Integer>
     * @param dnsHeader             DNSHeader
     * @throws IOException IOException
     */
    void writeBytes(ByteArrayOutputStream byteArrayOutputStream, HashMap<String, Integer> stringIntegerHashMap, DNSHeader dnsHeader) throws IOException {
        DNSMessage.writeDomainName(byteArrayOutputStream, stringIntegerHashMap, url);

        byteArrayOutputStream.write(type >> 8);
        byteArrayOutputStream.write(type);
        byteArrayOutputStream.write(RClass >> 8);
        byteArrayOutputStream.write(RClass);
        byteArrayOutputStream.write(TTL >> 24);
        byteArrayOutputStream.write(TTL >> 16);
        byteArrayOutputStream.write(TTL >> 8);
        byteArrayOutputStream.write(TTL);
        byteArrayOutputStream.write(RLength >> 8);
        byteArrayOutputStream.write(RLength);
        byteArrayOutputStream.write(address);


    }


    /**
     * If the cache hashMap contains the thing, check if it expired, then return true or remove that stuff if expired and return false.
     *
     * @param dnsMessage DNSMessage
     * @return Boolean
     */
    static boolean isExpired(DNSMessage dnsMessage) {
        if (DNSCache.dnsQuestionDNSRecordHashMap.containsKey(dnsMessage.questions.get(0))) {
            DNSRecord dnsRecord = DNSCache.dnsQuestionDNSRecordHashMap.get(dnsMessage.questions.get(0));
            System.out.println(Calendar.getInstance().getTime().getTime() + "Expired?");
            System.out.println(dnsRecord.expiry + "Expiry");
            if (Calendar.getInstance().getTime().getTime() <= dnsRecord.expiry) {
                return true;
            } else {
                DNSCache.dnsQuestionDNSRecordHashMap.remove(dnsMessage.questions.get(0));
            }
        }
        return false;
    }


}
