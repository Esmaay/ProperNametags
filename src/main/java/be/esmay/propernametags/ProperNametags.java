package be.esmay.propernametags;

import be.esmay.propernametags.api.configuration.DefaultConfiguration;
import be.esmay.propernametags.api.objects.ProperNameTag;
import be.esmay.propernametags.commands.ProperNametagsCommand;
import be.esmay.propernametags.common.listeners.PlayerJoinListener;
import be.esmay.propernametags.common.listeners.PlayerSneakListener;
import be.esmay.propernametags.common.listeners.PlayerTeleportListener;
import be.esmay.propernametags.common.tasks.MountNametagTask;
import be.esmay.propernametags.common.tasks.UpdateNameTask;
import be.esmay.propernametags.common.tasks.UpdateVisibilityTask;
import be.esmay.propernametags.utils.ChatUtils;
import be.esmay.propernametags.utils.task.SteppingTaskRegistry;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPassengers;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import lombok.Getter;
import lombok.Setter;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class ProperNametags extends JavaPlugin {

    @Getter
    private static ProperNametags instance;

    @Getter
    private final Set<ProperNameTag> nameTags = new HashSet<>();

    @Getter
    @Setter
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

    @Getter
    @Setter
    private boolean nameTagVisible = true;

    public ProperNametags() {
        instance = this;
    }

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();

        this.defaultConfiguration = new DefaultConfiguration(this);
    }

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        this.getServer().getPluginManager().registerEvents(new PlayerSneakListener(this), this);
        this.getServer().getPluginManager().registerEvents(new PlayerTeleportListener(this), this);

        getCommand("propernametags").setExecutor(new ProperNametagsCommand(this));

        SteppingTaskRegistry.register(new UpdateVisibilityTask(this));
        new UpdateNameTask(this).runTaskTimer(this, 0L, this.defaultConfiguration.getUpdateInterval());
        new MountNametagTask(this).runTaskTimer(this, 0L, 5L);

        Bukkit.getScoreboardManager().getMainScoreboard().getTeams().forEach(team -> {
            team.setOption(org.bukkit.scoreboard.Team.Option.NAME_TAG_VISIBILITY, org.bukkit.scoreboard.Team.OptionStatus.NEVER);
        });

        PacketEvents.getAPI().init();
    }

    public void sendNameTag(Player player, Player viewer, boolean isLogin) {
        int entityId = this.lastEntityId.decrementAndGet();
        ProperNameTag nameTag = new ProperNameTag(player.getUniqueId(), viewer.getUniqueId(), entityId);
        this.nameTags.add(nameTag);

        WrapperPlayServerSpawnEntity spawnEntityPacket = new WrapperPlayServerSpawnEntity(
                entityId,
                UUID.randomUUID(),
                EntityTypes.ARMOR_STAND,
                new Location(
                        player.getLocation().getX(),
                        player.getLocation().getY() + 1.8,
                        player.getLocation().getZ(),
                        player.getLocation().getYaw(),
                        player.getLocation().getPitch()
                ),
                player.getYaw(),
                0,
                null);

        WrapperPlayServerEntityMetadata entityMetadataPacket = this.createMetaData(player, viewer, player.isSneaking(), entityId);
        if (entityMetadataPacket == null) return;

        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, spawnEntityPacket);
        this.sendMetaData(viewer, entityMetadataPacket);

        Bukkit.getScheduler().runTaskLater(this, () -> this.sendMountNameTag(nameTag), isLogin ? 7L : 2L);
    }

    public void sendMountNameTag(ProperNameTag nameTag) {
        Player player = Bukkit.getPlayer(nameTag.getPlayer());
        Player viewer = Bukkit.getPlayer(nameTag.getViewer());
        if (player == null) return;
        if (viewer == null) return;

        int entityId = nameTag.getEntityId();

        WrapperPlayServerSetPassengers packet = new WrapperPlayServerSetPassengers(player.getEntityId(), new int[]{entityId});
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
    }

    public WrapperPlayServerEntityMetadata createMetaData(ProperNameTag nameTag, boolean sneaking) {
        return this.createMetaData(Bukkit.getPlayer(nameTag.getPlayer()), Bukkit.getPlayer(nameTag.getViewer()), sneaking, nameTag.getEntityId());
    }

    public WrapperPlayServerEntityMetadata createNameUpdate(ProperNameTag nameTag) {
        Player player = Bukkit.getPlayer(nameTag.getPlayer());
        if (player == null) return null;

        String tag = this.defaultConfiguration.getPrefix() + this.getDefaultConfiguration().getName() + this.defaultConfiguration.getSuffix();
        tag = tag.replaceAll("%player%", player.getName());

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            tag = PlaceholderAPI.setPlaceholders(player, tag);
        }

        Component tagComponent = ChatUtils.format(tag);

        List<EntityData> entityData = List.of(
                new EntityData(2, EntityDataTypes.OPTIONAL_ADV_COMPONENT, Optional.of(tagComponent))
        );

        return new WrapperPlayServerEntityMetadata(
                nameTag.getEntityId(),
                entityData
        );
    }

    public WrapperPlayServerEntityMetadata createMetaData(Player player, Player viewer, boolean sneaking, Integer entityId) {
        if (entityId == null) {
            ProperNameTag nameTag = this.nameTags.stream().filter(tag -> tag.getPlayer().equals(player.getUniqueId()) && tag.getViewer().equals(viewer.getUniqueId())).findFirst().orElse(null);
            if (nameTag == null) return null;

            entityId = nameTag.getEntityId();
        }

        String tag = this.defaultConfiguration.getPrefix() + this.getDefaultConfiguration().getName() + this.defaultConfiguration.getSuffix();
        tag = tag.replaceAll("%player%", player.getName());

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            tag = PlaceholderAPI.setPlaceholders(player, tag);
        }

        Component tagComponent = ChatUtils.format(tag);

        List<EntityData> entityData = List.of(
                new EntityData(0, EntityDataTypes.BYTE, sneaking ? (byte) 0x02 | 0x20 : (byte) 32),
                new EntityData(2, EntityDataTypes.OPTIONAL_ADV_COMPONENT, Optional.of(tagComponent)),
                new EntityData(3, EntityDataTypes.BOOLEAN, true),
                new EntityData(15, EntityDataTypes.BYTE, (byte) 16)
        );

        return new WrapperPlayServerEntityMetadata(entityId, entityData);
    }

    public void sendMetaData(Player player, Player viewer, boolean sneaking, Integer entityId) {
        WrapperPlayServerEntityMetadata packetContainer = this.createMetaData(player, viewer, sneaking, entityId);
        if (packetContainer == null) return;

        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packetContainer);
    }

    public void sendMetaData(ProperNameTag nameTag, boolean sneaking) {
        WrapperPlayServerEntityMetadata packetContainer = this.createMetaData(nameTag, sneaking);
        if (packetContainer == null) return;

        Player player = Bukkit.getPlayer(nameTag.getViewer());
        if (player == null) return;

        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packetContainer);
    }

    public void sendNameUpdate(ProperNameTag nameTag) {
        WrapperPlayServerEntityMetadata packetContainer = this.createNameUpdate(nameTag);
        if (packetContainer == null) return;

        Player player = Bukkit.getPlayer(nameTag.getViewer());
        if (player == null) return;

        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packetContainer);
    }

    public void sendMetaData(Player viewer, @NotNull WrapperPlayServerEntityMetadata packetContainer) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packetContainer);
    }

    public boolean hasNameTag(Player player, Player viewer) {
        return this.nameTags.stream().anyMatch(nameTag -> nameTag.getPlayer().equals(player.getUniqueId()) && nameTag.getViewer().equals(viewer.getUniqueId()));
    }

    public void removeNameTags(Player player) {
        List<ProperNameTag> entities = new ArrayList<>(this.nameTags.stream().filter(nameTag -> nameTag.getPlayer().equals(player.getUniqueId())).toList());

        WrapperPlayServerDestroyEntities packet = new WrapperPlayServerDestroyEntities(entities.stream().mapToInt(ProperNameTag::getEntityId).toArray());
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);

        entities.forEach(this.nameTags::remove);
    }

    public void removeNameTag(ProperNameTag nameTag) {
        WrapperPlayServerDestroyEntities packet = new WrapperPlayServerDestroyEntities(nameTag.getEntityId());
        Player player = Bukkit.getPlayer(nameTag.getViewer());
        if (player == null) return;

        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
        this.nameTags.remove(nameTag);
    }

    public void removeNameTag(Player player, Player viewer) {
        ProperNameTag nameTag = this.nameTags.stream().filter(tag -> tag.getPlayer().equals(player.getUniqueId()) && tag.getViewer().equals(viewer.getUniqueId())).findFirst().orElse(null);
        if (nameTag == null) return;

        WrapperPlayServerDestroyEntities packet = new WrapperPlayServerDestroyEntities(nameTag.getEntityId());
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
        this.nameTags.remove(nameTag);
    }

    @Override
    public void onDisable() {
        PacketEvents.getAPI().terminate();
    }
}
