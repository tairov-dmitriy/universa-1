/*
 * Copyright (c) 2017 Sergey Chernov, iCodici S.n.C, All Rights Reserved
 *
 * Written by Sergey Chernov <real.sergeych@gmail.com>
 *
 */

package com.icodici.universa.node2;

import com.icodici.crypto.KeyAddress;
import com.icodici.crypto.PrivateKey;
import com.icodici.crypto.PublicKey;
import com.icodici.universa.Core;
import com.icodici.universa.HashId;
import com.icodici.universa.contract.Contract;
import com.icodici.universa.node.PostgresLedger;
import com.icodici.universa.node.StateRecord;
import com.icodici.universa.node2.network.ClientHTTPServer;
import com.icodici.universa.node2.network.DatagramAdapter;
import com.icodici.universa.node2.network.NetworkV2;
import com.icodici.universa.node2.network.UDPAdapter;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import net.sergeych.tools.*;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

public class Main {
    public static final String NODE_VERSION = Core.VERSION;
    private PostgresLedger ledger;
    private OptionParser parser;
    private OptionSet options;
    public final Reporter reporter = new Reporter();

    private String NAME_STRING = "Universa node server v" + NODE_VERSION + "\n";

    private AsyncEvent eventReady = new AsyncEvent();
    public final BufferedLogger logger = new BufferedLogger(4096);
    private String configRoot;
    private Thread hookThread;
    private EnvCache envCache;

    public static void main(String[] args) {
        new Main(args);
    }

    public Main(String[] args) {

//        args = new String[]{"--test", "--config", "/Users/sergeych/dev/new_universa/universa_core/src/test_node_config_v2/node1"};

        Config.forceInit(Contract.class);
        Config.forceInit(ItemNotification.class);

        Runtime.getRuntime().addShutdownHook(hookThread = new Thread(() -> shutdown()));
//        LogPrinter.showDebug(true);

        parser = new OptionParser() {
            {
                acceptsAll(asList("?", "h", "help"), "show help").forHelp();
                acceptsAll(asList("config-to-db"), "converts file config to db").forHelp();
                acceptsAll(asList("c", "config"), "configuration file for the network")
                        .withRequiredArg().ofType(String.class)
                        .describedAs("config_file");
                acceptsAll(asList("d", "database"), "database connection url")
                        .withRequiredArg().ofType(String.class)
                        .describedAs("db_url");
                accepts("test", "intended to be used in integration tests");
                accepts("nolog", "do not buffer log messages (good for testing)");
                accepts("verbose", "sets verbose level to nothing, base or detail")
                        .withRequiredArg()
                        .ofType(String.class)
                        .describedAs("level");
                accepts("udp-verbose", "sets udp-verbose level to nothing, base or detail")
                        .withRequiredArg()
                        .ofType(String.class)
                        .describedAs("level");
                accepts("restart-socket", "restarts UDPAdapter: shutdown it and create new");
                accepts("shutdown", "delicate shutdown with rollback current processing contracts");
                accepts("version");
            }
        };
        try {
            options = parser.parse(args);
            if(options.has("version")) {
                System.out.println("Version "+NODE_VERSION);
                System.exit(0);
            }
            if (options.has("nolog")) {
                logger.interceptStdOut();
            } else
                logger.printTo(System.out, false);
//            logger.printTo(System.out, false);

            if (options.has("?")) {
                usage(null);
            }
            log(NAME_STRING);


            if(options.has("config")) {
                loadNodeConfig();
                loadNetConfig();

                ledger.saveConfig(myInfo,netConfig,nodeKey);
            } else if(options.has("database")) {
                ledger = new PostgresLedger((String) options.valueOf("database"));
                log("ledger constructed");
                Object[] result = ledger.loadConfig();
                myInfo = (NodeInfo) result[0];
                netConfig = (NetConfig) result[1];
                nodeKey = (PrivateKey) result[2];

                log("key loaded: " + nodeKey.info());
                log("node local URL: " + myInfo.publicUrlString());
                log("node info: " + myInfo.toBinder());


            } else if(options.has("verbose")) {
                String lvl = (String) options.valueOf("verbose");
                int lvlId = 0;
                if("nothing".equals(lvl)) {
                    lvlId = DatagramAdapter.VerboseLevel.NOTHING;
                } else if("base".equals(lvl)) {
                    lvlId = DatagramAdapter.VerboseLevel.BASE;
                } else if("detail".equals(lvl)) {
                    lvlId = DatagramAdapter.VerboseLevel.DETAILED;
                }
                setVerboseLevel(lvlId);
            } else if(options.has("udp-verbose")) {
                String lvl = (String) options.valueOf("udp-verbose");
                int lvlId = 0;
                if("nothing".equals(lvl)) {
                    lvlId = DatagramAdapter.VerboseLevel.NOTHING;
                } else if("base".equals(lvl)) {
                    lvlId = DatagramAdapter.VerboseLevel.BASE;
                } else if("detail".equals(lvl)) {
                    lvlId = DatagramAdapter.VerboseLevel.DETAILED;
                }
                setUDPVerboseLevel(lvlId);
            } else if(options.has("restart-socket")) {
                restartUDPAdapter();
            } else if(options.has("shutdown")) {
                shutdown();
            } else {
                System.err.println("Neither config no database option passed, leaving");
                return;
            }

            log("--------------- step 3 --------------------");
            log("Starting the client HTTP server...");
            startClientHttpServer();

            log("--------------- step 4 --------------------");
            log("Starting the Universa node service...");
            startNode();

            log("all initialization is done -----------------------------------");
            startAndWaitEnd();
        } catch (OptionException e) {
            usage("Unrecognized parameter: " + e.getMessage());
        } catch (InterruptedException e) {
            e.printStackTrace();
            log("interrupted exception, leaving");
            System.err.println("interrupted exception, leaving");
        } catch (Exception e) {
            log("exception " + e);
            logger.e("exception " + e);
            e.printStackTrace();
            usage(e.getMessage());
        }
    }

