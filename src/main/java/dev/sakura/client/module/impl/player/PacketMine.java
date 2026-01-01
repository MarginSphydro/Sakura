/*
package dev.sakura.client.module.impl.player;

import dev.sakura.client.events.client.TickEvent;
import dev.sakura.client.events.render.Render3DEvent;
import dev.sakura.client.manager.Managers;
import dev.sakura.client.module.Category;
import dev.sakura.client.module.Module;
import dev.sakura.client.utils.color.ColorUtil;
import dev.sakura.client.utils.entity.EntityUtil;
import dev.sakura.client.utils.player.InvUtil;
import dev.sakura.client.utils.render.Render3DUtil;
import dev.sakura.client.utils.time.TimerUtil;
import dev.sakura.client.values.impl.BoolValue;
import dev.sakura.client.values.impl.ColorValue;
import dev.sakura.client.values.impl.EnumValue;
import dev.sakura.client.values.impl.NumberValue;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

public class PacketMine extends Module {
    public PacketMine() {
        super("PacketMine", "发包挖掘", Category.Player);
    }

    public enum RemineMode {
        Instant,
        Normal,
        Fast
    }

    public enum Swap {
        Normal,
        Silent,
        SilentAlt,
        Off
    }

    private final BoolValue autoConfig = new BoolValue("Auto", "自动", false);
    private final BoolValue avoidSelfConfig = new BoolValue("AvoidSelf", "避开自身", false, autoConfig::get);
    private final BoolValue strictDirectionConfig = new BoolValue("StrictDirection", "严格方向", false, autoConfig::get);
    private final NumberValue<Double> enemyRangeConfig = new NumberValue<>("EnemyRange", "敌人范围", 5.0, 1.0, 10.0, 0.1, autoConfig::get);
    private final BoolValue antiCrawlConfig = new BoolValue("AntiCrawl", "反爬行", false);
    private final BoolValue headConfig = new BoolValue("TargetBody", "目标身体", false, autoConfig::get);
    private final BoolValue aboveHeadConfig = new BoolValue("TargetHead", "目标头部", false, autoConfig::get);
    private final BoolValue doubleBreakConfig = new BoolValue("DoubleBreak", "双重破坏", false);
    private final NumberValue<Integer> mineTicksConfig = new NumberValue<>("MiningTicks", "挖掘刻数", 20, 5, 60, 1, doubleBreakConfig::get);
    private final EnumValue<RemineMode> remineConfig = new EnumValue<>("Remine", "重挖", RemineMode.Normal);
    private final BoolValue packetInstantConfig = new BoolValue("Fast", "快速", false, () -> remineConfig.is(RemineMode.Instant));
    private final NumberValue<Double> rangeConfig = new NumberValue<>("Range", "范围", 4.0, 0.1, 6.0, 0.1);
    private final NumberValue<Double> speedConfig = new NumberValue<>("Speed", "速度", 1.0, 0.1, 1.0, 0.1);
    private final EnumValue<Swap> swapConfig = new EnumValue<>("AutoSwap", "自动切换", Swap.Silent);
    private final BoolValue swapBeforeConfig = new BoolValue("SwapBefore", "挖前切换", false, () -> !swapConfig.is(Swap.Off));
    private final BoolValue rotateConfig = new BoolValue("Rotate", "旋转", true);
    private final NumberValue<Integer> rotationSpeed = new NumberValue<>("Rotation Speed", "旋转速度", 10, 0, 10, 1, rotateConfig::get);
    private final BoolValue switchResetConfig = new BoolValue("Switch Reset", "切换重置", false);
    private final BoolValue miningFix = new BoolValue("Mining Fix", "切换重置", false);
    private final BoolValue grimConfig = new BoolValue("Grim", "Grim", false);
    private final BoolValue grimNewConfig = new BoolValue("GrimV3", "GrimV3", false, grimConfig::get);

    private final ColorValue sideColor = new ColorValue("MineSide", "挖掘面颜色", new Color(255, 0, 0, 31));
    private final ColorValue lineColor = new ColorValue("MineLine", "挖掘线颜色", new Color(255, 0, 0, 233));
    private final ColorValue doneSideColor = new ColorValue("DoneSide", "完成面颜色", new Color(0, 255, 0, 23));
    private final ColorValue doneLineColor = new ColorValue("DoneLine", "完成线颜色", new Color(0, 255, 0, 233));
    private final NumberValue<Integer> fadeTimeConfig = new NumberValue<>("Fade-Time", "淡出时间", 250, 0, 1000, 1, () -> false);
    private final BoolValue smoothColorConfig = new BoolValue("SmoothColor", "平滑颜色", false, () -> false);

    private final BoolValue debugConfig = new BoolValue("Debug", "调试", false);
    private final ColorValue packetSide = new ColorValue("PacketSide", "发包面颜色", new Color(0, 0, 255, 31), debugConfig::get);
    private final ColorValue packetLine = new ColorValue("PacketLine", "发包线颜色", new Color(0, 0, 255, 233), debugConfig::get);
    private final ColorValue instantSide = new ColorValue("InstantSide", "瞬挖面颜色", new Color(255, 0, 255, 31), debugConfig::get);
    private final ColorValue instantLine = new ColorValue("InstantLine", "瞬挖线颜色", new Color(255, 0, 255, 233), debugConfig::get);
    private final BoolValue debugTicksConfig = new BoolValue("Debug-Ticks", "调试刻数", false, debugConfig::get);

    private PlayerEntity playerTarget;
    private MineData packetMine, instantMine; // mining2 should always be the instant mine
    private boolean packetSwapBack;
    private boolean manualOverride;
    private final TimerUtil remineTimer = new TimerUtil();

    private boolean changedInstantMine;
    private boolean waitForPacketMine;
    private boolean packetMineStuck;

    private boolean antiCrawlOverride;
    private int antiCrawlTicks;

    private final java.util.Queue<MineData> autoMineQueue = new ArrayDeque<>();
    private int autoMineTickDelay;


    @Override
    public String getSuffix() {
        if (instantMine != null) {
            return String.format("%.1f", Math.min(instantMine.getBlockDamage(), 1.0f));
        }
        return super.getSuffix();
    }

    @Override
    public void onDisable() {
        autoMineQueue.clear();
        playerTarget = null;
        packetMine = null;
        if (instantMine != null) {
            abortMining(instantMine);
            instantMine = null;
        }

        autoMineTickDelay = 0;
        antiCrawlTicks = 0;
        manualOverride = false;
        antiCrawlOverride = false;
        waitForPacketMine = false;
        packetMineStuck = false;
        if (packetSwapBack) {
            InvUtil.swapBack();
            packetSwapBack = false;
        }
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (mc.player.isCreative() || mc.player.isSpectator()) {
            return;
        }

        PlayerEntity currentTarget = EntityUtil.getClosestPlayer(enemyRangeConfig.get());
        boolean targetChanged = playerTarget != null && playerTarget != currentTarget;
        playerTarget = currentTarget;

        if (isInstantMineComplete()) {
            if (changedInstantMine) {
                changedInstantMine = false;
            }
            if (waitForPacketMine) {
                waitForPacketMine = false;
            }
        }

        autoMineTickDelay--;
        antiCrawlTicks--;

        // Mining packet handling
        if (packetMine != null && packetMine.getTicksMining() > mineTicksConfig.get()) {
            packetMineStuck = true;
            //packetMineAnim.animation.setState(false);
            if (packetSwapBack) {
                InvUtil.swapBack();
                packetSwapBack = false;
            }
            packetMine = null;
            if (!isInstantMineComplete()) {
                waitForPacketMine = true;
            }
        }

        if (packetMine != null) {
            final float damageDelta = SpeedMine.getInstance().calcBlockBreakingDelta(
                    packetMine.getState(), mc.world, packetMine.getPos());
            packetMine.addBlockDamage(damageDelta);

            int slot = packetMine.getBestSlot();
            float damageDone = packetMine.getBlockDamage() + (swapBeforeConfig.getValue()
                    || packetMineStuck ? damageDelta : 0.0f);
            if (damageDone >= 1.0f && slot != -1 && !checkMultitask()) {
                Managers.INVENTORY.setSlot(slot);
                packetSwapBack = true;
                if (packetMineStuck) {
                    packetMineStuck = false;
                }
            }
        }

        if (packetSwapBack) {
            if (packetMine != null && canMine(packetMine.getState())) {
                packetMine.markAttemptedMine();
            } else {
                Managers.INVENTORY.syncToClient();
                packetSwapBack = false;
                packetMineAnim.animation.setState(false);
                packetMine = null;
                if (!isInstantMineComplete()) {
                    waitForPacketMine = true;
                }
            }
        }

        if (instantMine != null) {
            final double distance = mc.player.getEyePos().squaredDistanceTo(instantMine.getPos().toCenterPos());
            if (distance > ((NumberConfig<Float>) rangeConfig).getValueSq()
                    || instantMine.getTicksMining() > mineTicksConfig.getValue()) {
                abortMining(instantMine);
                instantMineAnim.animation.setState(false);
                instantMine = null;
            }
        }

        if (instantMine != null) {
            final float damageDelta = SpeedMine.getInstance().calcBlockBreakingDelta(
                    instantMine.getState(), mc.world, instantMine.getPos());
            instantMine.addBlockDamage(damageDelta);

            if (instantMine.getBlockDamage() >= speedConfig.getValue()) {
                boolean canMine = canMine(instantMine.getState());
                boolean canPlace = mc.world.canPlace(instantMine.getState(), instantMine.getPos(), ShapeContext.absent());
                if (canMine) {
                    instantMine.markAttemptedMine();
                } else {
                    instantMine.resetMiningTicks();
                    if (remineConfig.getValue() == RemineMode.NORMAL || remineConfig.getValue() == RemineMode.FAST) {
                        instantMine.setTotalBlockDamage(0.0f, 0.0f);
                    }

                    if (manualOverride) {
                        manualOverride = false;
                        // Clear our old manual mine
                        abortMining(instantMine);
                        instantMineAnim.animation.setState(false);
                        instantMine = null;
                    }
                }

                boolean passedRemine = remineConfig.getValue() == RemineMode.INSTANT || remineTimer.passedMS(500);
                if (instantMine != null && (remineConfig.getValue() == RemineMode.INSTANT
                        && packetInstantConfig.getValue() && packetMine == null && canPlace || canMine && passedRemine)
                        && (!checkMultitask() || multitaskConfig.getValue() || swapConfig.getValue() == Swap.OFF)) {
                    stopMining(instantMine);
                    remineTimer.reset();

                    if (AutoCrystalModule.getInstance().isEnabled()
                            && AutoCrystalModule.getInstance().shouldPreForcePlace()) {
                        AutoCrystalModule.getInstance().placeCrystalForTarget(playerTarget, instantMine.getPos().down());
                    }

                    if (remineConfig.getValue() == RemineMode.FAST) {
                        startMining(instantMine);
                    }
                }
            }
        }

        // Clear overrides
        if (manualOverride && (instantMine == null || instantMine.getGoal() != MiningGoal.MANUAL)) {
            manualOverride = false;
        }

        if (antiCrawlOverride && (instantMine == null || instantMine.getGoal() != MiningGoal.PREVENT_CRAWL)) {
            antiCrawlOverride = false;
        }

        if (autoConfig.getValue()) {
            if (!autoMineQueue.isEmpty() && autoMineTickDelay <= 0) {
                MineData nextMine = autoMineQueue.poll();
                if (nextMine != null) {
                    startMining(nextMine);
                    autoMineTickDelay = 5;
                }
            }

            BlockPos antiCrawlPos = getAntiCrawlPos(playerTarget);
            if (antiCrawlOverride) {
                if (mc.player.getPose().equals(EntityPose.SWIMMING)) {
                    antiCrawlTicks = 10;
                }

                if (antiCrawlTicks <= 0 || !isInstantMineComplete() && antiCrawlPos != null
                        && !instantMine.getPos().equals(antiCrawlPos)) {
                    antiCrawlOverride = false;
                }
            }

            if (autoMineQueue.isEmpty() && !manualOverride && !antiCrawlOverride) {
                if (antiCrawlConfig.getValue() && mc.player.getPose().equals(EntityPose.SWIMMING) && antiCrawlPos != null) {
                    MineData data = new MineData(antiCrawlPos, strictDirectionConfig.getValue() ?
                            Managers.INTERACT.getInteractDirection(antiCrawlPos, false) : Direction.UP, MiningGoal.PREVENT_CRAWL);
                    if (isInstantMineComplete() || !instantMine.equals(data)) {
                        startAutoMine(data);
                        antiCrawlOverride = true;
                    }
                } else if (playerTarget != null && !targetChanged) {
                    BlockPos targetPos = EntityUtil.getRoundedBlockPos(playerTarget);
                    boolean bedrockPhased = PositionUtil.isBedrock(playerTarget.getBoundingBox(), targetPos) && !playerTarget.isCrawling();

                    if (!isInstantMineComplete() && checkDataY(instantMine, targetPos, bedrockPhased)) {
                        abortMining(instantMine);
                        instantMineAnim.animation.setState(false);
                        instantMine = null;
                    } else if (packetMine != null && checkDataY(packetMine, targetPos, bedrockPhased)) {
                        packetMineAnim.animation.setState(false);
                        if (packetSwapBack) {
                            Managers.INVENTORY.syncToClient();
                            packetSwapBack = false;
                        }
                        packetMine = null;
                        waitForPacketMine = false;
                    } else {
                        java.util.List<BlockPos> phasedBlocks = getPhaseBlocks(playerTarget, targetPos, bedrockPhased);

                        MineData bestMine;
                        if (!phasedBlocks.isEmpty()) {
                            BlockPos pos1 = phasedBlocks.removeFirst();
                            bestMine = new MineData(pos1, strictDirectionConfig.getValue() ?
                                    Managers.INTERACT.getInteractDirection(pos1, false) : Direction.UP);

                            if (packetMine == null && doubleBreakConfig.getValue() || isInstantMineComplete()) {
                                startAutoMine(bestMine);
                            }
                        } else {
                            java.util.List<BlockPos> miningBlocks = getMiningBlocks(playerTarget, targetPos, bedrockPhased);
                            bestMine = getInstantMine(miningBlocks, bedrockPhased);

                            if (bestMine != null && (packetMine == null && !changedInstantMine
                                    && doubleBreakConfig.getValue() || isInstantMineComplete())) {
                                startAutoMine(bestMine);
                            }
                        }
                    }
                } else {
                    if (!isInstantMineComplete() && instantMine.getGoal() == MiningGoal.MINING_ENEMY) {
                        abortMining(instantMine);
                        instantMineAnim.animation.setState(false);
                        instantMine = null;
                    }

                    if (packetMine != null && packetMine.getGoal() == MiningGoal.MINING_ENEMY) {
                        packetMineAnim.animation.setState(false);
                        if (packetSwapBack) {
                            Managers.INVENTORY.syncToClient();
                            packetSwapBack = false;
                        }
                        packetMine = null;
                        waitForPacketMine = false;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onAttackBlock(AttackBlockEvent event) {
        if (mc.player.isCreative() || mc.player.isSpectator()) {
            return;
        }

        event.cancel();

        // Do not try to break unbreakable blocks
        if (event.getState().getBlock().getHardness() == -1.0f || !canMine(event.getState()) || isMining(event.getPos())) {
            return;
        }

        MineData data = new MineData(event.getPos(), event.getDirection(), MiningGoal.MANUAL);

        if (instantMine != null && instantMine.getGoal() == MiningGoal.MINING_ENEMY
                || packetMine != null && packetMine.getGoal() == MiningGoal.MINING_ENEMY) {
            manualOverride = true;
        }

        if (!doubleBreakConfig.getValue()) {
            instantMine = data;
            startMining(instantMine);
            mc.player.swingHand(Hand.MAIN_HAND, false);
            return;
        }

        boolean updateChanged = false;
        if (!isInstantMineComplete() && !changedInstantMine) {
            if (packetMine == null) {
                packetMine = instantMine.copy();
                packetMineAnim = new MineAnimation(packetMine,
                        new Animation(true, fadeTimeConfig.getValue()));
            } else {
                updateChanged = true;
            }
        }

        instantMine = data;
        startMining(instantMine);
        mc.player.swingHand(Hand.MAIN_HAND, false);
        if (updateChanged) {
            changedInstantMine = true;
        }
    }

    @EventHandler
    public void onPacketOutbound(PacketEvent.Outbound event) {
        if (event.getPacket() instanceof UpdateSelectedSlotC2SPacket && switchResetConfig.getValue() && instantMine != null) {
            instantMine.setTotalBlockDamage(0.0f, 0.0f);
        }
    }

    @EventHandler
    public void onPacketInbound(PacketEvent.Inbound event) {
        if (event.getPacket() instanceof BlockUpdateS2CPacket packet && canMine(packet.getState())) {
            if (antiCrawlOverride && packet.getPos().equals(getAntiCrawlPos(playerTarget))) {
                antiCrawlTicks = 10;
            }
        }
    }

    public void startAutoMine(MineData data) {
        if (!canMine(data.getState()) || isMining(data.getPos())) {
            return;
        }

        if (!doubleBreakConfig.getValue()) {
            instantMine = data;
            autoMineQueue.offer(data);
            return;
        }

        if (changedInstantMine && !isInstantMineComplete() || waitForPacketMine) {
            return;
        }

        boolean updateChanged = false;
        if (!isInstantMineComplete() && !changedInstantMine) {
            if (packetMine == null) {
                packetMine = instantMine.copy();
                packetMineAnim = new MineAnimation(packetMine,
                        new Animation(true, fadeTimeConfig.getValue()));
            } else {
                updateChanged = true;
            }
        }

        instantMine = data;
        autoMineQueue.offer(data);

        if (updateChanged) {
            changedInstantMine = true;
        }
    }

    public float calcBlockBreakingDelta(BlockState state, BlockView world, BlockPos pos) {
        if (swapConfig.get() == Swap.Off) {
            return state.calcBlockBreakingDelta(mc.player, mc.world, pos);
        }
        float f = state.getHardness(world, pos);
        if (f == -1.0f) {
            return 0.0f;
        } else {
            int i = canHarvest(state) ? 30 : 100;
            return getBlockBreakingSpeed(state) / f / (float) i;
        }
    }

    public MineData getInstantMine(java.util.List<BlockPos> miningBlocks, boolean bedrockPhased) {
        PriorityQueue<MineData> validInstantMines = new PriorityQueue<>();
        for (BlockPos blockPos : miningBlocks) {
            BlockState state1 = mc.world.getBlockState(blockPos);
            if (!isAutoMineBlock(state1.getBlock())) // bedrock mine exploit!!
            {
                continue;
            }

            double dist = mc.player.getEyePos().squaredDistanceTo(blockPos.toCenterPos());
            if (dist > ((NumberConfig<Float>) rangeConfig).getValueSq()) {
                continue;
            }

            BlockState state2 = mc.world.getBlockState(blockPos.down());
            if (bedrockPhased || state2.isOf(Blocks.OBSIDIAN) || state2.isOf(Blocks.BEDROCK)) {
                Direction direction = strictDirectionConfig.getValue() ?
                        Managers.INTERACT.getInteractDirection(blockPos, false) : Direction.UP;

                validInstantMines.add(new MineData(blockPos, direction));
            }
        }

        if (validInstantMines.isEmpty()) {
            return null;
        }

        return validInstantMines.peek();
    }

    public java.util.List<BlockPos> getPhaseBlocks(PlayerEntity player, BlockPos playerPos, boolean targetBedrockPhased) {
        java.util.List<BlockPos> phaseBlocks = PositionUtil.getAllInBox(player.getBoundingBox(),
                targetBedrockPhased && headConfig.getValue() ? playerPos.up() : playerPos);

        phaseBlocks.removeIf(p ->
        {
            BlockState state = mc.world.getBlockState(p);
            if (!isAutoMineBlock(state.getBlock()) || !canMine(state) || isMining(p)) {
                return true;
            }

            double dist = mc.player.getEyePos().squaredDistanceTo(p.toCenterPos());
            if (dist > ((NumberConfig<Float>) rangeConfig).getValueSq()) {
                return true;
            }

            return avoidSelfConfig.getValue() && intersectsPlayer(p);
        });

        if (targetBedrockPhased && aboveHeadConfig.getValue()) {
            phaseBlocks.add(playerPos.up(2));
        }

        return phaseBlocks;
    }

*
     *
     * @param player
     * @return A {@link Set} of potential blocks to mine for an enemy player


    public java.util.List<BlockPos> getMiningBlocks(PlayerEntity player, BlockPos playerPos, boolean bedrockPhased) {
        java.util.List<BlockPos> surroundingBlocks = SurroundModule.getInstance().getSurroundNoDown(player, rangeConfig.getValue());
        java.util.List<BlockPos> miningBlocks;
        if (bedrockPhased) {
            java.util.List<BlockPos> facePlaceBlocks = new ArrayList<>();
            if (headConfig.getValue()) {
                facePlaceBlocks.addAll(surroundingBlocks.stream().map(BlockPos::up).toList());
            }

            BlockState belowFeet = mc.world.getBlockState(playerPos.down());
            if (canMine(belowFeet)) {
                facePlaceBlocks.add(playerPos.down());
            }
            miningBlocks = facePlaceBlocks;
        } else {
            miningBlocks = surroundingBlocks;
        }

        miningBlocks.removeIf(p -> avoidSelfConfig.getValue() && intersectsPlayer(p));
        return miningBlocks;
    }

    private BlockPos getAntiCrawlPos(PlayerEntity playerTarget) {
        if (!mc.player.isOnGround()) {
            return null;
        }
        BlockPos crawlingPos = EntityUtil.getRoundedBlockPos(mc.player);
        boolean playerBelow = playerTarget != null && EntityUtil.getRoundedBlockPos(playerTarget).getY() < crawlingPos.getY();
        // We want to be same level as our opponent
        if (playerBelow) {
            BlockState state = mc.world.getBlockState(crawlingPos.down());
            if (isAutoMineBlock(state.getBlock()) && canMine(state)) {
                return crawlingPos.down();
            }
        } else {
            BlockState state = mc.world.getBlockState(crawlingPos.up());
            if (isAutoMineBlock(state.getBlock()) && canMine(state)) {
                return crawlingPos.up();
            }
        }
        return null;
    }

    private boolean checkDataY(MineData data, BlockPos targetPos, boolean bedrockPhased) {
        return data.getGoal() == MiningGoal.MINING_ENEMY && !bedrockPhased && data.getPos().getY() != targetPos.getY();
    }

    private boolean intersectsPlayer(BlockPos pos) {
        List<BlockPos> playerBlocks = SurroundModule.getInstance().getPlayerBlocks(mc.player);
        List<BlockPos> surroundingBlocks = SurroundModule.getInstance().getSurroundNoDown(mc.player);
        return playerBlocks.contains(pos) || surroundingBlocks.contains(pos);
    }

    @EventHandler
    public void onRenderWorld(Render3DEvent event) {
        if (mc.player.isCreative() || mc.player.isSpectator()) {
            return;
        }

*/
/*        if (instantMineAnim != null && instantMineAnim.animation().getFactor() > 0.01f) {
            renderMiningData(event.getMatrices(), event.getTickDelta(),
                    instantMineAnim, true);
        }

        if (doubleBreakConfig.get() && packetMineAnim != null && packetMineAnim.animation().getFactor() > 0.01f) {
            renderMiningData(event.getMatrices(), event.getTickDelta(),
                    packetMineAnim, false);
        }*//*

    }

    public void renderMiningData(MatrixStack matrixStack, float tickDelta, MineAnimation mineAnimation, boolean instantMine) {
        MineData data = mineAnimation.data();

        Color boxColor;
        Color lineColor;

        if (this.debugConfig.get()) {
            boxColor = instantMine ? this.instantSide.get() : this.packetSide.get();
            lineColor = instantMine ? this.instantLine.get() : this.packetLine.get();
        } else if (smoothColorConfig.get()) {
            boxColor = !canMine(data.getState()) ? doneSideColor.get() :
                    ColorUtil.interpolateColor(Math.min(data.getBlockDamage(), 1.0f), doneSideColor.get(), sideColor.get());
            lineColor = !canMine(data.getState()) ? doneSideColor.get() :
                    ColorUtil.interpolateColor(Math.min(data.getBlockDamage(), 1.0f), doneLineColor.get(), this.lineColor.get());
        } else {
            boxColor = data.getBlockDamage() >= 0.95f || !canMine(data.getState()) ? doneSideColor.get() : sideColor.get();
            lineColor = data.getBlockDamage() >= 0.95f || !canMine(data.getState()) ? doneLineColor.get() : this.lineColor.get();
        }

        BlockPos mining = data.getPos();
        VoxelShape outlineShape = VoxelShapes.fullCube();
        if (!instantMine || data.getBlockDamage() < speedConfig.get()) {
            outlineShape = data.getState().getOutlineShape(mc.world, mining);
            outlineShape = outlineShape.isEmpty() ? VoxelShapes.fullCube() : outlineShape;
        }
        Box render1 = outlineShape.getBoundingBox();
        Vec3d center = render1.offset(mining).getCenter();
        float total = instantMine ? speedConfig.get().floatValue() : 1.0f;
        float scale = (instantMine && data.getBlockDamage() >= speedConfig.get()) || !canMine(data.getState()) ? 1.0f :
                MathHelper.clamp((data.getBlockDamage() + (data.getBlockDamage() - data.getLastDamage()) * tickDelta) / total, 0.0f, 1.0f);
        double dx = (render1.maxX - render1.minX) / 2.0;
        double dy = (render1.maxY - render1.minY) / 2.0;
        double dz = (render1.maxZ - render1.minZ) / 2.0;
        final Box scaled = new Box(center, center).expand(dx * scale, dy * scale, dz * scale);

        Render3DUtil.drawFullBox(matrixStack, scaled, boxColor, lineColor, 2f);
    }

    public void startMining(MineData data) {
        if (rotateConfig.get()) {
            if (grimConfig.get()) {
                Managers.ROTATION.lookAt(data.getPos().toCenterPos(), rotationSpeed.get());
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), Managers.ROTATION.getYaw(), Managers.ROTATION.getPitch(), mc.player.isOnGround(), mc.player.horizontalCollision));
            } else {
                Managers.ROTATION.lookAt(data.getPos().toCenterPos(), rotationSpeed.get());
            }
        }

        if (doubleBreakConfig.get()) {
            // https://github.com/GrimAnticheat/Grim/blob/2.0/src/main/java/ac/grim/grimac/checks/impl/misc/FastBreak.java#L76
            // https://github.com/GrimAnticheat/Grim/blob/2.0/src/main/java/ac/grim/grimac/checks/impl/misc/FastBreak.java#L98
            if (grimNewConfig.get()) {
                if (!miningFix.get()) {
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                            PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                            PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, data.getPos(), data.getDirection()));
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                            PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, data.getPos(), data.getDirection()));
                } else {
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                            PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, data.getPos(), data.getDirection()));
                }

                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
                mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            } else {
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, data.getPos(), data.getDirection()));
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, data.getPos(), data.getDirection()));
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
                mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            }
        } else {
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, data.getPos(), data.getDirection()));
        }

        if (rotateConfig.getValue() && grimConfig.getValue()) {
            Managers.ROTATION.setRotationSilentSync();
        }

        instantMineAnim = new MineAnimation(data, new Animation(true, fadeTimeConfig.getValue()));
    }

    public void abortMining(MineData data) {
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, data.getPos(), data.getDirection()));
    }

    public void stopMining(MineData data) {
        if (rotateConfig.getValue()) {
            float[] rotations = RotationUtil.getRotationsTo(mc.player.getEyePos(), data.getPos().toCenterPos());
            if (grimConfig.getValue()) {
                setRotationSilent(rotations[0], rotations[1]);
            } else {
                setRotation(rotations[0], rotations[1]);
            }
        }

        int slot = data.getBestSlot();
        if (slot != -1) {
            swapTo(slot);
        }

        stopMiningInternal(data);

        if (slot != -1) {
            swapSync(slot);
        }

        if (rotateConfig.getValue() && grimConfig.getValue()) {
            Managers.ROTATION.setRotationSilentSync();
        }
    }

    private void stopMiningInternal(MineData data) {
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, data.getPos(), data.getDirection()));
    }

    public boolean isInstantMineComplete() {
        return instantMine == null || instantMine.getBlockDamage() >= speedConfig.getValue() && !canMine(instantMine.getState());
    }

    public BlockPos getMiningBlock() {
        if (instantMine != null) {
            double damage = instantMine.getBlockDamage() / speedConfig.getValue();
            if (damage > 0.75) {
                return instantMine.getPos();
            }
        }
        return null;
    }

    private void swapTo(int slot) {
        switch (swapConfig.getValue()) {
            case NORMAL -> Managers.INVENTORY.setClientSlot(slot);
            case SILENT -> Managers.INVENTORY.setSlot(slot);
            case SILENT_ALT -> Managers.INVENTORY.setSlotAlt(slot);
        }
    }

    private void swapSync(int slot) {
        switch (swapConfig.getValue()) {
            case SILENT -> Managers.INVENTORY.syncToClient();
            case SILENT_ALT -> Managers.INVENTORY.setSlotAlt(slot);
        }
    }

    public boolean isSilentSwapping() {
        return packetSwapBack;
    }

    public boolean isMining(BlockPos blockPos) {
        return instantMine != null && instantMine.getPos().equals(blockPos) ||
                packetMine != null && packetMine.getPos().equals(blockPos);
    }

    private boolean isAutoMineBlock(Block block) {
        if (BlastResistantBlocks.isUnbreakable(block)) {
            return false;
        }
        return switch (selectionConfig.getValue()) {
            case WHITELIST -> ((BlockListConfig<?>) whitelistConfig).contains(block);
            case BLACKLIST -> !((BlockListConfig<?>) blacklistConfig).contains(block);
            case ALL -> true;
        };
    }

    public boolean canMine(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty();
    }

    public static class MineData implements Comparable<MineData> {
        private final BlockPos pos;
        private final Direction direction;
        private final MiningGoal goal;
        //
        private int ticksMining;
        private float blockDamage, lastDamage;

        public MineData(BlockPos pos, Direction direction) {
            this.pos = pos;
            this.direction = direction;
            this.goal = MiningGoal.MINING_ENEMY;
        }

        public MineData(BlockPos pos, Direction direction, MiningGoal goal) {
            this.pos = pos;
            this.direction = direction;
            this.goal = goal;
        }

        private double getPriority() {
            double dist = mc.player.getEyePos().squaredDistanceTo(pos.down().toCenterPos());
            if (dist <= AutoCrystalModule.getInstance().getPlaceRange()) {
                return 10.0f;
            }

            return 0.0f;
        }

        @Override
        public int compareTo(@NotNull MineData o) {
            return Double.compare(getPriority(), o.getPriority());
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof MineData d && d.getPos().equals(pos);
        }

        public void resetMiningTicks() {
            ticksMining = 0;
        }

        public void markAttemptedMine() {
            ticksMining++;
        }

        public void addBlockDamage(float blockDamage) {
            this.lastDamage = this.blockDamage;
            this.blockDamage += blockDamage;
        }

        public void setTotalBlockDamage(float blockDamage, float lastDamage) {
            this.blockDamage = blockDamage;
            this.lastDamage = lastDamage;
        }

        public BlockPos getPos() {
            return pos;
        }

        public Direction getDirection() {
            return direction;
        }

        public MiningGoal getGoal() {
            return goal;
        }

        public int getTicksMining() {
            return ticksMining;
        }

        public float getBlockDamage() {
            return blockDamage;
        }

        public float getLastDamage() {
            return lastDamage;
        }

        public static MineData empty() {
            return new MineData(BlockPos.ORIGIN, Direction.UP);
        }

        public MineData copy() {
            final MineData data = new MineData(pos, direction, goal);
            data.setTotalBlockDamage(blockDamage, lastDamage);
            return data;
        }

        public BlockState getState() {
            return mc.world.getBlockState(pos);
        }

        public int getBestSlot() {
            return AutoToolModule.getInstance().getBestToolNoFallback(getState());
        }
    }

    public record MineAnimation(MineData data, Animation animation) {
    }
}
*/
