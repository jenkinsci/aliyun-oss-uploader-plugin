package org.inurl.jenkins.plugin;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;
import org.inurl.jenkins.plugin.Messages;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.IOException;

public class OSSPublisher extends Publisher implements SimpleBuildStep {

    private final String endpoint;

    private final String accessKeyId;

    private final String accessKeySecret;

    private final String bucketName;

    private final String localPath;

    private final String remotePath;


    public String getEndpoint() {
        return endpoint;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getLocalPath() {
        return localPath;
    }

    public String getRemotePath() {
        return remotePath;
    }

    @DataBoundConstructor
    public OSSPublisher(String endpoint, String accessKeyId, String accessKeySecret, String bucketName, String localPath, String remotePath) {
        this.endpoint = endpoint;
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.bucketName = bucketName;
        this.localPath = localPath;
        this.remotePath = remotePath;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        listener.getLogger().println("endpoint          => " + endpoint + "!");
        listener.getLogger().println("accessKeyId       => " + accessKeyId + "!");
        listener.getLogger().println("accessKeySecret   => " + accessKeySecret + "!");
        listener.getLogger().println("bucketName        => " + bucketName + "!");
        listener.getLogger().println("localPath         => " + localPath + "!");
        listener.getLogger().println("remotePath        => " + remotePath + "!");
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public FormValidation doCheckEndpoint(@QueryParameter String value) {
            return checkValue(value, Messages.OSSPublish_MissingEndpoint());
        }

        public FormValidation doCheckAccessKeyId(@QueryParameter String value) {
            return checkValue(value, Messages.OSSPublish_MissingAccessKeyId());
        }

        public FormValidation doCheckAccessKeySecret(@QueryParameter String value) {
            return checkValue(value, Messages.OSSPublish_MissingAccessKeySecret());
        }


        public FormValidation doCheckBucketName(@QueryParameter String value) {
            return checkValue(value, Messages.OSSPublish_MissingBucketName());
        }

        public FormValidation doCheckLocalPath(@QueryParameter String value) {
            return checkValue(value, Messages.OSSPublish_MissingLocalPath());
        }

        public FormValidation doCheckRemotePath(@QueryParameter String value) {
            return checkValue(value, Messages.OSSPublish_MissingRemotePath());
        }

        private FormValidation checkValue(String value, String message) {
            if (StringUtils.isEmpty(value)) {
                return FormValidation.error(message);
            }
            return FormValidation.ok();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.OSSPublish_DisplayName();
        }
    }

}
