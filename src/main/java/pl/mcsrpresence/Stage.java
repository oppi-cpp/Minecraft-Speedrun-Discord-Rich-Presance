package pl.mcsrpresence;

public enum Stage {
    WAITING("Waiting for run...", "overworld"),
    OVERWORLD("Overworld", "overworld"),
    NETHER("Nether", "nether"),
    BASTION("Bastion Remnant", "bastion"),
    FORTRESS("Nether Fortress", "fortress"),
    PORTAL("Second Portal", "portal"),
    STRONGHOLD_SEARCH("Searching for Stronghold", "eye_of_ender"),
    STRONGHOLD("Stronghold", "strongholdportalroom"),
    END("The End", "endstone");

    public final String label;
    public final String asset;
    Stage(String label, String asset) { this.label = label; this.asset = asset; }
}
