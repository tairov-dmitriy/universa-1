package com.icodici.universa.node2;

import com.icodici.crypto.PrivateKey;
import com.icodici.crypto.PublicKey;
import com.icodici.universa.HashId;
import com.icodici.universa.contract.Contract;
import com.icodici.universa.contract.ContractsService;
import com.icodici.universa.contract.InnerContractsService;
import com.icodici.universa.contract.Parcel;
import com.icodici.universa.node.ItemResult;
import com.icodici.universa.node.ItemState;
import com.icodici.universa.node.network.TestKeys;
import com.icodici.universa.node2.network.Client;
import com.icodici.universa.node2.network.ClientError;
import net.sergeych.boss.Boss;
import net.sergeych.tools.Do;
import net.sergeych.utils.Bytes;
import net.sergeych.utils.LogPrinter;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class Research2Test {

    Main createMain(String name, boolean nolog) throws InterruptedException {
        return createMain(name,"",nolog);
    }

    Main createMain(String name, String postfix, boolean nolog) throws InterruptedException {
        String path = new File("src/test_node_config_v2" + postfix + "/" + name).getAbsolutePath();
        System.out.println(path);
        String[] args = new String[]{"--test", "--config", path, nolog ? "--nolog" : ""};

        List<Main> mm = new ArrayList<>();

        Thread thread = new Thread(() -> {
            try {
                Main m = new Main(args);
                m.config.setTransactionUnitsIssuerKeyData(Bytes.fromHex("1E 08 1C 01 00 01 C4 00 01 B9 C7 CB 1B BA 3C 30 80 D0 8B 29 54 95 61 41 39 9E C6 BB 15 56 78 B8 72 DC 97 58 9F 83 8E A0 B7 98 9E BB A9 1D 45 A1 6F 27 2F 61 E0 26 78 D4 9D A9 C2 2F 29 CB B6 F7 9F 97 60 F3 03 ED 5C 58 27 27 63 3B D3 32 B5 82 6A FB 54 EA 26 14 E9 17 B6 4C 5D 60 F7 49 FB E3 2F 26 52 16 04 A6 5E 6E 78 D1 78 85 4D CD 7B 71 EB 2B FE 31 39 E9 E0 24 4F 58 3A 1D AE 1B DA 41 CA 8C 42 2B 19 35 4B 11 2E 45 02 AD AA A2 55 45 33 39 A9 FD D1 F3 1F FA FE 54 4C 2E EE F1 75 C9 B4 1A 27 5C E9 C0 42 4D 08 AD 3E A2 88 99 A3 A2 9F 70 9E 93 A3 DF 1C 75 E0 19 AB 1F E0 82 4D FF 24 DA 5D B4 22 A0 3C A7 79 61 41 FD B7 02 5C F9 74 6F 2C FE 9A DD 36 44 98 A2 37 67 15 28 E9 81 AC 40 CE EF 05 AA 9E 36 8F 56 DA 97 10 E4 10 6A 32 46 16 D0 3B 6F EF 80 41 F3 CC DA 14 74 D1 BF 63 AC 28 E0 F1 04 69 63 F7"));
                m.config.getKeysWhiteList().add(m.config.getTransactionUnitsIssuerKey());
                m.waitReady();
                mm.add(m);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        thread.setName("Node Server: " + name);
        thread.start();

        while (mm.size() == 0) {
            Thread.sleep(100);
        }
        return mm.get(0);
    }

    Main createMainFromDb(String dbUrl, boolean nolog) throws InterruptedException {
        String[] args = new String[]{"--test","--database", dbUrl, nolog ? "--nolog" : ""};

        List<Main> mm = new ArrayList<>();

        Thread thread = new Thread(() -> {
            try {
                Main m = new Main(args);
                m.config.setTransactionUnitsIssuerKeyData(Bytes.fromHex("1E 08 1C 01 00 01 C4 00 01 B9 C7 CB 1B BA 3C 30 80 D0 8B 29 54 95 61 41 39 9E C6 BB 15 56 78 B8 72 DC 97 58 9F 83 8E A0 B7 98 9E BB A9 1D 45 A1 6F 27 2F 61 E0 26 78 D4 9D A9 C2 2F 29 CB B6 F7 9F 97 60 F3 03 ED 5C 58 27 27 63 3B D3 32 B5 82 6A FB 54 EA 26 14 E9 17 B6 4C 5D 60 F7 49 FB E3 2F 26 52 16 04 A6 5E 6E 78 D1 78 85 4D CD 7B 71 EB 2B FE 31 39 E9 E0 24 4F 58 3A 1D AE 1B DA 41 CA 8C 42 2B 19 35 4B 11 2E 45 02 AD AA A2 55 45 33 39 A9 FD D1 F3 1F FA FE 54 4C 2E EE F1 75 C9 B4 1A 27 5C E9 C0 42 4D 08 AD 3E A2 88 99 A3 A2 9F 70 9E 93 A3 DF 1C 75 E0 19 AB 1F E0 82 4D FF 24 DA 5D B4 22 A0 3C A7 79 61 41 FD B7 02 5C F9 74 6F 2C FE 9A DD 36 44 98 A2 37 67 15 28 E9 81 AC 40 CE EF 05 AA 9E 36 8F 56 DA 97 10 E4 10 6A 32 46 16 D0 3B 6F EF 80 41 F3 CC DA 14 74 D1 BF 63 AC 28 E0 F1 04 69 63 F7"));
                m.config.getKeysWhiteList().add(m.config.getTransactionUnitsIssuerKey());
                m.waitReady();
                mm.add(m);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        thread.setName("Node Server: " + dbUrl);
        thread.start();

        while (mm.size() == 0) {
            Thread.sleep(100);
        }
        return mm.get(0);
    }

    public synchronized Parcel createParcelWithFreshTU(Client client, Contract c, Collection<PrivateKey> keys) throws Exception {
        Set<PublicKey> ownerKeys = new HashSet();
        keys.stream().forEach(key->ownerKeys.add(key.getPublicKey()));
        Contract stepaTU = InnerContractsService.createFreshTU(100000000, ownerKeys);
        stepaTU.check();
        //stepaTU.setIsTU(true);
        stepaTU.traceErrors();

        PrivateKey clientPrivateKey = client.getSession().getPrivateKey();
        PrivateKey newPrivateKey = new PrivateKey(Do.read("./src/test_contracts/keys/tu_key.private.unikey"));
        client.getSession().setPrivateKey(newPrivateKey);
        client.restart();

        Thread.sleep(8000);

        ItemResult itemResult = client.register(stepaTU.getPackedTransaction(), 5000);
//        node.registerItem(stepaTU);
//        ItemResult itemResult = node.waitItem(stepaTU.getId(), 18000);

        client.getSession().setPrivateKey(clientPrivateKey);
        client.restart();

        Thread.sleep(8000);

        assertEquals(ItemState.APPROVED, itemResult.state);
        Set<PrivateKey> keySet = new HashSet<>();
        keySet.addAll(keys);
        return ContractsService.createParcel(c, stepaTU, 150, keySet);
    }

    @After
    public void tearDown() throws Exception {
        LogPrinter.showDebug(false);
    }

    @Ignore
    @Test
    public void localNetwork() throws Exception {
        List<Main> mm = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            mm.add(createMain("node" + (i + 1), false));
        }

        Main main = mm.get(0);
//        assertEquals("http://localhost:8080", main.myInfo.internalUrlString());
//        assertEquals("http://localhost:8080", main.myInfo.publicUrlString());
        PrivateKey myKey = TestKeys.privateKey(3);

//        assertEquals(main.cache, main.node.getCache());
//        ItemCache c1 = main.cache;
//        ItemCache c2 = main.node.getCache();

//        Client client = new Client(myKey, main.myInfo, null);

        List<Contract> contractsForThreads = new ArrayList<>();
        int N = 100;
        int M = 2;
        float threshold = 1.2f;
        float ratio = 0;
        boolean createNewContracts = false;
//        assertTrue(singleContract.isOk());

//        ItemResult r = client.getState(singleContract.getId());
//        assertEquals(ItemState.UNDEFINED, r.state);
//        System.out.println(r);

        contractsForThreads = new ArrayList<>();
        for (int j = 0; j < M; j++) {
            Contract contract = new Contract(myKey);

            for (int k = 0; k < 10; k++) {
                Contract nc = new Contract(myKey);
                nc.seal();
                contract.addNewItems(nc);
            }
            contract.seal();
            assertTrue(contract.isOk());
            contractsForThreads.add(contract);

//            ItemResult r = client.getState(contract.getId());
//            assertEquals(ItemState.UNDEFINED, r.state);
//            System.out.println(r);
        }

        Contract singleContract = new Contract(myKey);

        for (int k = 0; k < 10; k++) {
            Contract nc = new Contract(myKey);
            nc.seal();
            singleContract.addNewItems(nc);
        }
        singleContract.seal();

        // register
        for (int i = 0; i < N; i++) {

            if (createNewContracts) {
                contractsForThreads = new ArrayList<>();
                for (int j = 0; j < M; j++) {
                    Contract contract = new Contract(myKey);

                    for (int k = 0; k < 10; k++) {
                        Contract nc = new Contract(myKey);
                        nc.seal();
                        contract.addNewItems(nc);
                    }
                    contract.seal();
                    assertTrue(contract.isOk());
                    contractsForThreads.add(contract);
                }

                singleContract = new Contract(myKey);

                for (int k = 0; k < 10; k++) {
                    Contract nc = new Contract(myKey);
                    nc.seal();
                    singleContract.addNewItems(nc);
                }
                singleContract.seal();
            }

            long ts1;
            long ts2;
            Semaphore semaphore = new Semaphore(-(M - 1));

            ts1 = new Date().getTime();

            for (Contract c : contractsForThreads) {
                Thread thread = new Thread(() -> {

                    Client client = null;
                    try {
                        synchronized (this) {
                            client = new Client(myKey, main.myInfo, null);
                        }
                        long t = System.nanoTime();
                        ItemResult rr = null;
                        rr = client.register(c.getPackedTransaction(), 15000);
                        System.out.println("multi thread: " + rr + " time: " + ((System.nanoTime() - t) * 1e-9));

                    } catch (ClientError clientError) {
                        clientError.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    semaphore.release();
                });
                thread.setName("Multi-thread register: " + c.getId().toString());
                thread.start();
            }

            semaphore.acquire();

            ts2 = new Date().getTime();

            long threadTime = ts2 - ts1;

            ts1 = new Date().getTime();

            Contract finalSingleContract = singleContract;
            Thread thread = new Thread(() -> {
                long t = System.nanoTime();
                ItemResult rr = null;
                try {
                    Client client = null;
                    client = new Client(myKey, main.myInfo, null);
                    rr = client.register(finalSingleContract.getPackedTransaction(), 15000);
                    System.out.println("single thread: " + rr + " time: " + ((System.nanoTime() - t) * 1e-9));
                } catch (ClientError clientError) {
                    clientError.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                semaphore.release();
            });
            thread.setName("single-thread register: " + singleContract.getId().toString());
            thread.start();

            semaphore.acquire();

            ts2 = new Date().getTime();

            long singleTime = ts2 - ts1;

            System.out.println(threadTime * 1.0f / singleTime);
            ratio += threadTime * 1.0f / singleTime;
        }
        ratio /= N;
        System.out.println("average " + ratio);

        mm.forEach(x -> x.shutdown());
    }

    @Ignore
    @Test
    public void localNetwork2() throws Exception {
        List<Main> mm = new ArrayList<>();
        for (int i = 0; i < 4; i++)
            mm.add(createMain("node" + (i + 1), false));
        Main main = mm.get(0);
        assertEquals("http://localhost:8080", main.myInfo.internalUrlString());
        assertEquals("http://localhost:8080", main.myInfo.publicUrlString());
        PrivateKey myKey = new PrivateKey(Do.read("./src/test_contracts/keys/tu_key.private.unikey"));

        //Client client = new Client(myKey, main.myInfo, null);

        final long CONTRACTS_PER_THREAD = 60;
        final long THREADS_COUNT = 4;

        class TestRunnable implements Runnable {

            public int threadNum = 0;
            List<Contract> contractList = new ArrayList<>();
            Map<HashId, Contract> contractHashesMap = new ConcurrentHashMap<>();
            Client client = null;

            public void prepareClient() {
                try {
                    client = new Client(myKey, main.myInfo, null);
                } catch (Exception e) {
                    System.out.println("prepareClient exception: " + e.toString());
                }
            }

            public void prepareContracts() throws Exception {
                contractList = new ArrayList<>();
                for (int iContract = 0; iContract < CONTRACTS_PER_THREAD; ++iContract) {
                    Contract testContract = new Contract(myKey);
                    for (int i = 0; i < 10; i++) {
                        Contract nc = new Contract(myKey);
                        nc.seal();
                        testContract.addNewItems(nc);
                    }
                    testContract.seal();
                    assertTrue(testContract.isOk());
                    contractList.add(testContract);
                    contractHashesMap.put(testContract.getId(), testContract);
                }
            }

            private void sendContractsToRegister() throws Exception {
                for (int i = 0; i < contractList.size(); ++i) {
                    Contract contract = contractList.get(i);
                    client.register(contract.getPackedTransaction());
                }
            }

            private void waitForContracts() throws Exception {
                while (contractHashesMap.size() > 0) {
                    Thread.currentThread().sleep(300);
                    for (HashId id : contractHashesMap.keySet()) {
                        ItemResult itemResult = client.getState(id);
                        if (!itemResult.state.isPending())
                            contractHashesMap.remove(id);
                        else
                            break;
                    }
                }
            }

            @Override
            public void run() {
                try {
                    sendContractsToRegister();
                    waitForContracts();
                } catch (Exception e) {
                    System.out.println("runnable exception: " + e.toString());
                }
            }
        }

        System.out.println("singlethread test prepare...");
        TestRunnable runnableSingle = new TestRunnable();
        Thread threadSingle = new Thread(() -> {
            runnableSingle.threadNum = 0;
            runnableSingle.run();
        });
        runnableSingle.prepareClient();
        runnableSingle.prepareContracts();
        System.out.println("singlethread test start...");
        long t1 = new Date().getTime();
        threadSingle.start();
        threadSingle.join();
        long t2 = new Date().getTime();
        long dt = t2 - t1;
        long singleThreadTime = dt;
        System.out.println("singlethread test done!");

        System.out.println("multithread test prepare...");
        List<Thread> threadsList = new ArrayList<>();
        List<Thread> threadsPrepareList = new ArrayList<>();
        List<TestRunnable> runnableList = new ArrayList<>();
        for (int iThread = 0; iThread < THREADS_COUNT; ++iThread) {
            TestRunnable runnableMultithread = new TestRunnable();
            final int threadNum = iThread + 1;
            Thread threadMultiThread = new Thread(() -> {
                runnableMultithread.threadNum = threadNum;
                runnableMultithread.run();
            });
            Thread threadPrepareMultiThread = new Thread(() -> {
                try {
                    runnableMultithread.prepareContracts();
                } catch (Exception e) {
                    System.out.println("prepare exception: " + e.toString());
                }
            });
            runnableMultithread.prepareClient();
            threadsList.add(threadMultiThread);
            threadsPrepareList.add(threadPrepareMultiThread);
            runnableList.add(runnableMultithread);
        }
        for (Thread thread : threadsPrepareList)
            thread.start();
        for (Thread thread : threadsPrepareList)
            thread.join();
        Thread.sleep(500);
        System.out.println("multithread test start...");
        t1 = new Date().getTime();
        for (Thread thread : threadsList)
            thread.start();
        for (Thread thread : threadsList)
            thread.join();
        t2 = new Date().getTime();
        dt = t2 - t1;
        long multiThreadTime = dt;
        System.out.println("multithread test done!");

        Double tpsSingleThread = (double) CONTRACTS_PER_THREAD / (double) singleThreadTime * 1000.0;
        Double tpsMultiThread = (double) CONTRACTS_PER_THREAD * (double) THREADS_COUNT / (double) multiThreadTime * 1000.0;
        Double boostRate = tpsMultiThread / tpsSingleThread;

        System.out.println("\n === total ===");
        System.out.println("singleThread: " + (CONTRACTS_PER_THREAD) + " for " + singleThreadTime + "ms, tps=" + String.format("%.2f", tpsSingleThread));
        System.out.println("multiThread(N=" + THREADS_COUNT + "): " + (CONTRACTS_PER_THREAD * THREADS_COUNT) + " for " + multiThreadTime + "ms, tps=" + String.format("%.2f", tpsMultiThread));
        System.out.println("boostRate: " + String.format("%.2f", boostRate));
        System.out.println("\n");

        mm.forEach(x -> x.shutdown());
    }

    @Ignore
    @Test
    public void localNetwork3() throws Exception {
        List<Main> mm = new ArrayList<>();
        for (int i = 0; i < 4; i++)
            mm.add(createMain("node" + (i + 1), false));
        Main main = mm.get(0);
        assertEquals("http://localhost:8080", main.myInfo.internalUrlString());
        assertEquals("http://localhost:8080", main.myInfo.publicUrlString());
        PrivateKey myKey = new PrivateKey(Do.read("./src/test_contracts/keys/tu_key.private.unikey"));

        Set<PrivateKey> fromPrivateKeys = new HashSet<>();
        fromPrivateKeys.add(myKey);

        //Client client = new Client(myKey, main.myInfo, null);

        final long CONTRACTS_PER_THREAD = 10;
        final long THREADS_COUNT = 4;

        LogPrinter.showDebug(true);

        class TestRunnable implements Runnable {

            public int threadNum = 0;
            List<Parcel> contractList = new ArrayList<>();
            Map<HashId, Parcel> contractHashesMap = new ConcurrentHashMap<>();
            Client client = null;

            public void prepareClient() {
                try {
                    client = new Client(myKey, main.myInfo, null);
                } catch (Exception e) {
                    System.out.println("prepareClient exception: " + e.toString());
                }
            }

            public void prepareContracts() throws Exception {
                contractList = new ArrayList<>();
                for (int iContract = 0; iContract < CONTRACTS_PER_THREAD; ++iContract) {
                    Contract testContract = new Contract(myKey);
                    for (int i = 0; i < 10; i++) {
                        Contract nc = new Contract(myKey);
//                        nc.seal();
                        testContract.addNewItems(nc);
                    }
                    testContract.seal();
                    assertTrue(testContract.isOk());
                    Parcel parcel = createParcelWithFreshTU(client, testContract,Do.listOf(myKey));
                    contractList.add(parcel);
                    contractHashesMap.put(parcel.getId(), parcel);
                }
            }

            private void sendContractsToRegister() throws Exception {
                for (int i = 0; i < contractList.size(); ++i) {
                    Parcel parcel = contractList.get(i);
                    client.registerParcel(parcel.pack());
                }
            }

            private void waitForContracts() throws Exception {
                while (contractHashesMap.size() > 0) {
                    Thread.currentThread().sleep(100);
                    for (Parcel p : contractHashesMap.values()) {
                        ItemResult itemResult = client.getState(p.getPayloadContract().getId());
                        if (!itemResult.state.isPending())
                            contractHashesMap.remove(p.getId());
                    }
                }
            }

            @Override
            public void run() {
                try {
                    sendContractsToRegister();
                    waitForContracts();
                } catch (Exception e) {
                    System.out.println("runnable exception: " + e.toString());
                }
            }
        }

        System.out.println("singlethread test prepare...");
        TestRunnable runnableSingle = new TestRunnable();
        Thread threadSingle = new Thread(() -> {
            runnableSingle.threadNum = 0;
            runnableSingle.run();
        });
        runnableSingle.prepareClient();
        runnableSingle.prepareContracts();
        System.out.println("singlethread test start...");
        long t1 = new Date().getTime();
        threadSingle.start();
        threadSingle.join();
        long t2 = new Date().getTime();
        long dt = t2 - t1;
        long singleThreadTime = dt;
        System.out.println("singlethread test done!");

        System.out.println("multithread test prepare...");
        List<Thread> threadsList = new ArrayList<>();
        List<Thread> threadsPrepareList = new ArrayList<>();
        List<TestRunnable> runnableList = new ArrayList<>();
        for (int iThread = 0; iThread < THREADS_COUNT; ++iThread) {
            TestRunnable runnableMultithread = new TestRunnable();
            final int threadNum = iThread + 1;
            Thread threadMultiThread = new Thread(() -> {
                runnableMultithread.threadNum = threadNum;
                runnableMultithread.run();
            });
            Thread threadPrepareMultiThread = new Thread(() -> {
                try {
                    runnableMultithread.prepareContracts();
                } catch (Exception e) {
                    System.out.println("prepare exception: " + e.toString());
                }
            });
            runnableMultithread.prepareClient();
            threadsList.add(threadMultiThread);
            threadsPrepareList.add(threadPrepareMultiThread);
            runnableList.add(runnableMultithread);
        }
        for (Thread thread : threadsPrepareList)
            thread.start();
        for (Thread thread : threadsPrepareList)
            thread.join();
        Thread.sleep(500);
        System.out.println("multithread test start...");
        t1 = new Date().getTime();
        for (Thread thread : threadsList)
            thread.start();
        for (Thread thread : threadsList)
            thread.join();
        t2 = new Date().getTime();
        dt = t2 - t1;
        long multiThreadTime = dt;
        System.out.println("multithread test done!");

        Double tpsSingleThread = (double) CONTRACTS_PER_THREAD / (double) singleThreadTime * 1000.0;
        Double tpsMultiThread = (double) CONTRACTS_PER_THREAD * (double) THREADS_COUNT / (double) multiThreadTime * 1000.0;
        Double boostRate = tpsMultiThread / tpsSingleThread;

        System.out.println("\n === total ===");
        System.out.println("singleThread: " + (CONTRACTS_PER_THREAD) + " for " + singleThreadTime + "ms, tps=" + String.format("%.2f", tpsSingleThread));
        System.out.println("multiThread(N=" + THREADS_COUNT + "): " + (CONTRACTS_PER_THREAD * THREADS_COUNT) + " for " + multiThreadTime + "ms, tps=" + String.format("%.2f", tpsMultiThread));
        System.out.println("boostRate: " + String.format("%.2f", boostRate));
        System.out.println("\n");

        mm.forEach(x -> x.shutdown());
    }

    public void testSomeWork(Runnable someWork) throws Exception {
        final long THREADS_COUNT_MAX = Runtime.getRuntime().availableProcessors();

        System.out.println("warm up...");
        Thread thread0 = new Thread(someWork);
        thread0.start();
        thread0.join();

        long t1 = new Date().getTime();
        Thread thread1 = new Thread(someWork);
        thread1.start();
        thread1.join();
        long t2 = new Date().getTime();
        long singleTime = t2 - t1;
        System.out.println("single: " + singleTime + "ms");

        for (int THREADS_COUNT = 2; THREADS_COUNT <= THREADS_COUNT_MAX; ++THREADS_COUNT) {
            t1 = new Date().getTime();
            List<Thread> threadList = new ArrayList<>();
            for (int n = 0; n < THREADS_COUNT; ++n) {
                Thread thread = new Thread(someWork);
                threadList.add(thread);
                thread.start();
            }
            for (Thread thread : threadList)
                thread.join();
            t2 = new Date().getTime();
            long multiTime = t2 - t1;
            double boostRate = (double) THREADS_COUNT / (double) multiTime * (double) singleTime;
            System.out.println("multi(N=" + THREADS_COUNT + "): " + multiTime + "ms,   boostRate: x" + String.format("%.2f", boostRate));
        }
    }

    public static long idealConcurrentWork() {
        long s = 0l;
        for (int i = 0; i < 100000000; ++i)
            s += i;
        return s;
    }

    @Ignore
    @Test //
    public void testBossPack() throws Exception {
        byte[] br = new byte[200];
        new Random().nextBytes(br);
        Runnable r = () -> {
            try {
                Boss.Writer w = new Boss.Writer();
                for (int i = 0; i < 1000000; ++i) {
                    w.writeObject(br);
                    br[0]++;
                }
                w.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
        testSomeWork(() -> {
            r.run();
        });
        Thread.sleep(1500);
        testSomeWork(() -> {
            r.run();
        });
    }

    @Ignore
    @Test //OK no assert
    public void testContractCheck() throws Exception {
        PrivateKey key = TestKeys.privateKey(3);
        testSomeWork(() ->  {
            try {
                Contract c = new Contract(key);
                for (int k = 0; k < 500; k++) {
                    Contract nc = new Contract(key);
                    nc.seal();
                    c.addNewItems(nc);
                }
                c.seal();
                c.check();
            } catch (Quantiser.QuantiserException e) {
                e.printStackTrace();
            }
        });
    }

    @Ignore
    @Test //OK, no assert
    public void testIdealConcurrentWork() throws Exception {
        testSomeWork(() -> {
            for (int i = 0; i < 100; ++i)
                idealConcurrentWork();
        });
    }

    @Ignore
    @Test // OK, no assert
    public void testNewContractSeal() throws Exception {
        testSomeWork(() -> {
            for (int i = 0; i < 10; ++i) {
                PrivateKey myKey = null;
                try {
                    myKey = TestKeys.privateKey(3);
                } catch (Exception e) {
                }
                Contract testContract = new Contract(myKey);
                for (int iContract = 0; iContract < 10; ++iContract) {
                    Contract nc = new Contract(myKey);
                    nc.seal();
                    testContract.addNewItems(nc);
                }
                testContract.seal();
            }
        });
    }

    @Ignore
    @Test //OK, no assert
    public void testHashId() throws Exception {
        testSomeWork(() -> {
            byte[] randBytes = Do.randomBytes(1*1024*1024);
            for (int i = 0; i < 100; ++i)
                HashId.of(randBytes);
        });
    }

}
