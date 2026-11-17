package org.inurl.jenkins.plugin;

import org.jenkinsci.plugins.structs.describable.DescribableModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.assertEquals;

@WithJenkins
class OSSPublisherTest {

    private JenkinsRule jenkins;

    @BeforeEach
    void beforeEach(JenkinsRule rule) {
        jenkins = rule;
    }

    @Test
    void maxRetriesSurvivesDescribableRoundTrip() throws Exception {
        DescribableModel<OSSPublisher> model = new DescribableModel<>(OSSPublisher.class);
        for (String maxRetries : new String[] {null, "", "0", "5"}) {
            OSSPublisher publisher = new OSSPublisher("oss-example.aliyuncs.com", "access-key-id",
                    "access-key-secret", "example-bucket", "/test.txt", "/test.txt", maxRetries);

            OSSPublisher restored = model.uninstantiate2(publisher).instantiate(OSSPublisher.class);
            assertEquals(maxRetries, restored.getMaxRetries());
        }
    }
}
