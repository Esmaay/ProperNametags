package be.esmay.propernametags;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class NameTagAPI {

    public static void enableNameTags() {
        ProperNametags.getInstance().setNameTagVisible(true);
    }

    public static void disableNameTags() {
        ProperNametags.getInstance().setNameTagVisible(false);
    }

}