    public NetConfig netConfig;
    public NetworkV2 network;
    public final Config config = new Config();
    private void loadNetConfig() throws IOException {
        netConfig = new NetConfig(configRoot + "/config/nodes");
        log("Network configuration is loaded from " + configRoot + ", " + netConfig.size() + " nodes.");
    }

    private void startNode() throws SQLException, IOException {

        //Minimum size of network is 10. Smaller configurations are treated as test networks.
        //For such configurations we are using table data rather than formula
        config.setConsensusConfigUpdater((config, n) -> {
            int negative;
            int positive;
            if(n < 3) {
                negative = 1;
                positive = n;
            } else if(n < 10) {
                negative = 2;
                positive = n-1;
            } else {
                positive = (int) Math.ceil(n * 0.9);
                negative = n + 1 - positive;
            }

            int resyncBreak = (int) Math.ceil(n * 0.2);
            if (resyncBreak < 1)
                resyncBreak = 1;
            if( resyncBreak+positive == n)
                resyncBreak += 1;

            log(myInfo.getNumber() + ": Network consensus is set to (negative/positive/resyncBreak): " + negative + " / " + positive + " / " + resyncBreak);
            config.setPositiveConsensus(positive);
            config.setNegativeConsensus(negative);
            config.setResyncBreakConsensus(resyncBreak);
        });

        network = new NetworkV2(netConfig, myInfo, nodeKey);
        node = new Node(config, myInfo, ledger, network, nodeKey,new File(configRoot+"/config/contracts"));
        cache = node.getCache();
        parcelCache = node.getParcelCache();
        paidOperationCache = node.getPaidOperationCache();
        envCache = node.getEnvCache();

        StateRecord r = ledger.getRecord(HashId.withDigest("bS/c4YMidaVuzTBhHLkGPFAvPbZQHybzQnXAoBwaZYM8eLYb7mAkVYEpuqKRXYc7anqX47BeNdvFN1n7KluH9A=="));
        if( r != null )
            r.destroy();

        clientHTTPServer.setConfig(config);
        clientHTTPServer.setNode(node);
        clientHTTPServer.setCache(cache);
        clientHTTPServer.setParcelCache(parcelCache);
        clientHTTPServer.setPaidOperationCache(paidOperationCache);
        clientHTTPServer.setEnvCache(envCache);
        clientHTTPServer.setLocalCors(myInfo.getPublicHost().equals("localhost"));
    }

    /**
     * To use in unti-tests. Start a node and blocks the thread until the nodes stops.
     *
     * @throws InterruptedException
     */
    private void startAndWaitEnd() throws InterruptedException {
        eventReady.fire(null);
        if (!options.has("test"))
            synchronized (parser) {
                parser.wait();
            }
    }

    /**
     * For unit-tests. Blocks until the node is initialized.
     *
     * @throws InterruptedException for unexpected interrupt
     */
    public void waitReady() throws InterruptedException {
        eventReady.await();
    }

