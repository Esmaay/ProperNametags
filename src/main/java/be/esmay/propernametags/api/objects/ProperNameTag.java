package be.esmay.propernametags.api.objects;

import lombok.Data;

import java.util.UUID;

@Data
public final class ProperNameTag {

    private final UUID player;
    private final UUID viewer;

    private final int entityId;

}
