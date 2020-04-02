package minegame159.meteorclient.utils;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.mixininterface.IMinecraftClient;
import minegame159.meteorclient.mixininterface.IVec3d;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.ModuleManager;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityContext;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.Collectors;

public class Utils {
    public static MinecraftClient mc;

    private static Random random = new Random();
    private static Vec3d eyesPos = new Vec3d(0, 0, 0);
    private static Vec3d vec1 = new Vec3d(0, 0, 0);
    private static Vec3d vec2 = new Vec3d(0, 0, 0);

    public static String nameToTitle(String name) {
        return Arrays.stream(name.split("-")).map(StringUtils::capitalize).collect(Collectors.joining(" "));
    }

    public static String getKeyName(int key) {
        switch (key) {
            case GLFW.GLFW_KEY_UNKNOWN: return "Unknown";
            case GLFW.GLFW_KEY_ESCAPE: return "Esc";
            case GLFW.GLFW_KEY_PRINT_SCREEN: return "Print Screen";
            case GLFW.GLFW_KEY_PAUSE: return "Pause";
            case GLFW.GLFW_KEY_INSERT: return "Insert";
            case GLFW.GLFW_KEY_DELETE: return "Delete";
            case GLFW.GLFW_KEY_HOME: return "Home";
            case GLFW.GLFW_KEY_PAGE_UP: return "Page Up";
            case GLFW.GLFW_KEY_PAGE_DOWN: return "Page Down";
            case GLFW.GLFW_KEY_END: return "End";
            case GLFW.GLFW_KEY_TAB: return "Tab";
            case GLFW.GLFW_KEY_LEFT_CONTROL: return "Left Control";
            case GLFW.GLFW_KEY_RIGHT_CONTROL: return "Right Control";
            case GLFW.GLFW_KEY_LEFT_ALT: return "Left Alt";
            case GLFW.GLFW_KEY_RIGHT_ALT: return "Right Alt";
            case GLFW.GLFW_KEY_LEFT_SHIFT: return "Left Shift";
            case GLFW.GLFW_KEY_RIGHT_SHIFT: return "Right Shift";
            case GLFW.GLFW_KEY_UP: return "Arrow Up";
            case GLFW.GLFW_KEY_DOWN: return "Arrow Down";
            case GLFW.GLFW_KEY_LEFT: return "Arrow Left";
            case GLFW.GLFW_KEY_RIGHT: return "Arrow Right";
            case GLFW.GLFW_KEY_APOSTROPHE: return "Apostrophe";
            case GLFW.GLFW_KEY_BACKSPACE: return "Backspace";
            case GLFW.GLFW_KEY_CAPS_LOCK: return "Caps Lock";
            case GLFW.GLFW_KEY_MENU: return "Menu";
            case GLFW.GLFW_KEY_LEFT_SUPER: return "Left Super";
            case GLFW.GLFW_KEY_RIGHT_SUPER: return "Right Super";
            case GLFW.GLFW_KEY_ENTER: return "Enter";
            case GLFW.GLFW_KEY_NUM_LOCK: return "Num Lock";
            case GLFW.GLFW_KEY_SPACE: return "Space";
            default: return GLFW.glfwGetKeyName(key, 0);
        }
    }

    public static boolean place(BlockState blockState, BlockPos blockPos) {
        // Calculate eyes pos
        ((IVec3d) eyesPos).set(mc.player.x, mc.player.y + mc.player.getEyeHeight(mc.player.getPose()), mc.player.z);

        // Check if current block is replaceable
        if (!mc.world.getBlockState(blockPos).getMaterial().isReplaceable()) return false;

        for (Direction side : Direction.values()) {
            BlockPos neighbor = blockPos.offset(side);
            Direction side2 = side.getOpposite();

            // Check if side is visible (facing away from player)
            ((IVec3d) vec1).set(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
            ((IVec3d) vec2).set(neighbor.getX() + 0.5, neighbor.getY() + 0.5, neighbor.getZ() + 0.5);
            if(eyesPos.squaredDistanceTo(vec1) >= eyesPos.squaredDistanceTo(vec2)) continue;

            // Check if neighbor can be right clicked
            if (mc.world.getBlockState(blockPos).getOutlineShape(mc.world, blockPos) != VoxelShapes.empty()) continue;

            // Calculate hit pos
            ((IVec3d) vec1).set(neighbor.getX() + 0.5 + side2.getVector().getX() * 0.5, neighbor.getY() + 0.5 + side2.getVector().getY() * 0.5, neighbor.getZ() + 0.5 + side2.getVector().getZ() * 0.5);

            // Check if hitVec is within range (4.25 blocks)
            if(eyesPos.squaredDistanceTo(vec1) > 18.0625) continue;

            // Check if intersects entities
            if (!mc.world.canPlace(blockState, blockPos, EntityContext.absent())) continue;

            // Place block
            PlayerMoveC2SPacket.LookOnly packet = new PlayerMoveC2SPacket.LookOnly(getNeededYaw(vec1), getNeededPitch(vec1), mc.player.onGround);
            mc.player.networkHandler.sendPacket(packet);
            mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(vec1, side2, neighbor, false));
            mc.interactionManager.interactItem(mc.player, mc.world, Hand.MAIN_HAND);
            mc.player.swingHand(Hand.MAIN_HAND);

            return true;
        }

        return false;
    }

