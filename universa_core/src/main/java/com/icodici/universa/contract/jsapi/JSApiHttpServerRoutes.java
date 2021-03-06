package com.icodici.universa.contract.jsapi;

import com.icodici.universa.HashId;
import com.icodici.universa.contract.Contract;
import net.sergeych.tools.JsonTool;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class JSApiHttpServerRoutes {

    private int portToListen = 8080;
    private Map<String, RouteModel> routes = new HashMap<>();

    public JSApiHttpServerRoutes() {
    }

    public JSApiHttpServerRoutes(String routesJsonFilePath, JSApiHttpServer.ISlot1Requestor slot1Requestor) throws IOException {
        this(Files.readAllBytes(Paths.get(routesJsonFilePath)), slot1Requestor);
    }

    public JSApiHttpServerRoutes(byte[] routeBytes, JSApiHttpServer.ISlot1Requestor slot1Requestor) throws IOException {
        HashMap mapRoutesConfig = JsonTool.fromJson(new String(routeBytes));
        portToListen = Integer.parseInt((String)mapRoutesConfig.get("listenPort"));
        List listRoutes = (List)mapRoutesConfig.get("routes");
        for (Object routeObj : listRoutes) {
            HashMap route = (HashMap) routeObj;
            String endpoint = (String)route.get("endpoint");
            String handlerName = (String)route.get("handlerName");
            String contractPath = (String)route.get("contractPath");
            String scriptName = (String)route.get("scriptName");
            String slotIdStr = (String)route.get("slotId");
            HashId slotId = slotIdStr==null ? null : HashId.withDigest(slotIdStr);
            String originIdStr = (String)route.get("originId");
            HashId originId = originIdStr==null ? null : HashId.withDigest(originIdStr);
            ArrayList<String> jsApiParamsList = (ArrayList<String>)route.get("jsApiParams");
            String[] jsApiParams = jsApiParamsList == null ? null : jsApiParamsList.toArray(new String[0]);
            Contract contract = null;
            if (contractPath != null) {
                if (contractPath.startsWith("http://") || contractPath.startsWith("https://")) {
                    URL contractUrl = new URL(contractPath);
                    URLConnection contractUrlConnection = contractUrl.openConnection();
                    try {
                        int contractBinLength = contractUrlConnection.getInputStream().available();
                        if (contractBinLength > 2 * 1024 * 1024)
                            throw new IllegalArgumentException("JSApiHttpServerRoutes error: remote contract too large (" + contractPath + ")");
                        byte[] contractBytes = new byte[contractBinLength];
                        contractUrlConnection.getInputStream().read(contractBytes);
                        contract = Contract.fromPackedTransaction(contractBytes);
                    } finally {
                        contractUrlConnection.getInputStream().close();
                    }
                } else {
                    contract = Contract.fromPackedTransaction(Files.readAllBytes(Paths.get(contractPath)));
                }
            }
            else if (slotIdStr != null && originIdStr != null)
                contract = Contract.fromPackedTransaction(slot1Requestor.queryContract(slotId, originId));
            if (contract == null)
                throw new IllegalArgumentException("JSApiHttpServerRoutes error: you must specify either contractPath or slotId+originId");
            addNewRoute(endpoint, handlerName, contract, scriptName, jsApiParams, slotId);
        }
    }

    public void addNewRoute(String endpoint, String handlerMethodName, Contract contract, String scriptName, String[] jsParams, HashId slotId) {
        RouteModel prev = routes.putIfAbsent(endpoint, new RouteModel(endpoint, handlerMethodName, contract, scriptName, slotId, jsParams));
        if (prev != null)
            throw new IllegalArgumentException("JSApiHttpServerRoutes error: endpoint duplicates");
    }

    public void addNewRoute(String endpoint, String handlerMethodName, Contract contract, String scriptName, String[] jsParams) {
        addNewRoute(endpoint, handlerMethodName, contract, scriptName, jsParams, null);
    }

    public void setPortToListen(int newValue) {
        this.portToListen = newValue;
    }

    public int getPortToListen() {
        return portToListen;
    }

    public void forEach(BiConsumer<String, RouteModel> lambda) {
        routes.forEach(lambda);
    }

    class RouteModel {
        public String endpoint;
        public String handlerMethodName;
        public Contract contract;
        public String scriptName;
        public HashId slotId;
        public String[] jsParams;
        public RouteModel(String endpoint, String handlerMethodName, Contract contract, String scriptName, HashId slotId, String[] jsParams) {
            this.endpoint = endpoint;
            this.handlerMethodName = handlerMethodName;
            this.contract = contract;
            this.scriptName = scriptName;
            this.slotId = slotId;
            this.jsParams = jsParams;
        }
    }
}
