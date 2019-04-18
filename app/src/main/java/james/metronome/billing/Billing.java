package james.metronome.billing;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import james.metronome.Metronome;

public class Billing {

    public static final int REQUEST_PURCHASE = 614;

    private static final String[] CLASSNAMES = {
            "james.metronome.billing.GplayBillingProvider",
            "james.metronome.billing.OssBillingProvider"
    };

    @Nullable
    public static BillingInterface get(Metronome metronome) {
        for (String className : CLASSNAMES) {
            try {
                Constructor<BillingInterface> constructor = (Constructor<BillingInterface>) Class.forName(className).getConstructor(Metronome.class);
                constructor.setAccessible(true);
                return constructor.newInstance(metronome);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

}
