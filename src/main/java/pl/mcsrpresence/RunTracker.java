package pl.mcsrpresence;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

public final class RunTracker {
    private static IntegratedServer runServer;
    private static Stage stage = Stage.WAITING;
    private static RegistryKey<World> lastDimension;
    private static boolean visitedNether;
    private static boolean postNether;
    private static long enteredNetherAt;
    private static long returnedFromNetherAt;
    private static int scanCooldown;
    private static int bastionHits;
    private static int fortressHits;
    private static int strongholdHits;
    private static long runStartTimestamp;

    private RunTracker() {}

    public static void tick(MinecraftClient client) {
        ClientWorld world = client.world;
        if (world == null || client.player == null) {
            runServer = null;
            lastDimension = null;
            visitedNether = false;
            postNether = false;
            runStartTimestamp = 0L;
            resetHits();
            setStage(Stage.WAITING, false);
            return;
        }

        IntegratedServer server = client.getServer();
        if (server != null && server != runServer) {
            runServer = server;
            visitedNether = false;
            postNether = false;
            enteredNetherAt = 0L;
            returnedFromNetherAt = 0L;
            lastDimension = world.getRegistryKey();
            scanCooldown = 0;
            resetHits();
            runStartTimestamp = System.currentTimeMillis() / 1000L;
            setStage(Stage.OVERWORLD, true);
            return;
        }

        RegistryKey<World> dim = world.getRegistryKey();
        boolean changed = lastDimension != null && !dim.equals(lastDimension);
        long now = System.currentTimeMillis();

        if (dim.equals(World.END)) {
            setStage(Stage.END, false);
        } else if (dim.equals(World.NETHER)) {
            if (changed || !visitedNether) {
                enteredNetherAt = now;
                resetHits();
            }
            visitedNether = true;
            if (stage != Stage.BASTION && stage != Stage.FORTRESS) setStage(Stage.NETHER, false);
            if (now - enteredNetherAt >= 1500L) scanNether(world, client.player.getBlockPos());
        } else if (dim.equals(World.OVERWORLD)) {
            if (changed && lastDimension != null && lastDimension.equals(World.NETHER) && visitedNether) {
                postNether = true;
                returnedFromNetherAt = now;
                resetHits();
                setStage(Stage.PORTAL, false);
            } else if (postNether) {
                if (stage == Stage.PORTAL && now - returnedFromNetherAt >= 2000L) {
                    setStage(Stage.STRONGHOLD_SEARCH, false);
                } else if (stage != Stage.PORTAL && stage != Stage.STRONGHOLD && stage != Stage.STRONGHOLD_SEARCH) {
                    setStage(Stage.STRONGHOLD_SEARCH, false);
                }
                scanStronghold(world, client.player.getBlockPos());
            } else {
                setStage(Stage.OVERWORLD, false);
            }
        }
        lastDimension = dim;
    }

    private static void scanNether(ClientWorld world, BlockPos center) {
        if (scanCooldown-- > 0) return;
        scanCooldown = 4;
        int bastion = 0, fortress = 0;
        BlockPos.Mutable p = new BlockPos.Mutable();
        for (int y=-6; y<=6; y++) for (int x=-9; x<=9; x++) for (int z=-9; z<=9; z++) {
            p.set(center.getX()+x, center.getY()+y, center.getZ()+z);
            Block b = world.getBlockState(p).getBlock();
            if (b == Blocks.GILDED_BLACKSTONE) bastion += 40;
            else if (b == Blocks.POLISHED_BLACKSTONE_BRICKS || b == Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS
                    || b == Blocks.CHISELED_POLISHED_BLACKSTONE || b == Blocks.BLACKSTONE) bastion++;
            if (b == Blocks.NETHER_BRICKS || b == Blocks.NETHER_BRICK_FENCE || b == Blocks.NETHER_BRICK_STAIRS) fortress++;
        }
        bastionHits = bastion >= 70 ? bastionHits + 1 : 0;
        fortressHits = fortress >= 45 ? fortressHits + 1 : 0;
        if (bastionHits >= 2) setStage(Stage.BASTION, false);
        else if (fortressHits >= 2) setStage(Stage.FORTRESS, false);
    }

    private static void scanStronghold(ClientWorld world, BlockPos center) {
        if (scanCooldown-- > 0) return;
        scanCooldown = 4;
        int bricks = 0;
        boolean frame = false;
        BlockPos.Mutable p = new BlockPos.Mutable();
        for (int y=-6; y<=6; y++) for (int x=-9; x<=9; x++) for (int z=-9; z<=9; z++) {
            p.set(center.getX()+x, center.getY()+y, center.getZ()+z);
            Block b = world.getBlockState(p).getBlock();
            if (b == Blocks.END_PORTAL_FRAME) frame = true;
            if (b == Blocks.STONE_BRICKS || b == Blocks.CRACKED_STONE_BRICKS
                    || b == Blocks.MOSSY_STONE_BRICKS || b == Blocks.INFESTED_STONE_BRICKS
                    || b == Blocks.INFESTED_CRACKED_STONE_BRICKS || b == Blocks.INFESTED_MOSSY_STONE_BRICKS) bricks++;
        }
        strongholdHits = (frame || bricks >= 45) ? strongholdHits + 1 : 0;
        if (frame || strongholdHits >= 2) setStage(Stage.STRONGHOLD, false);
    }

    private static void resetHits() {
        bastionHits = fortressHits = strongholdHits = 0;
        scanCooldown = 0;
    }

    private static void setStage(Stage next, boolean force) {
        if (!force && next == stage) return;
        stage = next;
        DiscordPresence.update(stage, stage == Stage.WAITING ? 0L : runStartTimestamp);
    }
}
