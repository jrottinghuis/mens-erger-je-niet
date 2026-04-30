package com.rttnghs.mejn.rmi;

import org.junit.jupiter.api.Test;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Minimal integration test for RemoteTournamentServer RMI call chain.
 * Assumes the server is already running on localhost:1099.
 */
public class RemoteTournamentServerIT {
    @Test
    void testRunBracketRmiCall() throws Exception {
        // Port is loaded from rmi-default.properties using Config
        int port = com.rttnghs.mejn.configuration.Config.configuration.getInt("rmi.port", 1099);
        String host = System.getProperty("java.rmi.server.hostname", "localhost");

        Registry registry = LocateRegistry.getRegistry(host, port);
        RemoteTournament remote = (RemoteTournament) registry.lookup("RemoteTournament");
        List<List<Integer>> bracket = Arrays.asList(Arrays.asList(0, 1), Arrays.asList(2, 3));
        int games = 1;
        List<Double> result = remote.runBracket(bracket, games);
        assertNotNull(result);
        assertEquals(bracket.size(), result.size());
    }
}