    /**
     * Request the shutdown and wait until the node stops.
     */
    public void shutdown() {
        try {
            if (hookThread != null)
                Runtime.getRuntime().removeShutdownHook(hookThread);
//            network.close();
            ledger.close();
            log("shutting down");
            network.shutdown();
            clientHTTPServer.shutdown();
        } catch (Exception e) {
        }

        synchronized (parser) {
            parser.notifyAll();
        }
        try {
            logger.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Set verbose level via {@link  DatagramAdapter.VerboseLevel}
     *
     * @param level is {@link DatagramAdapter.VerboseLevel#NOTHING}, {@link DatagramAdapter.VerboseLevel#BASE} or
     * {@link DatagramAdapter.VerboseLevel#DETAILED}
     */
    public void setVerboseLevel(int level) {
        network.setVerboseLevel(level);
        node.setVerboseLevel(level);
    }

    /**
     * Set UDP verbose level via {@link  DatagramAdapter.VerboseLevel}
     *
     * @param level is {@link DatagramAdapter.VerboseLevel#NOTHING}, {@link DatagramAdapter.VerboseLevel#BASE} or
     * {@link DatagramAdapter.VerboseLevel#DETAILED}
     */
    public void setUDPVerboseLevel(int level) {
        network.setUDPVerboseLevel(level);
    }

    /**
     * Set verbose level via {@link  UDPAdapter}
     */
    public void restartUDPAdapter() {
        try {
            network.restartUDPAdapter();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private PrivateKey nodeKey;
    //private Binder settings;

    public PublicKey getNodePublicKey() {
        return nodeKey.getPublicKey();
    }

    public NodeInfo myInfo;

    private void loadNodeConfig() throws IOException, SQLException {
        Yaml yaml = new Yaml();
        configRoot = (String) options.valueOf("config");

        nodeKey = null;
        Binder settings = Binder.of(yaml.load(new FileInputStream(configRoot + "/config/config.yaml")));

        Binder settingsShared;
        if(new File(configRoot + "/config/shared.yaml").exists()) {
            settingsShared = Binder.of(yaml.load(new FileInputStream(configRoot + "/config/shared.yaml")));
        } else {
            settingsShared = settings;
        }

        log("node settings: " + settings);
        String nodeKeyFileName = configRoot + "/tmp/" + settings.getStringOrThrow("node_name") + ".private.unikey";
        log(nodeKeyFileName);
        nodeKey = new PrivateKey(Do.read(nodeKeyFileName));

        myInfo = new NodeInfo(nodeKey.getPublicKey(),
                              settings.getIntOrThrow("node_number"),
                              settings.getStringOrThrow("node_name"),
                              (String) settings.getListOrThrow("ip").get(0),
                              settings.containsKey("ipv6") ? (String) settings.getListOrThrow("ipv6").get(0) : null,
                              settings.getStringOrThrow("public_host"),
                              settings.getIntOrThrow("udp_server_port"),
                              settings.getIntOrThrow("http_client_port"),
                              settings.getIntOrThrow("http_server_port"),
                              settings.getInt("public_http_port",8080)
        );

        config.setIsFreeRegistrationsAllowedFromYaml(settingsShared.getBoolean("allow_free_registrations", false));
        config.setPermanetMode(settingsShared.getBoolean("permanet_mode", false));
        List<Object> whiteList = settingsShared.getList("whitelist", new ArrayList<>());
        for(Object value : whiteList) {
            try {
                config.getAddressesWhiteList().add(new KeyAddress((String) value));
            } catch (KeyAddress.IllegalAddressException e) {
                e.printStackTrace();
            }
        }

        if(settings.containsKey("disk_cache_age")) {
            int days = settings.getIntOrThrow("disk_cache_age");
            log("using extended disk cache age: " + days + " days");
            config.setMaxDiskCacheAge(Duration.ofDays(days));
        }

        ledger = new PostgresLedger(settings.getStringOrThrow("database"));
        log("ledger constructed");

        log("key loaded: " + nodeKey.info());
        log("node local URL: " + myInfo.serverUrlString());
        log("node public URL: " + myInfo.publicUrlString());
        log("node info: " + myInfo.toBinder());
    }

    private ClientHTTPServer clientHTTPServer;

    public Node node;
    public ItemCache cache = new ItemCache(Duration.ofMinutes(30));
    public ParcelCache parcelCache = new ParcelCache(Duration.ofMinutes(30));
    public PaidOperationCache paidOperationCache = new PaidOperationCache(Duration.ofMinutes(30));


    private void startClientHttpServer() throws Exception {
        if(myInfo.getClientAddress().getPort() == 6003) {
            int a = 0;
            a++;
        }
        log("prepare to start client HTTP server on " + myInfo.getClientAddress().getPort());

        clientHTTPServer = new ClientHTTPServer(nodeKey, myInfo.getClientAddress().getPort(), logger);
        clientHTTPServer.setCache(cache);
        clientHTTPServer.setParcelCache(parcelCache);
        clientHTTPServer.setPaidOperationCache(paidOperationCache);
        clientHTTPServer.setNetConfig(netConfig);
//        node = new Node()
    }

    private void log(String msg) {
        logger.log(msg);
    }

    private void usage(String text) {
        log("usafe called");
        boolean error = false;
        PrintStream out = System.out;
        if (text != null) {
            out = System.err;
            error = true;
        }
        out.println("\n" + NAME_STRING);
        if (text != null)
            out.println("ERROR: " + text + "\n");
        try {
            parser.printHelpOn(out);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (options != null && !options.has("test"))
            System.exit(100);
    }
}
