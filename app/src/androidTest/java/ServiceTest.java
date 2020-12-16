import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.test.InstrumentationRegistry;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ServiceTestRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.microg.nlp.client.UnifiedLocationClient;
import org.microg.nlp.service.UnifiedLocationService;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(AndroidJUnit4.class)
public class ServiceTest {

    @Rule
    public final ServiceTestRule serviceRule = new ServiceTestRule();

    @Test
    public void testWithBoundService() throws Exception {
        Intent serviceIntent =
                new Intent(ApplicationProvider.getApplicationContext(),
                        UnifiedLocationService.class);
        serviceRule.startService(serviceIntent);
        // TODO seems that this does not bind to service -> Get the service and to something with it.
        UnifiedLocationClient test = UnifiedLocationClient.get(ApplicationProvider.getApplicationContext());
    }
}
