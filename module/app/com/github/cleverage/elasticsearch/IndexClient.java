package com.github.cleverage.elasticsearch;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.node.NodeBuilder;
import play.Application;
import play.Logger;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

public class IndexClient {

    public static org.elasticsearch.node.Node node = null;

    public static org.elasticsearch.client.Client client = null;

    public static IndexConfig config;

    public IndexClient(Application application) {
        // ElasticSearch config load from application.conf
        this.config = new IndexConfig(application);
    }

    /**
     * Checks if is local mode.
     *
     * @return true, if is local mode
     */
    private boolean isLocalMode() {
        try {
            if (config.client == null) {
                return true;
            }
            if (config.client.equalsIgnoreCase("false") || config.client.equalsIgnoreCase("true")) {
                return true;
            }

            return config.local;
        } catch (Exception e) {
            Logger.error("Error! Starting in Local Model: %s", e);
            return true;
        }
    }

    public void start() throws Exception {
        // Start Node Builder

        ImmutableSettings.Builder settings = ImmutableSettings.settingsBuilder();

        // Check Model
        if (this.isLocalMode()) {
            Logger.info("ElasticSearch : Starting in Local Mode");

            // load settings on local mode
            if(config.localConfig != null) {
                Logger.info("Elasticsearch : load default settings from " + config.localConfig);
                settings.loadFromClasspath(config.localConfig);
            }
            settings.build();
            Logger.info("Elasticsearch : settings  " + settings.internalMap().toString());

            NodeBuilder nb = nodeBuilder().settings(settings).local(true).client(false).data(true);
            node = nb.node();
            client = node.client();
            Logger.info("ElasticSearch : Started in Local Mode");
        }
        else
        {
            settings.put("client.transport.sniff", true);
            if (config.clusterName != null) {
                settings.put("cluster.name", config.clusterName);
            }
            settings.build();

            Logger.info("ElasticSearch : Starting in Client Mode");
            TransportClient c = new TransportClient(settings);
            if (config.client == null) {
                throw new Exception("Configuration required - elasticsearch.client when local model is disabled!");
            }

            String[] hosts = config.client.trim().split(",");
            boolean done = false;
            for (String host : hosts) {
                String[] parts = host.split(":");
                if (parts.length != 2) {
                    throw new Exception("Invalid Host: " + host);
                }
                Logger.info("ElasticSearch : Client - Host: " + parts[0] + " Port: " + parts[1]);
                c.addTransportAddress(new InetSocketTransportAddress(parts[0], Integer.valueOf(parts[1])));
                done = true;
            }
            if (!done) {
                throw new Exception("No Hosts Provided for ElasticSearch!");
            }
            client = c;
            Logger.info("ElasticSearch : Started in Client Mode");
        }

        // Check Client
        if (client == null) {
            throw new Exception("ElasticSearch Client cannot be null - please check the configuration provided and the health of your ElasticSearch instances.");
        }
    }

    public void stop() throws Exception {
         client.close();

         if(node != null) {
             node.close();
         }
    }
}