    public static float getNeededYaw(Vec3d vec) {
        return mc.player.yaw + MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(vec.z - mc.player.z, vec.x - mc.player.x)) - 90f - mc.player.yaw);
    }

    public static float getNeededPitch(Vec3d vec) {
        double diffX = vec.x - mc.player.x;
        double diffY = vec.y - (mc.player.y + mc.player.getEyeHeight(mc.player.getPose()));
        double diffZ = vec.z - mc.player.z;

        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        return mc.player.pitch + MathHelper.wrapDegrees((float) -Math.toDegrees(Math.atan2(diffY, diffXZ)) - mc.player.pitch);
    }

    public static boolean canUpdate() {
        return mc.world != null || mc.player != null;
    }

    public static int random(int min, int max) {
        return random.nextInt(max - min) + min;
    }

    public static int getTextWidth(String text) {
        return MeteorClient.TEXT_RENDERER.getStringWidth(text);
    }
    public static int getTextHeight() {
        return MeteorClient.TEXT_RENDERER.getHeight() + 2;
    }

    public static void drawText(String text, float x, float y, int color) {
        MeteorClient.TEXT_RENDERER.drawString(text, x, y + 1, color);
    }
    public static void drawTextWithShadow(String text, float x, float y, int color) {
        MeteorClient.TEXT_RENDERER.drawStringWithShadow(text, x, y + 1, color);
    }

    public static void sendMessage(String msg, Object... args) {
        msg = String.format(msg, args);
        msg = msg.replaceAll("#yellow", Formatting.YELLOW.toString());
        msg = msg.replaceAll("#white", Formatting.WHITE.toString());
        msg = msg.replaceAll("#red", Formatting.RED.toString());
        msg = msg.replaceAll("#blue", Formatting.BLUE.toString());
        msg = msg.replaceAll("#pink", Formatting.LIGHT_PURPLE.toString());
        msg = msg.replaceAll("#gray", Formatting.GRAY.toString());

        mc.player.sendMessage(new LiteralText(msg));
    }

    public static void leftClick() {
        ((IMinecraftClient) mc).leftClick();
    }
    public static void rightClick() {
        ((IMinecraftClient) mc).rightClick();
    }

    public static Module tryToGetModule(String[] args) {
        if (args.length < 1) {
            Utils.sendMessage("#redYou must specify module name.");
            return null;
        }
        Module oldModule = ModuleManager.INSTANCE.get(args[0]);
        if (oldModule == null) {
            Utils.sendMessage("#redModule with name #blue'%s' #reddoesn't exist.", args[0]);
            return null;
        }
        return oldModule;
    }

    public static boolean isShulker(Item item) {
        return item == Items.SHULKER_BOX || item == Items.WHITE_SHULKER_BOX || item == Items.ORANGE_SHULKER_BOX || item == Items.MAGENTA_SHULKER_BOX || item == Items.LIGHT_BLUE_SHULKER_BOX || item == Items.YELLOW_SHULKER_BOX || item == Items.LIME_SHULKER_BOX || item == Items.PINK_SHULKER_BOX || item == Items.GRAY_SHULKER_BOX || item == Items.LIGHT_GRAY_SHULKER_BOX || item == Items.CYAN_SHULKER_BOX || item == Items.PURPLE_SHULKER_BOX || item == Items.BLUE_SHULKER_BOX || item == Items.BROWN_SHULKER_BOX || item == Items.GREEN_SHULKER_BOX || item == Items.RED_SHULKER_BOX || item == Items.BLACK_SHULKER_BOX;
    }

    public static boolean isThrowable(Item item) {
        return item instanceof BowItem || item instanceof CrossbowItem || item instanceof SnowballItem || item instanceof EggItem || item instanceof EnderPearlItem || item instanceof SplashPotionItem || item instanceof LingeringPotionItem || item instanceof FishingRodItem || item instanceof TridentItem;
    }

    public static String floatToString(float number) {
        if (number % 1 == 0) return Integer.toString((int) number);
        return Float.toString(number);
    }

    public static String doubleToString(double number) {
        if (number % 1 == 0) return Integer.toString((int) number);
        return Double.toString(number);
    }

    public static int clamp(int value, int min, int max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }
    public static float clamp(float value, float min, float max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }
    public static double clamp(double value, double min, double max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }
}
