package net.botwithus;


import java.util.*;
import java.util.regex.Pattern;

import net.botwithus.api.game.hud.inventories.Backpack;
import net.botwithus.internal.scripts.ScriptDefinition;
import net.botwithus.rs3.events.impl.ChatMessageEvent;
import net.botwithus.rs3.game.*;
import net.botwithus.rs3.game.cs2.ScriptBuilder;
import net.botwithus.rs3.game.cs2.layouts.Layout;
import net.botwithus.rs3.game.hud.interfaces.Component;
import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.game.js5.types.StructType;
import net.botwithus.rs3.game.js5.types.configs.ConfigManager;
import net.botwithus.rs3.game.queries.builders.components.ComponentQuery;
import net.botwithus.rs3.game.queries.builders.items.*;
import net.botwithus.rs3.game.queries.builders.objects.SceneObjectQuery;
import net.botwithus.rs3.game.scene.entities.characters.player.LocalPlayer;
import net.botwithus.rs3.game.scene.entities.object.SceneObject;
import net.botwithus.rs3.game.vars.VarManager;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.LoopingScript;
import net.botwithus.rs3.script.config.ScriptConfig;
import net.botwithus.rs3.game.Item;

public class BlancuhThroneRoom extends LoopingScript {

    private BotState botState = BotState.DEPOSITING;
    private boolean someBool = true;
    private String empoweredCrystal = "";
    private Random random = new Random();
    private int remainingXP = 800000;

    SceneObjectQuery currentEmpoweredQuery = SceneObjectQuery.newQuery().contains("(Empowered)").option("Deposit");
    SceneObjectQuery yellowCrateQuery = SceneObjectQuery.newQuery().contains("Crate of yellow crystals").option("Deposit");
    SceneObjectQuery greenCrateQuery = SceneObjectQuery.newQuery().contains("Crate of green crystals").option("Deposit");
    SceneObjectQuery blueCrateQuery = SceneObjectQuery.newQuery().contains("Crate of blue crystals").option("Deposit");
    SceneObjectQuery redCrateQuery = SceneObjectQuery.newQuery().contains("Crate of red crystals").option("Deposit");

    SceneObject currentEmpowered;
    SceneObject yellowCrate;
    SceneObject greenCrate;
    SceneObject blueCrate;
    SceneObject redCrate;

    ComponentQuery transmuteMenuTextQuery = ComponentQuery.newQuery(1370).componentIndex(29).subComponentIndex(3);
    ComponentQuery transmuteMenuInputQuery = ComponentQuery.newQuery(1370).componentIndex(30).subComponentIndex(-1);

    InventoryItemQuery senntistenCrystalQuery = InventoryItemQuery.newQuery(93).name("Senntisten crystal");
    int senntistenCrystalCount;

    private enum BotState {
        //define your own states here
        IDLE,
        WITHDRAWING,
        TRANSMUTING,
        DEPOSITING,
    }

    public BlancuhThroneRoom(String s, ScriptConfig scriptConfig, ScriptDefinition scriptDefinition) {
        super(s, scriptConfig, scriptDefinition);
        this.sgc = new BlancuhThroneRoomGraphicsContext(getConsole(), this);
        subscribe(ChatMessageEvent.class, chatMessageEvent -> {
            if (chatMessageEvent.getMessage().contains("Your item has cooled down slightly")) {
                println("Need to heatup!");
            }
        });
    }

    @Override
    public void onLoop() {
        LocalPlayer player = Client.getLocalPlayer();
        if (player == null || Client.getGameState() != Client.GameState.LOGGED_IN || botState == BotState.IDLE) {
            Execution.delay(random.nextLong(3000,7000));
            return;
        }
        setRemainingXP(800000 - VarManager.getVarbitValue(34277));
        if (getRemainingXP() <= 0) {
            return;
        }
        switch (botState) {
            case IDLE -> {
                println("We're idle!");
                Execution.delay(random.nextLong(1000,3000));
            }
            case WITHDRAWING -> Execution.delay(handleWithdrawing(player));
            case TRANSMUTING -> Execution.delay(handleTransmuting(player));
            case DEPOSITING  -> Execution.delay(handleDepositing(player));
        }
    }

    public long handleWithdrawing(LocalPlayer player) {
        if (player == null || Client.getGameState() != Client.GameState.LOGGED_IN || botState != BotState.WITHDRAWING) {
            Execution.delay(random.nextLong(3000,7000));
            return random.nextLong(1500,3000);
        }
        SceneObject storageBin = SceneObjectQuery.newQuery().name("Crystal storage bin").option("Withdraw").results().nearest();
        if (storageBin != null ) {
            println("Found storage bin.");
            println("Interacted storage bin: " + storageBin.interact("Withdraw"));
            Execution.delayUntil(3000, Backpack::isFull);
            setBotState(BotState.TRANSMUTING);
        } else {
            println("Did not find storage bin.");
        }
        return random.nextLong(1500,3000);
    }

