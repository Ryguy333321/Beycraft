package com.grillo78.beycraft.inventory;

import com.grillo78.beycraft.inventory.slots.LockedSlot;
import com.grillo78.beycraft.inventory.slots.SlotBeyLayer;
import com.grillo78.beycraft.inventory.slots.SlotBeyLogger;
import com.grillo78.beycraft.inventory.slots.SlotHandle;
import com.grillo78.beycraft.network.PacketHandler;
import com.grillo78.beycraft.network.message.MessageUpdateLauncherItemStack;
import com.grillo78.beycraft.network.message.MessageUpdateLayerItemStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;


public class LauncherContainer extends Container {

    private ItemStack stack;
    private Hand hand;

    /**
     * @param type
     * @param id
     */
    public LauncherContainer(ContainerType<?> type, int id, ItemStack stack, PlayerInventory playerInventory,
                             PlayerEntity player, int rotation, Hand hand) {
        super(type, id);
        this.stack = stack;
        this.hand = hand;
        stack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
                .ifPresent(h -> this.addSlot(new SlotBeyLayer(h, 0, 10, 10, rotation)));
        stack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
                .ifPresent(h -> {
                    this.addSlot(new SlotHandle(h, 1, 62, 10));
                        });
        stack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
                .ifPresent(h -> this.addSlot(new SlotBeyLogger(h, 2, 62, 30)));
        addPlayerSlots(new InvWrapper(playerInventory), playerInventory.currentItem);
    }

    protected void addPlayerSlots(InvWrapper playerinventory, int locked) {
        int yStart = 30 + 18 * 3;
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                int x = 8 + col * 18;
                int y = row * 18 + yStart;
                this.addSlot(new SlotItemHandler(playerinventory, col + row * 9 + 9, x, y));
            }
        }

        for (int row = 0; row < 9; ++row) {
            int x = 8 + row * 18;
            int y = yStart + 58;
            if (row != locked)
                this.addSlot(new SlotItemHandler(playerinventory, row, x, y));
            else
                this.addSlot(new LockedSlot(playerinventory, row, x, y));
        }
    }

    @Override
    public void onContainerClosed(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity) {
            stack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(h -> {
                PacketHandler.instance.sendTo(new MessageUpdateLauncherItemStack(stack,
                        h.getStackInSlot(0),
                        h.getStackInSlot(1),
                        h.getStackInSlot(2),
                        hand,player.getUniqueID()),((ServerPlayerEntity)player).connection.getNetworkManager(), NetworkDirection.PLAY_TO_CLIENT);
            });
        }
    }

    @Override
    public boolean canInteractWith(PlayerEntity playerIn) {
        return true;
    }

    @Override
    public ItemStack transferStackInSlot(PlayerEntity playerIn, int index) {
        ItemStack transferred = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        int otherSlots = this.inventorySlots.size() - 36;

        if (slot != null && slot.getHasStack()) {
            ItemStack current = slot.getStack();
            transferred = current.copy();

            if (index < otherSlots) {
                if (!this.mergeItemStack(current, otherSlots, this.inventorySlots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.mergeItemStack(current, 0, otherSlots, false)) {
                return ItemStack.EMPTY;
            }

            if (current.getCount() == 0) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }
        }

        return transferred;
    }

}
