package org.inurl.jenkins.plugin;

import org.jenkinsci.plugins.structs.describable.DescribableModel;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertEquals;

public class OSSPublisherTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void maxRetriesSurvivesDescribableRoundTrip() throws Exception {
        DescribableModel<OSSPublisher> model = new DescribableModel<>(OSSPublisher.class);
        for (String maxRetries : new String[] {null, "", "0", "5"}) {
            OSSPublisher publisher = new OSSPublisher("oss-example.aliyuncs.com", "access-key-id",
                    "access-key-secret", "example-bucket", "/test.txt", "/test.txt", maxRetries);

            OSSPublisher restored = model.uninstantiate2(publisher).instantiate(OSSPublisher.class);
            assertEquals(maxRetries, restored.getMaxRetries());
        }
    }
}