    private boolean handleTransmuteMenu() {
        Component menuText = transmuteMenuTextQuery.results().first();
        if (menuText == null) {
            return false;
        }
        if (menuText.getText() == null) {
            return false;
        }
        if (Objects.equals(Objects.requireNonNull(transmuteMenuTextQuery.results().first()).getText(), "Weave")) {
//            println("Transmute window open");
//            if(Objects.requireNonNull(transmuteMenuInputQuery.results().first()).interact(1)){
//                println("Transmute option pressed");
//                return true;
//            } else {
//                println("Transmutes option failed.");
//                return false;
//            }
            return Objects.requireNonNull(transmuteMenuInputQuery.results().first()).interact(1);
        }
//        print("Transmute window failed to open");
        return false;
    }

    private boolean handleDestroyMenu() {
        ComponentQuery itemToBeDestroyed = ComponentQuery.newQuery(1183).componentIndex(4).subComponentIndex(-1);
        if (itemToBeDestroyed.results().first() == null) {
            return false;
        }
        if (itemToBeDestroyed.results().first().getText() == null) {
            return false;
        }
        if (Objects.equals(Objects.requireNonNull(itemToBeDestroyed.results().first()).getText(), "Senntisten crystal")) {
            println("Destroy window open");
            ComponentQuery destroyConfirmationSelect = ComponentQuery.newQuery(1183).componentIndex(5).subComponentIndex(-1);
            if(destroyConfirmationSelect.results().first() != null && Objects.requireNonNull(destroyConfirmationSelect.results().first()).interact(1)){
                println("Destroy option pressed");
                return true;
            } else {
                println("Destroy option failed.");
            }
        }
        return false;
    }

    public long handleTransmuting(LocalPlayer player) {
        if (player == null || Client.getGameState() != Client.GameState.LOGGED_IN || botState != BotState.TRANSMUTING) {
            Execution.delay(random.nextLong(3000,7000));
            return random.nextLong(1500,3000);
        }
        // Check we have senntisten crystals
        // If we do not have any, go to deposit
        // if we do, continue transmuting
        if (Backpack.isEmpty() || (!Backpack.contains(Pattern.compile(".*crystal$")))) {
            setBotState(BotState.WITHDRAWING);
            return random.nextLong(1500,3000);
        }
        println("Checking inventory has crystals to transmute.");
        senntistenCrystalCount = senntistenCrystalQuery.results().size();
        if (senntistenCrystalCount == 0 && InventoryItemQuery.newQuery(93).ids(-1).results().size() < 28) {
            println("No crystals found. Depositing transmuted crystals.");
            setBotState(BotState.DEPOSITING);
            return random.nextLong(1500,3000);
        }

        //the following is for forcing a spot open for memory strands outside of currency pouch xd
        if (VarManager.getVarbitValue(34807) == 0 && InventoryItemQuery.newQuery(93).ids(-1, 39486).results().isEmpty()) {
            Backpack.interact("Senntisten crystal", "Destroy");
            Execution.delayUntil(3000, this::handleDestroyMenu);
        }

        // Check which is empowered
        currentEmpowered = currentEmpoweredQuery.results().nearest();
        if (currentEmpowered == null) {
            println("Empowered crate not found");
            return random.nextLong(1500,3000);
        }

        String currentEmpoweredName;
        if (currentEmpowered.getName() != null) {
            currentEmpoweredName = currentEmpowered.getName();
            println("Current empowered: " + currentEmpoweredName);
        } else {
            println("Empowered crate not found");
            return random.nextLong(1500,3000);
        }

        if (currentEmpoweredName.equals(getEmpoweredCrystal()) && Interfaces.isOpen(1251)) {
//            println("Already transmuting empowered colour.");     //Just removed for my own testing, good debugging imo
            return random.nextLong(1500,3000);
        } else {
            setEmpoweredCrystal(currentEmpoweredName);
            println("Transmuting");
            boolean interacted = Backpack.interact("Senntisten crystal", "Transmute");
            println("Interacted: " + interacted);
            if (interacted) {
                println("Senntisten crystal clicked");
                Execution.delayUntil(3000, this::handleTransmuteMenu);
            } else {
                println("No senntisten crystals.");
            }
        }
        return random.nextLong(1500,3000);
    }

