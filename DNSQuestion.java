import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

public class DNSQuestion {
    String[] url;

    short type;
    short dClass;

    @Override
    public String toString() {
        return "DNSQuestion{" +
                "url=" + Arrays.toString(url) +
                ", type=" + type +
                ", dClass=" + dClass +
                '}';
    }

    /**
     * Decode the querying section
     *
     * @param inputStream InputStream
     * @param msg         DNSMessage
     * @return DNSQuestion
     * @throws IOException DNSQuestion
     */
    static DNSQuestion decodeQuestion(InputStream inputStream, DNSMessage msg) throws IOException {
        DNSQuestion dnsQuestion = new DNSQuestion();

        dnsQuestion.url = msg.readDomainName(inputStream);
        byte[] readNBytes = inputStream.readNBytes(2);
        dnsQuestion.type = (short) (readNBytes[0] << 8);
        dnsQuestion.type = (short) (dnsQuestion.type | (readNBytes[1]));
        readNBytes = inputStream.readNBytes(2);
        dnsQuestion.dClass = (short) (readNBytes[0] << 8);
        dnsQuestion.dClass = (short) (dnsQuestion.dClass | (readNBytes[1]));
        System.out.println(dnsQuestion.toString());
        return dnsQuestion;
    }

    /**
     * Write everything decoded into bytesOutputStream
     *
     * @param byteArrayOutputStream ByteArrayOutputStream
     * @param domainNameLocations   HashMap<String, Integer>
     */
    void writeBytes(ByteArrayOutputStream byteArrayOutputStream, HashMap<String, Integer> domainNameLocations) {
        DNSMessage.writeDomainName(byteArrayOutputStream, domainNameLocations, url);

        byteArrayOutputStream.write(type >> 8);
        byteArrayOutputStream.write(type);
        byteArrayOutputStream.write(dClass >> 8);
        byteArrayOutputStream.write(dClass);

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DNSQuestion that = (DNSQuestion) o;
        return type == that.type && dClass == that.dClass && Arrays.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(type, dClass);
        result = 31 * result + Arrays.hashCode(url);
        return result;
    }
}
