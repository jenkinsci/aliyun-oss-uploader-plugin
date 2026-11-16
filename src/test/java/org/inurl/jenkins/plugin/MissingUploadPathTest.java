package org.inurl.jenkins.plugin;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import java.util.HashMap;
import java.util.Map;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.structs.describable.DescribableModel;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class MissingUploadPathTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void legacyConfigurationStillSkipsMissingFiles() throws Exception {
        OSSPublisher publisher = (OSSPublisher) Jenkins.XSTREAM2.fromXML(
                getClass().getResource("legacy-publisher.xml"));
        assertFalse(publisher.isThrowExceptionWhenFilePathNotExisted());
        FreeStyleBuild build = buildWith(publisher);
        jenkins.assertBuildStatusSuccess(build);
        jenkins.assertLogContains("not exists, skipped", build);
    }

    @Test
    public void missingFilesAreSkippedWhenExplicitlyDisabled() throws Exception {
        OSSPublisher publisher = publisher();
        publisher.setThrowExceptionWhenFilePathNotExisted(false);
        FreeStyleBuild build = buildWith(publisher);
        jenkins.assertBuildStatusSuccess(build);
        jenkins.assertLogContains("not exists, skipped", build);
    }

    @Test
    public void missingFilesFailTheBuildWhenEnabled() throws Exception {
        OSSPublisher publisher = publisher();
        publisher.setThrowExceptionWhenFilePathNotExisted(true);
        FreeStyleBuild build = buildWith(publisher);
        jenkins.assertBuildStatus(Result.FAILURE, build);
        jenkins.assertLogContains("upload fail, file [", build);
        assertFalse(JenkinsRule.getLog(build).contains("not exists, skipped"));
        assertFalse(JenkinsRule.getLog(build).contains("NullPointerException"));
    }

    @Test
    public void checkboxSurvivesConfigurationRoundTripAndReload() throws Exception {
        for (boolean enabled : new boolean[] {false, true}) {
            FreeStyleProject project = jenkins.createFreeStyleProject();
            OSSPublisher publisher = publisher();
            publisher.setThrowExceptionWhenFilePathNotExisted(enabled);
            project.getPublishersList().add(publisher);

            jenkins.configRoundtrip(project);
            assertEquals(enabled, project.getPublishersList().get(OSSPublisher.class)
                    .isThrowExceptionWhenFilePathNotExisted());
            project.doReload();
            assertEquals(enabled, project.getPublishersList().get(OSSPublisher.class)
                    .isThrowExceptionWhenFilePathNotExisted());
        }
    }

    @Test
    public void pipelineArgumentsSupportOmissionAndBothBooleanValues() throws Exception {
        DescribableModel<OSSPublisher> model = new DescribableModel<>(OSSPublisher.class);
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("endpoint", "http://127.0.0.1:9");
        arguments.put("accessKeyId", "test-key");
        arguments.put("accessKeySecret", "test-secret");
        arguments.put("bucketName", "test-bucket");
        arguments.put("localPath", "/missing-artifact");
        arguments.put("remotePath", "/artifact");
        arguments.put("maxRetries", "0");

        assertFalse(model.instantiate(arguments).isThrowExceptionWhenFilePathNotExisted());
        for (boolean enabled : new boolean[] {false, true}) {
            arguments.put("throwExceptionWhenFilePathNotExisted", enabled);
            OSSPublisher publisher = model.instantiate(arguments);
            assertEquals(enabled, publisher.isThrowExceptionWhenFilePathNotExisted());
            OSSPublisher restored = model.uninstantiate2(publisher).instantiate(OSSPublisher.class);
            assertEquals(enabled, restored.isThrowExceptionWhenFilePathNotExisted());
        }
    }

    private OSSPublisher publisher() {
        return new OSSPublisher("http://127.0.0.1:9", "test-key", "test-secret",
                "test-bucket", "/missing-artifact", "/artifact", "0");
    }

    private FreeStyleBuild buildWith(OSSPublisher publisher) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getPublishersList().add(publisher);
        return project.scheduleBuild2(0).get();
    }
}
