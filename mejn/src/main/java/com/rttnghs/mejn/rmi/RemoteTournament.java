package com.rttnghs.mejn.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * RMI interface for remote tournament evaluation.
 */
public interface RemoteTournament extends Remote {
    List<Double> runBracket(List<List<Integer>> genomeBracket, int games) throws RemoteException;
}
