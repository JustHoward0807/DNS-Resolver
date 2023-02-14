import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DNSHeader {
    @Override
    public String toString() {
        return "DNSHeader{" +
                "ID=" + ID +
                ", isResponse=" + isResponse +
                ", opcode=" + opcode +
                ", AA_authoritative=" + AA_authoritative +
                ", truncated=" + truncated +
                ", RD_recursion_desire=" + RD_recursion_desire +
                ", RA_recursion_available=" + RA_recursion_available +
                ", Z_Reserverd=" + Z_Reserverd +
                ", AD_adBit=" + AD_adBit +
                ", CD=" + CD +
                ", RCODE=" + RCODE +
                ", QDCOUNT=" + QDCOUNT +
                ", ANCOUNT=" + ANCOUNT +
                ", NSCOUNT=" + NSCOUNT +
                ", ARCOUNT=" + ARCOUNT +
                '}';
    }

    short ID;
    boolean isResponse;
    byte opcode;
    byte AA_authoritative;
    byte truncated;
    byte RD_recursion_desire;
    byte RA_recursion_available;
    byte Z_Reserverd;
    byte AD_adBit;
    byte CD;
    byte RCODE;
    short QDCOUNT;
    short ANCOUNT;

    short NSCOUNT;
    short ARCOUNT;

    /**
     * Decode header according to
     * <a href="https://www.rfc-editor.org/rfc/rfc5395#section-2.2">https://www.rfc-editor.org/rfc/rfc5395#section-2.2</a>
     *
     * @param inputStream InputSteam
     * @return DNSHeader
     * @throws IOException IOException
     */
    public static DNSHeader decodeHeader(InputStream inputStream) throws IOException {


//        System.out.println(Arrays.toString(inputStream.readAllBytes()));
//        inputStream.reset();
        DNSHeader dnsHeader = new DNSHeader();
        System.out.println("Length: " + inputStream.available());

        byte[] tIDArray = inputStream.readNBytes(2);
//        var ID1 = Integer.toHexString(tIDArray[0] & 0xff);
//        var ID2 = Integer.toHexString(tIDArray[1] & 0xff);
//        dnsHeader.transaction_ID = ID1 + ID2;
        dnsHeader.ID = (short) (tIDArray[0] << 8);
        dnsHeader.ID = (short) (dnsHeader.ID | (tIDArray[1] & 0xFF));
//        System.out.println(dnsHeader.ID);

        byte[] t = inputStream.readNBytes(2);

        dnsHeader.isResponse = (t[0] & 0x3E80) > 0;
        dnsHeader.opcode = (byte) ((byte) (t[0] & 0x78) >> 3);
        dnsHeader.AA_authoritative = (byte) ((byte) (t[0] & 0x04) >> 2);
        dnsHeader.truncated = (byte) ((byte) (t[0] & 0x02) >> 1);
        dnsHeader.RD_recursion_desire = (byte) (t[0] & 0x01);

        dnsHeader.RA_recursion_available = (byte) ((((byte) (t[1] & 0x80) & 0xFF) >> 7));
        dnsHeader.Z_Reserverd = (byte) ((byte) (t[1] & 0x40) >> 6);
        dnsHeader.AD_adBit = (byte) ((byte) (t[1] & 0x20) >> 5);
        dnsHeader.CD = (byte) ((byte) (t[1] & 0x10) >> 4);
        dnsHeader.RCODE = (byte) (t[1] & 0x0F);
        byte[] read2Bytes = inputStream.readNBytes(2);
        dnsHeader.QDCOUNT = (short) (read2Bytes[0] << 8);
        dnsHeader.QDCOUNT |= read2Bytes[1];


        read2Bytes = inputStream.readNBytes(2);
        dnsHeader.ANCOUNT = (short) (read2Bytes[0] << 8);
        dnsHeader.ANCOUNT |= read2Bytes[1];

        read2Bytes = inputStream.readNBytes(2);
        dnsHeader.NSCOUNT = (short) (read2Bytes[0] << 8);
        dnsHeader.NSCOUNT |= read2Bytes[1];

        read2Bytes = inputStream.readNBytes(2);
        dnsHeader.ARCOUNT = (short) (read2Bytes[0] << 8);
        dnsHeader.ARCOUNT |= read2Bytes[1];

        System.out.println(dnsHeader.toString());
        return dnsHeader;
    }

    /**
     * @param request  DNSMessage
     * @param response DNSMessage
     * @return DNSHeader
     */
    static DNSHeader buildHeaderForResponse(DNSMessage request, DNSMessage response) {
        //Set the header field to corresponding value.
        DNSHeader dnsHeader = request.dnsHeader;
        dnsHeader.isResponse = true;
        dnsHeader.ANCOUNT = 1;
        dnsHeader.RA_recursion_available = 1;
        return dnsHeader;
    }

    /**
     * Write everything that decoded into outputStream
     *
     * @param outputStream OutputStream
     * @throws IOException IOException
     */
    void writeBytes(OutputStream outputStream) throws IOException {

        byte outputByte = 0;
        //Write ID
        outputStream.write(ID >> 8);
        outputStream.write(ID);

        //Write response
        if (isResponse) outputByte = (byte) 0x80;


        //Write opcode
        outputByte |= (opcode << 3);

        //Write AA_authoritative
        outputByte |= (AA_authoritative << 2);

        //Write truncated
        outputByte |= (truncated << 1);

        //Write RD_recursion_desire
        outputByte |= RD_recursion_desire;
        outputStream.write(outputByte);

        //Reset byte
        outputByte = 0;
        //Write RA_recursion_available
        outputByte |= (RA_recursion_available << 7);

        //Write Z_Reserved
        outputByte |= (Z_Reserverd << 6);

        //Write ADBit
        outputByte |= (AD_adBit << 5);

        //Write CD
        outputByte |= (CD << 4);

        //Write RCODE
        outputByte |= (RCODE);
        outputStream.write(outputByte);
        outputStream.write(QDCOUNT >> 8);
        outputStream.write(QDCOUNT);
        outputStream.write(ANCOUNT >> 8);
        outputStream.write(ANCOUNT);
        outputStream.write(NSCOUNT >> 8);
        outputStream.write(NSCOUNT);
        outputStream.write(ARCOUNT >> 8);
        outputStream.write(ARCOUNT);
    }
}
