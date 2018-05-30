package com.icodici.universa.node2;

import com.icodici.universa.HashId;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Class-helper for concurrency work with UNS1 ledger functions.
 */
public class NameCache {

    private final Timer cleanerTimer = new Timer();
    private final Duration maxAge;

    final private static String NAME_PREFIX = "n_";
    final private static String ORIGIN_PREFIX = "o_";
    final private static String ADDRESS_PREFIX = "a_";

    public NameCache(Duration maxAge) {
        this.maxAge = maxAge;
        cleanerTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                cleanUp();
            }
        }, 5000, 5000);
    }

    final void cleanUp() {
        Instant now = Instant.now();
        records.values().forEach(r->r.checkExpiration(now));
    }

    public void shutdown() {
        cleanerTimer.cancel();
        cleanerTimer.purge();
    }

    private boolean lockStringValue(String name_reduced) {
        Record prev = records.putIfAbsent(name_reduced, new Record(name_reduced));
        return (prev == null);
    }

    private void unlockStringValue(String name_reduced) {
        records.remove(name_reduced);
    }

    private boolean lockStringList(Collection<String> reducedNameList) {
        boolean isAllNamesLocked = true;
        List<String> lockedByThisCall = new ArrayList<>();
        for (String reducedName : reducedNameList) {
            if (lockStringValue(reducedName)) {
                lockedByThisCall.add(reducedName);
            } else {
                isAllNamesLocked = false;
                break;
            }
        }
        if (!isAllNamesLocked) {
            for (String rn : lockedByThisCall)
                unlockStringValue(rn);
        }
        return isAllNamesLocked;
    }

    private void unlockStringList(Collection<String> reducedNameList) {
        for (String reducedName : reducedNameList)
            unlockStringValue(reducedName);
    }

    private List<String> getSringListWithPrefix(String prefix, Collection<String> srcList) {
        List<String> list = new ArrayList<>(srcList);
        for (int i = 0; i < list.size(); ++i)
            list.set(i, prefix + list.get(i));
        return list;
    }

    public boolean lockNameList(Collection<String> reducedNameList) {
        return lockStringList(getSringListWithPrefix(NAME_PREFIX, reducedNameList));
    }

    public void unlockNameList(Collection<String> reducedNameList) {
        unlockStringList(getSringListWithPrefix(NAME_PREFIX, reducedNameList));
    }

    public boolean lockOriginList(Collection<HashId> originList) {
        List<String> stringList = new ArrayList<>();
        for (HashId origin : originList)
            stringList.add(ORIGIN_PREFIX + origin.toBase64String());
        return lockStringList(stringList);
    }

    public void unlockOriginList(Collection<HashId> originList) {
        List<String> stringList = new ArrayList<>();
        for (HashId origin : originList)
            stringList.add(ORIGIN_PREFIX + origin.toBase64String());
        unlockStringList(stringList);
    }

    public boolean lockAddressList(Collection<String> addressList) {
        return lockStringList(getSringListWithPrefix(ADDRESS_PREFIX, addressList));
    }

    public void unlockAddressList(Collection<String> addressList) {
        unlockStringList(getSringListWithPrefix(ADDRESS_PREFIX, addressList));
    }

    private ConcurrentHashMap<String,Record> records = new ConcurrentHashMap();

    public int size() {
        return records.size();
    }

    private class Record {
        private Instant expiresAt;
        private String name_reduced;

        private Record(String name_reduced) {
            expiresAt = Instant.now().plus(maxAge);
            this.name_reduced = name_reduced;
        }

        private void checkExpiration(Instant now) {
            if( expiresAt.isBefore(now) ) {
                records.remove(name_reduced);
            }
        }
    }

}