    public long handleDepositing(LocalPlayer player) {
        if (player == null || Client.getGameState() != Client.GameState.LOGGED_IN || botState != BotState.DEPOSITING) {
            Execution.delay(random.nextLong(3000,7000));
            return random.nextLong(1500,3000);
        }
        println("Checking inventory empty");
        currentEmpowered = currentEmpoweredQuery.results().nearest();
        List<Item> backpackItems = new ArrayList<>(Backpack.getItems());
        if (!backpackItems.isEmpty() && !Backpack.isFull()) {
            for (Item item : backpackItems) {
                if (!Objects.equals(item.getName(), "Memory strand") && !Objects.equals(item.getName(), "Senntisten crystal") && !Objects.equals(item.getName(), "Blue crystal") && !Objects.equals(item.getName(), "Red crystal") && !Objects.equals(item.getName(), "Green crystal") && !Objects.equals(item.getName(), "Yellow crystal")) {
                    println("Found unexpected item in backpack: " + item.getName());
                    return random.nextLong(1500,3000);
                }
            }
        }
        if (currentEmpowered == null || currentEmpowered.getName() == null) {
            println("Empowered crate not found");
            return random.nextLong(1500,3000);
        }
        senntistenCrystalCount = senntistenCrystalQuery.results().size();
        switch ((0 == senntistenCrystalCount) ? 0:
                (28 == senntistenCrystalCount) ? 1:
                        (1 <= senntistenCrystalCount && senntistenCrystalCount <= 27) ? 2: 3) {
            case 0 -> {
                if (!InventoryItemQuery.newQuery(93).name(Pattern.compile(".*crystal$")).results().isEmpty()){break;}
                println("Inventory empty, withdrawing");
                setBotState(BotState.WITHDRAWING);
                return random.nextLong(1500,3000);
            }
            case 1 -> setBotState(BotState.TRANSMUTING);
            case 2 -> {
                if (!InventoryItemQuery.newQuery(93).ids(-1).results().isEmpty()) {
                    setBotState(BotState.WITHDRAWING);
                    return random.nextLong(1500,3000);
                }
                if (transmuteMenuTextQuery.results().first() == null){
                    println("Inventory contains unexpected items."); //reachable during script restart, when some are transmuted.
                    setBotState(BotState.TRANSMUTING);
                    return random.nextLong(1500,3000);
                }
            }
            case 3 -> {
                println("Unexpected inventory results");
                setBotState(BotState.IDLE);
                return random.nextLong(1500,3000);
            }
        }
        if (!Backpack.isEmpty()) {
            println("Inventory not empty.");
            if (Backpack.contains("Yellow crystal")) {
                println("Got yellow crystals");
                if (currentEmpowered.getName().contains("yellow")) {
                    yellowCrate = currentEmpowered;
                    println("Deposit crate is empowered");
                } else {
                    yellowCrate = yellowCrateQuery.results().nearest();
                }
                if (yellowCrate == null ) {
                    println("Yellow crate not found.");
                } else {
                    if (yellowCrate.interact("Deposit")) {
                        println("Deposited");
                    } else {
                        println("Failed to deposit");
                    }
                }
            } else if (Backpack.contains("Green crystal")) {
                println("Got green crystals");
                if (currentEmpowered.getName().contains("green")) {
                    greenCrate = currentEmpowered;
                    println("Deposit crate is empowered");
                } else {
                    greenCrate = greenCrateQuery.results().nearest();
                }
                if (greenCrate == null ) {
                    println("Green crate not found.");
                } else {
                    if (greenCrate.interact("Deposit")) {
                        println("Deposited");
                    } else {
                        println("Failed to deposit");
                    }
                }
            } else if (Backpack.contains("Blue crystal")) {
                println("Got blue crystals");
                if (currentEmpowered.getName().contains("blue")) {
                    blueCrate = currentEmpowered;
                    println("Deposit crate is empowered");
                } else {
                    blueCrate = blueCrateQuery.results().nearest();
                }
                if (blueCrate == null ) {
                    println("Blue crate not found.");
                } else {
                    if (blueCrate.interact("Deposit")) {
                        println("Deposited");
                    } else {
                        println("Failed to deposit");
                    }
                }
            } else if (Backpack.contains("Red crystal")) {
                println("Got red crystals");
                if (currentEmpowered.getName().contains("red crystals")) {
                    redCrate = currentEmpowered;
                    println("Deposit crate is empowered");
                } else {
                    redCrate = redCrateQuery.results().nearest();
                }
                if (redCrate == null ) {
                    println("Red crate not found.");
                } else {
                    if (redCrate.interact("Deposit")) {
                        println("Deposited");
                    } else {
                        println("Failed to deposit");
                    }
                }
            }
        }
        return random.nextLong(1500,3000);
    }

    BotState getBotState() {
        return botState;
    }

    private void setBotState(BotState botState) {
        this.botState = botState;
    }

    public boolean isSomeBool() {
        return someBool;
    }

    public void setSomeBool(boolean someBool) {
        this.someBool = someBool;
    }

    public String getEmpoweredCrystal() {
        return empoweredCrystal;
    }

    public void setEmpoweredCrystal(String empoweredCrystal) {
        this.empoweredCrystal = empoweredCrystal;
    }

    public int getRemainingXP() {return remainingXP;}

    public void setRemainingXP(int remainingXP) {this.remainingXP = remainingXP;}
}
