package org.inurl.jenkins.plugin;

import hudson.model.Result;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertFalse;

public class OSSPublisherPipelineTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void omittedOptionalArgumentsPreservePipelineBehavior() throws Exception {
        WorkflowRun build = runPipeline("");
        jenkins.assertBuildStatusSuccess(build);
        jenkins.assertLogContains("not exists, skipped", build);
        assertFalse(JenkinsRule.getLog(build).contains("ClassCastException"));
    }

    @Test
    public void explicitACLAndMissingFileFlagWorkInPipeline() throws Exception {
        WorkflowRun build = runPipeline(", maxRetries: '0', objectACL: 'private', throwExceptionWhenFilePathNotExisted: true");
        jenkins.assertBuildStatus(Result.FAILURE, build);
        jenkins.assertLogContains("upload fail, file [", build);
        assertFalse(JenkinsRule.getLog(build).contains("ClassCastException"));
    }

    private WorkflowRun runPipeline(String options) throws Exception {
        WorkflowJob job = jenkins.createProject(WorkflowJob.class);
        job.setDefinition(new CpsFlowDefinition("node { aliyunOSSUpload("
                + "endpoint: 'http://127.0.0.1:9', accessKeyId: 'test-key', accessKeySecret: 'test-secret', "
                + "bucketName: 'test-bucket', localPath: '/missing-artifact', remotePath: '/artifact'"
                + options + ") }", true));
        return job.scheduleBuild2(0).get();
    }
}
