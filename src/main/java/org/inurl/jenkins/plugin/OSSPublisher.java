package org.inurl.jenkins.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import javax.annotation.Nonnull;

import com.aliyun.oss.OSSClient;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.EnvVars;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class OSSPublisher extends Publisher implements SimpleBuildStep {

    private final String endpoint;

    private final String accessKeyId;

    private final Secret accessKeySecret;

    private final String bucketName;

    private final String localPath;

    private final String remotePath;

    private final String maxRetries;

    public String getEndpoint() {
        return endpoint;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret.getPlainText();
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

    public int getMaxRetries() {
        return StringUtils.isEmpty(maxRetries) ? 3 : Integer.parseInt(maxRetries);
    }

    @DataBoundConstructor
    public OSSPublisher(String endpoint, String accessKeyId, String accessKeySecret, String bucketName,
        String localPath, String remotePath, String maxRetries) {
        this.endpoint = endpoint;
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = Secret.fromString(accessKeySecret);
        this.bucketName = bucketName;
        this.localPath = localPath;
        this.remotePath = remotePath;
        this.maxRetries = maxRetries;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher,
        @Nonnull TaskListener listener) throws InterruptedException, IOException {
        PrintStream logger = listener.getLogger();
	EnvVars envVars = run.getEnvironment(listener);
        OSSClient client = new OSSClient(endpoint, accessKeyId, accessKeySecret.getPlainText());
        String local = localPath.substring(1);
        String remote = remotePath.substring(1);
        
	String expandLocal = envVars.expand(local);
	String expandRemote = envVars.expand(remote);
	logger.println("expandLocalPath =>" + expandLocal);
	logger.println("expandRemotePath =>" + expandRemote);
	FilePath p = new FilePath(workspace, expandLocal);
        if (p.isDirectory()) {
            logger.println("upload dir => " + p);
            upload(client, logger, expandRemote, p, true);
            logger.println("upload dir success");
        } else {
            logger.println("upload file => " + p);
            uploadFile(client, logger, expandRemote, p);
            logger.println("upload file success");
        }

    }

    private void upload(OSSClient client, PrintStream logger, String base, FilePath path, boolean root)
        throws InterruptedException, IOException {
        if (path.isDirectory()) {
            for (FilePath f : path.list()) {
                upload(client, logger, base + (root ? "" : ("/" + path.getName())), f, false);
            }
            return;
        }
        uploadFile(client, logger, base + "/" + path.getName(), path);
    }

    private void uploadFile(OSSClient client, PrintStream logger, String key, FilePath path)
        throws InterruptedException, IOException {
        if (!path.exists()) {
            logger.println("file [" + path.getRemote() + "] not exists, skipped");
            return;
        }
        int maxRetries = getMaxRetries();
        int retries = 0;
        do {
            if (retries > 0) {
                logger.println("upload retrying (" + retries + "/" + maxRetries + ")");
            }
            try {
                uploadFile0(client, logger, key, path);
                return;
            } catch (Exception e) {
                e.printStackTrace(logger);
            }
        } while ((++retries) <= maxRetries);
        throw new RuntimeException("upload fail, more than the max of retries");
    }

    private void uploadFile0(OSSClient client, PrintStream logger, String key, FilePath path)
        throws InterruptedException, IOException {
        String realKey = key;
        if (realKey.startsWith("/")) {
            realKey = realKey.substring(1);
        }

        InputStream inputStream = path.read();
        logger.println("uploading [" + path.getRemote() + "] to [" + realKey + "]");
        client.putObject(bucketName, realKey, inputStream);
    }

    @Symbol("aliyunOSSUpload")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public FormValidation doCheckMaxRetries(@QueryParameter String value) {
            try {
                Integer.parseInt(value);
            } catch (Exception e) {
                return FormValidation.error(Messages.OSSPublish_MaxRetiesMustBeNumbers());
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckEndpoint(@QueryParameter(required = true) String value) {
            return checkValue(value, Messages.OSSPublish_MissingEndpoint());
        }

        public FormValidation doCheckAccessKeyId(@QueryParameter(required = true) String value) {
            return checkValue(value, Messages.OSSPublish_MissingAccessKeyId());
        }

        public FormValidation doCheckAccessKeySecret(@QueryParameter(required = true) String value) {
            return checkValue(value, Messages.OSSPublish_MissingAccessKeySecret());
        }

        public FormValidation doCheckBucketName(@QueryParameter(required = true) String value) {
            return checkValue(value, Messages.OSSPublish_MissingBucketName());
        }

        public FormValidation doCheckLocalPath(@QueryParameter(required = true) String value) {
            return checkBeginWithSlash(value);
        }

        public FormValidation doCheckRemotePath(@QueryParameter(required = true) String value) {
            return checkBeginWithSlash(value);
        }

        private FormValidation checkBeginWithSlash(String value) {
            if (!value.startsWith("/")) {
                return FormValidation.error(Messages.OSSPublish_MustBeginWithSlash());
            }
            return FormValidation.ok();
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
