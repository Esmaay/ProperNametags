package be.esmay.propernametags;

import be.esmay.propernametags.api.configuration.DefaultConfiguration;
import be.esmay.propernametags.common.listeners.PlayerJoinListener;
import be.esmay.propernametags.common.listeners.PlayerSneakListener;
import be.esmay.propernametags.api.objects.ProperNameTag;
import be.esmay.propernametags.common.listeners.PlayerTeleportListener;
import be.esmay.propernametags.common.tasks.MountNametagTask;
import be.esmay.propernametags.common.tasks.UpdateNameTask;
import be.esmay.propernametags.common.tasks.UpdateVisibilityTask;
import be.esmay.propernametags.utils.ChatUtils;
import be.esmay.propernametags.utils.task.SteppingTaskRegistry;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import lombok.Getter;
import lombok.Setter;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class ProperNametags extends JavaPlugin {

    @Getter
    private static ProperNametags instance;

    @Getter
    private ProtocolManager protocolManager;

    @Getter
    private final Set<ProperNameTag> nameTags = new HashSet<>();

    @Getter
    private DefaultConfiguration defaultConfiguration;

    private final AtomicInteger lastEntityId = new AtomicInteger(Integer.MAX_VALUE);

    @Getter
    private final Cache<UUID, Long> quittingPlayers = CacheBuilder.newBuilder()
            .expireAfterWrite(2, TimeUnit.SECONDS)
            .build();

    @Getter
    private final Cache<UUID, Long> teleportingPlayers = CacheBuilder.newBuilder()
            .expireAfterWrite(2, TimeUnit.SECONDS)
            .build();

    @Getter @Setter
    private boolean nameTagVisible = true;

    public ProperNametags() {
        instance = this;
    }

    @Override
    public void onLoad() {
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        this.defaultConfiguration = new DefaultConfiguration(this);
    }

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        this.getServer().getPluginManager().registerEvents(new PlayerSneakListener(this), this);
        this.getServer().getPluginManager().registerEvents(new PlayerTeleportListener(this), this);

        SteppingTaskRegistry.register(new UpdateVisibilityTask(this));
        new UpdateNameTask(this).runTaskTimer(this, 0L, this.defaultConfiguration.getUpdateInterval());
        new MountNametagTask(this).runTaskTimer(this, 0L, 5L);

        Bukkit.getScoreboardManager().getMainScoreboard().getTeams().forEach(team -> {
            team.setOption(org.bukkit.scoreboard.Team.Option.NAME_TAG_VISIBILITY, org.bukkit.scoreboard.Team.OptionStatus.NEVER);
        });
    }

    public void sendNameTag(Player player, Player viewer, boolean isLogin) {
        int entityId = this.lastEntityId.decrementAndGet();
        ProperNameTag nameTag = new ProperNameTag(player.getUniqueId(), viewer.getUniqueId(), entityId);
        this.nameTags.add(nameTag);

        PacketContainer spawnEntityPacket = new PacketContainer(PacketType.Play.Server.SPAWN_ENTITY);

        spawnEntityPacket.getIntegers().write(0, entityId);
        spawnEntityPacket.getUUIDs().write(0, UUID.randomUUID());
        spawnEntityPacket.getEntityTypeModifier().write(0, EntityType.ARMOR_STAND);

        spawnEntityPacket.getDoubles()
                .write(0, player.getLocation().getX())
                .write(1, player.getLocation().getY() + 1.8)
                .write(2, player.getLocation().getZ());

        PacketContainer sneakPacket = this.createMetaData(player, viewer, player.isSneaking(), entityId);

        this.protocolManager.sendServerPacket(viewer, spawnEntityPacket);
        this.sendMetaData(viewer, sneakPacket);

        Bukkit.getScheduler().runTaskLater(this, () -> this.sendMountNameTag(nameTag), isLogin ? 7L : 2L);
    }

    public void sendMountNameTag(ProperNameTag nameTag) {
        Player player = Bukkit.getPlayer(nameTag.getPlayer());
        Player viewer = Bukkit.getPlayer(nameTag.getViewer());
        if (player == null) return;
        if (viewer == null) return;

        int entityId = nameTag.getEntityId();

        PacketContainer packetContainer = new PacketContainer(PacketType.Play.Server.MOUNT);
        packetContainer.getIntegers().write(0, player.getEntityId());
        packetContainer.getIntegerArrays().write(0, new int[]{entityId});

        this.protocolManager.sendServerPacket(viewer, packetContainer);
    }

    public PacketContainer createMetaData(ProperNameTag nameTag, boolean sneaking) {
        return this.createMetaData(Bukkit.getPlayer(nameTag.getPlayer()), Bukkit.getPlayer(nameTag.getViewer()), sneaking, nameTag.getEntityId());
    }

    public PacketContainer createNameUpdate(ProperNameTag nameTag) {
        PacketContainer metadataPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
        metadataPacket.getIntegers().write(0, nameTag.getEntityId());

        Player player = Bukkit.getPlayer(nameTag.getPlayer());
        if (player == null) return null;

        String tag = this.defaultConfiguration.getPrefix() + this.getDefaultConfiguration().getName() + this.defaultConfiguration.getSuffix();
        tag = tag.replaceAll("%player%", player.getName());

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            tag = PlaceholderAPI.setPlaceholders(player, tag);
        }

        tag = ChatUtils.format(tag);

        List<WrappedDataValue> values = Lists.newArrayList(
                new WrappedDataValue(2, WrappedDataWatcher.Registry.getChatComponentSerializer(true), Optional.of(WrappedChatComponent.fromLegacyText(tag).getHandle()))
        );

        metadataPacket.getDataValueCollectionModifier().write(0, values);
        return metadataPacket;
    }

    public PacketContainer createMetaData(Player player, Player viewer, boolean sneaking, Integer entityId) {
        if (entityId == null) {
            ProperNameTag nameTag = this.nameTags.stream().filter(tag -> tag.getPlayer().equals(player.getUniqueId()) && tag.getViewer().equals(viewer.getUniqueId())).findFirst().orElse(null);
            if (nameTag == null) return null;

            entityId = nameTag.getEntityId();
        }

        PacketContainer metadataPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
        metadataPacket.getIntegers().write(0, entityId);

        String tag = this.defaultConfiguration.getPrefix() + this.getDefaultConfiguration().getName() + this.defaultConfiguration.getSuffix();
        tag = tag.replaceAll("%player%", player.getName());

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            tag = PlaceholderAPI.setPlaceholders(player, tag);
        }

        tag = ChatUtils.format(tag);

        List<WrappedDataValue> values = Lists.newArrayList(
                new WrappedDataValue(0, WrappedDataWatcher.Registry.get(Byte.class), sneaking ? (byte) 0x02 | 0x20 : (byte) 32),
                new WrappedDataValue(2, WrappedDataWatcher.Registry.getChatComponentSerializer(true), Optional.of(WrappedChatComponent.fromLegacyText(tag).getHandle())),
                new WrappedDataValue(3, WrappedDataWatcher.Registry.get(Boolean.class), true),
                new WrappedDataValue(15, WrappedDataWatcher.Registry.get(Byte.class), (byte) 16)
        );

        metadataPacket.getDataValueCollectionModifier().write(0, values);
        return metadataPacket;
    }

    public void sendMetaData(Player player, Player viewer, boolean sneaking, Integer entityId) {
        PacketContainer packetContainer = this.createMetaData(player, viewer, sneaking, entityId);
        this.protocolManager.sendServerPacket(viewer, packetContainer);
    }

    public void sendMetaData(ProperNameTag nameTag, boolean sneaking) {
        PacketContainer packetContainer = this.createMetaData(nameTag, sneaking);
        this.protocolManager.sendServerPacket(Bukkit.getPlayer(nameTag.getViewer()), packetContainer);
    }

    public void sendNameUpdate(ProperNameTag nameTag) {
        PacketContainer packetContainer = this.createNameUpdate(nameTag);
        this.protocolManager.sendServerPacket(Bukkit.getPlayer(nameTag.getViewer()), packetContainer);
    }

    public void sendMetaData(Player viewer, PacketContainer packetContainer) {
        this.protocolManager.sendServerPacket(viewer, packetContainer);
    }

    public boolean hasNameTag(Player player, Player viewer) {
        return this.nameTags.stream().anyMatch(nameTag -> nameTag.getPlayer().equals(player.getUniqueId()) && nameTag.getViewer().equals(viewer.getUniqueId()));
    }

    public void removeNameTags(Player player) {
        List<ProperNameTag> entities = new ArrayList<>(this.nameTags.stream().filter(nameTag -> nameTag.getPlayer().equals(player.getUniqueId())).toList());

        entities.forEach(entity -> {
            PacketContainer destroyEntityPacket = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
            destroyEntityPacket.getModifier().write(0, IntArrayList.of(entity.getEntityId()));

            this.protocolManager.sendServerPacket(player, destroyEntityPacket);
            this.nameTags.remove(entity);
        });
    }

    public void removeNameTag(ProperNameTag nameTag) {
        PacketContainer destroyEntityPacket = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
        destroyEntityPacket.getModifier().writeDefaults();
        destroyEntityPacket.getModifier().write(0, IntArrayList.of(nameTag.getEntityId()));

        this.protocolManager.sendServerPacket(Bukkit.getPlayer(nameTag.getViewer()), destroyEntityPacket);
        this.nameTags.remove(nameTag);
    }

    public void removeNameTag(Player player, Player viewer) {
        ProperNameTag nameTag = this.nameTags.stream().filter(tag -> tag.getPlayer().equals(player.getUniqueId()) && tag.getViewer().equals(viewer.getUniqueId())).findFirst().orElse(null);
        if (nameTag == null) return;

        PacketContainer destroyEntityPacket = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
        destroyEntityPacket.getModifier().writeDefaults();
        destroyEntityPacket.getModifier().write(0, IntArrayList.of(nameTag.getEntityId()));

        this.protocolManager.sendServerPacket(viewer, destroyEntityPacket);
        this.nameTags.remove(nameTag);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
