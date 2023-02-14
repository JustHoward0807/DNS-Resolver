import java.util.HashMap;

public class DNSCache {
    static HashMap<DNSQuestion, DNSRecord> dnsQuestionDNSRecordHashMap = new HashMap<>();
    //This class is the local cache. It should basically just have a HashMap<DNSQuestion, DNSRecord> in it.
    // You can just store the first answer for any question in the cache (a response for google.com might return 10 IP addresses, just store the first one).
    // This class should have methods for querying and inserting records into the cache.
    // When you look up an entry, if it is too old (its TTL has expired), remove it and return "not found."

    /**
     * Get the answer
     *
     * @param dnsMessage DNSMessage
     * @return DNSRecord
     */
    static DNSRecord querying(DNSMessage dnsMessage) {
        return dnsQuestionDNSRecordHashMap.get(dnsMessage.questions.get(0));
    }

    /**
     * Insert the decoded stuff into hashMap. If there is no answer, insert the additional_Record.
     *
     * @param dnsMessage DNSMessage
     */
    static void inserting(DNSMessage dnsMessage) {
        if (dnsMessage.records.size() == 0) {
            dnsQuestionDNSRecordHashMap.put(dnsMessage.questions.get(0), dnsMessage.additional_Record.get(0));
        } else {
            dnsQuestionDNSRecordHashMap.put(dnsMessage.questions.get(0), dnsMessage.records.get(0));
        }

    }
}
