package dbms;

public class MainLauncher {
    public static void main(String[] args) {
        // Force complete software rendering and disable any GPU/D3D acceleration
        System.setProperty("prism.order", "sw");
        System.setProperty("prism.verbose", "true");
        System.setProperty("sun.java2d.d3d", "false");
        System.setProperty("prism.dirtyopts", "false");
        System.setProperty("glass.accessible.force", "false");
        
        System.out.println("Main: Hardware acceleration DISABLED.");
        
        // Καλούμε την main της App κλάσης σου
        App.main(args);
    }
}
