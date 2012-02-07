package de.minestar.sixteenblocks.Manager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import de.minestar.sixteenblocks.core.TextUtils;
import de.minestar.sixteenblocks.units.StructureBlock;
import de.minestar.sixteenblocks.units.ZoneXZ;

public class AreaManager {
    private static int areaSizeX = 32, areaSizeZ = 32;
    private static int minimumBuildY = 5;
    private static int baseY = 4;

    private HashMap<String, SkinArea> areaList = new HashMap<String, SkinArea>();

    // ////////////////////////////////////////////////
    //
    // CONSTRUCTOR
    //
    // ////////////////////////////////////////////////

    public AreaManager() {
        this.loadAreas();
    }

    // ////////////////////////////////////////////////
    //
    // PERSISTENCE
    //
    // ////////////////////////////////////////////////

    // TODO: PLACE PERSISTENCE-METHODS HERE
    // I THINK WE SHOULD USE MySQL
    private void loadAreas() {
        TextUtils.logInfo("Areas loaded.");
    }

    /**
     * EXPORT A STRUCTURE
     * 
     * @param world
     * @param structureName
     * @param thisZone
     * @return <b>true</b> if saving was successful, otherwise <b>false</b>
     */
    public boolean exportStructure(World world, String structureName, ZoneXZ thisZone) {
        ArrayList<StructureBlock> blockList = new ArrayList<StructureBlock>();
        int startX = thisZone.getX() * areaSizeX - (thisZone.getZ() % 2 != 0 ? (areaSizeX >> 1) : 0);
        int startZ = thisZone.getZ() * areaSizeZ;
        int startY = baseY;
        int ID;
        byte SubID;

        // FIRST WE GET ALL BLOCKS != AIR
        for (int x = 0; x < areaSizeX; x++) {
            for (int z = 0; z < areaSizeZ; z++) {
                for (int y = 0; y < 128 - startY; y++) {
                    ID = world.getBlockTypeIdAt(startX + x, startY + y, startZ + z);
                    SubID = world.getBlockAt(startX + x, startY + y, startZ + z).getData();
                    if (ID != 0) {
                        blockList.add(new StructureBlock(x, y, z, ID, SubID));
                    }
                }
            }
        }

        // WE ONLY CONTINUE, IF WE FOUND AT LEAST ONE BLOCK
        if (blockList.size() < 1)
            return false;

        // CREATE THE BYTE ARRAYS
        byte[] xArray = new byte[blockList.size()];
        byte[] yArray = new byte[blockList.size()];
        byte[] zArray = new byte[blockList.size()];
        byte[] IDArray = new byte[blockList.size()];
        byte[] SubIDArray = new byte[blockList.size()];

        // FILL THE ARRAYS
        for (int i = 0; i < blockList.size(); i++) {
            xArray[i] = (byte) blockList.get(i).getX();
            yArray[i] = (byte) blockList.get(i).getY();
            zArray[i] = (byte) blockList.get(i).getZ();
            IDArray[i] = (byte) blockList.get(i).getTypeID();
            SubIDArray[i] = (byte) blockList.get(i).getSubID();
        }

        // CREATE DIRS AND DELETE OLD FILE
        File folder = new File("plugins/16Blocks/structures");
        folder.mkdirs();

        File file = new File(folder, structureName + ".dat");
        if (file.exists())
            file.delete();

        // FINALLY SAVE THE FILE
        try {
            // OPEN STREAMS
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);

            // WRITE DATA
            objectOutputStream.writeInt(xArray.length);
            objectOutputStream.writeObject(xArray);
            objectOutputStream.writeObject(yArray);
            objectOutputStream.writeObject(zArray);
            objectOutputStream.writeObject(IDArray);
            objectOutputStream.writeObject(SubIDArray);

            // CLOSE STREAMS
            objectOutputStream.close();
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public HashSet<StructureBlock> loadStructure(String structureName) {
        HashSet<StructureBlock> blockList = new HashSet<StructureBlock>();
        try {
            // CREATE DIRS
            File folder = new File("plugins/16Blocks/structures");
            folder.mkdirs();

            // CHECK FILE EXISTANCE
            File file = new File(folder, structureName + ".dat");
            if (!file.exists())
                return null;

            // OPEN STREAMS
            FileInputStream fileInputStream = new FileInputStream(file);
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);

            // READ LENGTH
            int length = objectInputStream.readInt();

            // CREATE THE BYTE ARRAYS
            byte[] xArray = new byte[length];
            byte[] yArray = new byte[length];
            byte[] zArray = new byte[length];
            byte[] IDArray = new byte[length];
            byte[] SubIDArray = new byte[length];

            // READ THE BYTE ARRAYS
            xArray = (byte[]) objectInputStream.readObject();
            yArray = (byte[]) objectInputStream.readObject();
            zArray = (byte[]) objectInputStream.readObject();
            IDArray = (byte[]) objectInputStream.readObject();
            SubIDArray = (byte[]) objectInputStream.readObject();

            // CLOSE STREAMS
            objectInputStream.close();
            fileInputStream.close();

            // CREATE THE BLOCKLIST
            for (int i = 0; i < length; i++) {
                blockList.add(new StructureBlock(xArray[i], yArray[i], zArray[i], IDArray[i], SubIDArray[i]));
            }

            // RETURN THE BLOCKLIST
            return blockList.size() > 0 ? blockList : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ////////////////////////////////////////////////
    //
    // MODIFY & GET AREAS
    //
    // ////////////////////////////////////////////////

    public SkinArea getArea(ZoneXZ thisZone) {
        return this.areaList.get(thisZone.toString());
    }

    public boolean containsArea(ZoneXZ thisZone) {
        return this.containsArea(thisZone.toString());
    }

    public boolean containsArea(String coordinateString) {
        return this.areaList.containsKey(coordinateString);
    }

    public boolean addArea(SkinArea skinArea) {
        if (this.containsArea(skinArea.getZoneXZ()))
            return false;
        this.areaList.put(skinArea.getZoneXZ().toString(), skinArea);
        return true;
    }

    public boolean removeArea(ZoneXZ thisZone) {
        return (this.areaList.remove(thisZone.toString()) != null);
    }

    public boolean removeArea(SkinArea thisArea) {
        return this.removeArea(thisArea.getZoneXZ());
    }

    public boolean removeArea(Player player) {
        SkinArea toDelete = null;
        for (SkinArea thisArea : this.areaList.values()) {
            if (thisArea.isAreaOwner(player)) {
                toDelete = thisArea;
                break;
            }
        }
        // DELETE IF FOUND
        if (toDelete != null)
            return this.removeArea(toDelete);
        return false;
    }

    // ////////////////////////////////////////////////
    //
    // IS IN AREA
    //
    // ////////////////////////////////////////////////

    public boolean isInArea(Player player, Location location) {
        return this.isInArea(player, location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public boolean isInArea(Player player, Block block) {
        return this.isInArea(player, block.getX(), block.getY(), block.getZ());
    }

    public boolean isInArea(Player player, int x, int y, int z) {
        if (y < AreaManager.minimumBuildY)
            return false;

        ZoneXZ thisZone = ZoneXZ.fromPoint(x, z);
        SkinArea thisArea = this.getArea(thisZone);
        if (thisArea == null) {
            return false;
        }
        return thisArea.isAreaOwner(player);
    }

    // ////////////////////////////////////////////////
    //
    // GETTER-METHODS
    //
    // ////////////////////////////////////////////////

    public static int getAreaSizeX() {
        return areaSizeX;
    }

    public static int getAreaSizeZ() {
        return areaSizeZ;
    }

    public static int getMinimumY() {
        return minimumBuildY;
    }
}
