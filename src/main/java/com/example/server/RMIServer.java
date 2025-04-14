package com.example.server;



import com.example.auth.AuthService;
import com.example.auth.AuthServiceImpl;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RMIServer {
    private static final Logger LOGGER = Logger.getLogger(RMIServer.class.getName());
    private static final int PORT = 1099;

    public static void main(String[] args) {
        try {

            AuthService authService = new AuthServiceImpl();
            Registry registry = LocateRegistry.createRegistry(PORT);
            registry.rebind("AuthService", authService);

            LOGGER.info("Serveur RMI démarré sur le port " + PORT);
            LOGGER.info("Service d'authentification enregistré sous le nom 'AuthService'");

            System.out.println("Serveur d'authentification RMI prêt.");
            System.out.println("Appuyez sur Ctrl+C pour arrêter le serveur.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du démarrage du serveur RMI", e);
            System.err.println("Erreur du serveur RMI: " + e.getMessage());
        }
    }
}