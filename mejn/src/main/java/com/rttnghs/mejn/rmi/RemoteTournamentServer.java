/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.rttnghs.mejn.rmi;

import com.rttnghs.mejn.de.RankingStrategyTournament;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;

/**
 * RMI server exposing RankingStrategyTournament.runBracket for remote evaluation.
 *
 * <p>Configuration:</p>
 * <ul>
 *   <li>Port is read from <code>rmi-default.properties</code> using {@link com.rttnghs.mejn.configuration.Config}.</li>
 *   <li>Hostname is set via the JVM argument <code>-Djava.rmi.server.hostname=...</code> (not from properties file).</li>
 * </ul>
 *
 * <p>To set the RMI server hostname, launch the JVM with:</p>
 * <pre>
 *   java -Djava.rmi.server.hostname=your.host.name ...
 * </pre>
 *
 * <p>The <code>rmi.host</code> property is no longer used.</p>
 */
public class RemoteTournamentServer extends UnicastRemoteObject implements RemoteTournament {
    private final RankingStrategyTournament tournament;

    public RemoteTournamentServer() throws RemoteException {
        super();
        this.tournament = new RankingStrategyTournament();
    }

    @Override
    public List<Double> runBracket(List<List<Integer>> genomeBracket, int games) throws RemoteException {
        return tournament.runBracket(genomeBracket, games);
    }

    static void main(String[] args) throws Exception {
        // Port is loaded from rmi-default.properties using Config
        int port = com.rttnghs.mejn.configuration.Config.configuration.getInt("rmi.port", 1099);

        // Hostname is set via -Djava.rmi.server.hostname JVM argument if needed
        String host = System.getProperty("java.rmi.server.hostname", "localhost");

        Registry registry = LocateRegistry.createRegistry(port);
        RemoteTournamentServer server = new RemoteTournamentServer();
        registry.rebind("RemoteTournament", server);
        System.out.println("RemoteTournamentServer bound on host " + host + " port " + port);
        // Keep running
        Thread.currentThread().join();
    }
}




