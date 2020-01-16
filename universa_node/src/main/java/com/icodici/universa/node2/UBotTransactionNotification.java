package com.icodici.universa.node2;

import com.icodici.universa.HashId;
import net.sergeych.boss.Boss;

import java.io.IOException;
import java.util.Objects;

public class UBotTransactionNotification extends Notification {

    private static final int CODE_UBOT_TRANSACTION_NOTIFICATION = 7;
    private HashId executableContractId;
    private HashId requestId;
    private String transactionName;
    private boolean start;

    public UBotTransactionNotification(NodeInfo from, HashId executableContractId, HashId requestId, String transactionName, boolean isStart) {
        super(from);
        this.executableContractId = executableContractId;
        this.requestId = requestId;
        this.transactionName = transactionName;
        this.start = isStart;
    }

    @Override
    protected void writeTo(Boss.Writer bw) throws IOException {
        bw.writeObject(executableContractId.getDigest());
        bw.writeObject(requestId.getDigest());
        bw.writeObject(transactionName.getBytes());
        bw.writeObject(start ? 1 : 0);
    }

    @Override
    protected void readFrom(Boss.Reader br) throws IOException {
        executableContractId = HashId.withDigest(br.readBinary());
        requestId = HashId.withDigest(br.readBinary());
        transactionName = new String(br.readBinary());
        start = br.readInt() == 1;
    }

    protected UBotTransactionNotification(NodeInfo from) throws IOException {
        super(from);
    }

    protected UBotTransactionNotification() {
    }

    @Override
    protected int getTypeCode() {
        return CODE_UBOT_TRANSACTION_NOTIFICATION;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UBotTransactionNotification that = (UBotTransactionNotification) o;

        NodeInfo from = getFrom();
        if (!executableContractId.equals(that.executableContractId))
            return false;
        if (!requestId.equals(that.requestId))
            return false;
        if (!transactionName.equals(that.transactionName))
            return false;
        return start == that.start;
    }

    @Override
    public int hashCode() {
        return Objects.hash(executableContractId, transactionName);
    }

    public String toString() {
        return "[UBotTransactionNotification from " + getFrom()
                + " for item: " + executableContractId
                + " for request: " + requestId
                + ", transactionName: " + transactionName
                + ", isStart: " + start
                + "]";
    }

    static public void init() {
        registerClass(CODE_UBOT_TRANSACTION_NOTIFICATION, UBotTransactionNotification.class);
    }

    public HashId getExecutableContractId() {
        return executableContractId;
    }

    public HashId getRequestId() {
        return requestId;
    }

    public String getTransactionName() {
        return transactionName;
    }

    public boolean isStart() {
        return start;
    }
}
