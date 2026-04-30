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

    public static void main(String[] args) throws Exception {
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




