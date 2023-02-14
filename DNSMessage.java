import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DNSMessage {
    byte[] _bytes = null;
    ByteArrayInputStream inputStream;
    DNSHeader dnsHeader = null;
    ArrayList<DNSQuestion> questions = new ArrayList<>();
    ArrayList<DNSRecord> records = new ArrayList<>();
    ArrayList<DNSRecord> authority_Record = new ArrayList<>();
    ArrayList<DNSRecord> additional_Record = new ArrayList<>();

    /**
     * Decode everything in this function including header, questions, record.
     * Error handling such as entering unknown domain name, meaning there would be no answer section to decode.
     *
     * @param bytes Getting the input bytes to inputStream
     * @return DNSMessage
     * @throws IOException IOException
     */
    static DNSMessage decodeMessage(byte[] bytes) throws IOException {
        DNSMessage dnsMessage = new DNSMessage();
        dnsMessage._bytes = bytes;

        dnsMessage.inputStream = new ByteArrayInputStream(bytes);
        DNSHeader dnsHeader = DNSHeader.decodeHeader(dnsMessage.inputStream);

        dnsMessage.dnsHeader = dnsHeader;

        for (int i = 0; i < dnsHeader.QDCOUNT; i++) {
            dnsMessage.questions.add(DNSQuestion.decodeQuestion(dnsMessage.inputStream, dnsMessage));
        }

        for (int i = 0; i < dnsHeader.ANCOUNT; i++) {
            dnsMessage.records.add(DNSRecord.decodeRecord(dnsMessage.inputStream, dnsMessage));
        }

        for (int i = 0; i < dnsHeader.NSCOUNT; i++) {
            dnsMessage.authority_Record.add(DNSRecord.decodeRecord(dnsMessage.inputStream, dnsMessage));
        }

        for (int i = 0; i < dnsHeader.ARCOUNT; i++) {
            dnsMessage.additional_Record.add(DNSRecord.decodeRecord(dnsMessage.inputStream, dnsMessage));

        }

        System.out.println();
//        Test writeDomain function
//        ByteArrayOutputStream b = new ByteArrayOutputStream();
//        HashMap<String, Integer> t = new HashMap<>();
//        String[] s = new String[] { "google", "com", "hello", "there"};
//        writeDomainName(b, t, s);
        return dnsMessage;


    }

    /**
     * Read the domain name that will only consist of alphabets or numbers (ASCII table from 48 ~ 122)
     * Since String[] cannot grow in size, so i put everything in arrayList and for loop into String[]
     *
     * @param inputStream InputStream
     * @return Return domain name in String[]
     * @throws IOException IOException
     */
    String[] readDomainName(InputStream inputStream) throws IOException {
        inputStream.skipNBytes(1);
        List<String> domain = new ArrayList<>();
        int domainEnding = inputStream.read();
        String tempDomainName = "";

        while (domainEnding != 0) {
            if ((char) domainEnding >= 48 && (char) domainEnding <= 122) {
                tempDomainName += (char) domainEnding;
            } else {
                domain.add(tempDomainName);
                tempDomainName = "";
            }
            domainEnding = inputStream.read();
        }
        domain.add(tempDomainName);
        String[] domainName = new String[domain.size()];
        for (int i = 0; i < domain.size(); i++) {
            domainName[i] = domain.get(i);
        }
        return domainName;

    }


    /**
     * Same function purpose as above but taking first byte which is offset pointer to the domain name.
     * The purpose of compression pointer is to reduce the size of inserting same domain name in bytes over again.
     *
     * @param firstByte -> Offset pointer to domain name location
     * @return Return domain name in String[]
     * @throws IOException IOException
     */
    String[] readDomainName(int firstByte) throws IOException {

        byte[] copyBytes = _bytes;
        ByteArrayInputStream _inputStream = new ByteArrayInputStream(copyBytes);
        _inputStream.skipNBytes(firstByte + 1);

        List<String> domain = new ArrayList<>();
        int domainEnding = _inputStream.read();
        String tempDomainName = "";
        while (domainEnding != 0) {
            if ((char) domainEnding >= 48 && (char) domainEnding <= 122) {
                tempDomainName += (char) domainEnding;
            } else {
                domain.add(tempDomainName);
                tempDomainName = "";
            }
            domainEnding = _inputStream.read();

        }
        domain.add(tempDomainName);
        String[] domainName = new String[domain.size()];
        for (int i = 0; i < domain.size(); i++) {
            domainName[i] = domain.get(i);
        }
        return domainName;
    }

    /**
     * @param request from client
     * @param answers from cache
     * @return Total response as whole
     */
    static DNSMessage buildResponse(DNSMessage request, DNSRecord answers) {
        DNSMessage msg = new DNSMessage();
        msg.dnsHeader = DNSHeader.buildHeaderForResponse(request, msg);
        msg.questions = request.questions;
        msg.records.add(answers);
        msg.authority_Record = request.authority_Record;
        msg.additional_Record = request.additional_Record;

        return msg;

    }

    /**
     * @param pieces domain name in String[]
     * @return String with "." Ex: ["google", "com"] to google.com
     */
    String joinDomainName(String[] pieces) {
        String output = "";
        for (int i = 0; i < pieces.length; i++) {
            if (i != pieces.length - 1) {
                output += pieces[i] + ".";
            } else {
                output += pieces[i];
            }
        }
        return output;
    }

    /**
     * Depends on how many questions or records there are. We writeBytes
     *
     * @return byte[] ready to send back to client
     * @throws IOException IOException
     */
    byte[] toBytes() throws IOException {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        HashMap<String, Integer> domainLocations = new HashMap<>();


        dnsHeader.writeBytes(outBytes);

        for (DNSQuestion dnsQuestion : questions
        ) {

            dnsQuestion.writeBytes(outBytes, domainLocations);

        }

        for (DNSRecord dnsRecord : records
        ) {

            dnsRecord.writeBytes(outBytes, domainLocations, dnsHeader);

        }

        for (DNSRecord dnsRecord : authority_Record
        ) {
            dnsRecord.writeBytes(outBytes, domainLocations, dnsHeader);
        }

        for (DNSRecord dnsRecord : additional_Record
        ) {
            dnsRecord.writeBytes(outBytes, domainLocations, dnsHeader);
        }

        for (byte b : outBytes.toByteArray()
        ) {
            System.out.print(b + " ");
        }
        System.out.println();
        for (byte b : outBytes.toByteArray()
        ) {
            System.out.print(Integer.toHexString(b) + " ");
        }

        return outBytes.toByteArray();
    }

    /**
     * If the domainLocation not found, write a domain name byte. such as first bytes indicates length of the domain name behind. 06 google 03 com 00 (indicates end)
     * else get the domain name length which is an offset to the full length of domain name location.
     *
     * @param bOutputstream   write into outputStream
     * @param domainLocations Indicate which domain name have how many length. Ex: google : 6, com : 3
     * @param domainPieces    Provide domain name pieces
     */
    static void writeDomainName(ByteArrayOutputStream bOutputstream, HashMap<String, Integer> domainLocations, String[] domainPieces) {
        if (domainPieces[0].equals("")) {
            System.out.println("It is 0");
            bOutputstream.write(0);
//            bOutputstream.write(0);
            return;
        }
        DNSMessage dnsMessage = new DNSMessage();
        String joinDomainName = dnsMessage.joinDomainName(domainPieces);

        if (!domainLocations.containsKey(joinDomainName)) {
            //Put domain string and the current bO size in order store the location
            domainLocations.put(joinDomainName, bOutputstream.size());
            for (String domain : domainPieces) {
                //Before each ".". Indicate length
                System.out.println(domain.length() + "length ");
                bOutputstream.write(domain.length());
                for (int i = 0; i < domain.length(); i++) {
                    char domainChar = domain.charAt(i);
                    bOutputstream.write(domainChar);
                }
            }
            //0 Indicates the end of the domain name
            bOutputstream.write(0);

        } else {
            int offset = domainLocations.get(joinDomainName);
            System.out.println("Compression Offset: " + offset);
            bOutputstream.write((offset >> 8) | 0xC0);
            bOutputstream.write(offset);

        }
    }

}
