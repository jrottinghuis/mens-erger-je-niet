package com.rttnghs.mejn.rmi;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Minimal integration test for RemoteTournamentServer RMI call chain.
 * Starts an RMI registry and registers a mock RemoteTournament implementation.
 */
public class RemoteTournamentServerIT {

    private Registry registry;
    private RemoteTournament mockRemoteTournament;

    @BeforeEach
    void setUp() throws Exception {
        // Start RMI registry
        registry = LocateRegistry.createRegistry(1099);

        // Create and export a mock RemoteTournament implementation
        mockRemoteTournament = new RemoteTournament() {
            @Override
            public List<Double> runBracket(List<List<Integer>> bracket, int games) {
                // Mock implementation: return a list of dummy results
                return Arrays.asList(1.0, 2.0);
            }
        };
        RemoteTournament stub = (RemoteTournament) UnicastRemoteObject.exportObject(mockRemoteTournament, 0);

        // Bind the mock implementation to the registry
        registry.rebind("RemoteTournament", stub);
    }

    @AfterEach
    void tearDown() throws Exception {
        // Unbind the mock implementation and unexport the object
        registry.unbind("RemoteTournament");
        UnicastRemoteObject.unexportObject(mockRemoteTournament, true);
        UnicastRemoteObject.unexportObject(registry, true);
    }

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
