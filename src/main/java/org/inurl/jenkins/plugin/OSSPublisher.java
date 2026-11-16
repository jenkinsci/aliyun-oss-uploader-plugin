package org.inurl.jenkins.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import hudson.AbortException;
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
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

public class OSSPublisher extends Publisher implements SimpleBuildStep {

    private final String endpoint;

    private final String accessKeyId;

    private final Secret accessKeySecret;

    private final String bucketName;

    private final String localPath;

    private final String remotePath;

    private final String maxRetries;

    private boolean throwExceptionWhenFilePathNotExisted;

    @CheckForNull
    private String objectACL;

    public String getEndpoint() {
        return endpoint;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public String getAccessKeySecret() {
        return Secret.toString(accessKeySecret);
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

    public String getMaxRetries() {
        return maxRetries;
    }

    private int maxRetriesAsInt() {
        return StringUtils.isEmpty(maxRetries) ? 3 : Integer.parseInt(maxRetries);
    }

    @CheckForNull
    public String getObjectACL() {
        return objectACL;
    }

    @DataBoundSetter
    public void setObjectACL(@CheckForNull String objectACL) {
        parseObjectACL(objectACL);
        this.objectACL = StringUtils.isEmpty(objectACL) ? null : objectACL;
    }

    private static CannedAccessControlList parseObjectACL(String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        try {
            CannedAccessControlList acl = CannedAccessControlList.parse(value);
            if (acl != CannedAccessControlList.Default && acl != CannedAccessControlList.Private
                    && acl != CannedAccessControlList.PublicRead && acl != CannedAccessControlList.PublicReadWrite) {
                throw new IllegalArgumentException(Messages.OSSPublish_ObjectACLNotSupport());
            }
            return acl;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(Messages.OSSPublish_ObjectACLNotSupport(), e);
        }
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

    public boolean isThrowExceptionWhenFilePathNotExisted() {
        return throwExceptionWhenFilePathNotExisted;
    }

    @DataBoundSetter
    public void setThrowExceptionWhenFilePathNotExisted(boolean throwExceptionWhenFilePathNotExisted) {
        this.throwExceptionWhenFilePathNotExisted = throwExceptionWhenFilePathNotExisted;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull FilePath workspace, @NonNull Launcher launcher,
            @NonNull TaskListener listener) throws InterruptedException, IOException {
        // Also validate settings loaded directly from existing job XML.
        parseObjectACL(objectACL);
        PrintStream logger = listener.getLogger();
        EnvVars envVars = run.getEnvironment(listener);
        OSS client = new OSSClientBuilder().build(endpoint, accessKeyId, Secret.toString(accessKeySecret));
        try {
            String local = localPath.substring(1);

            String[] remotes = remotePath.split(",");
            for (String remote : remotes) {
                remote = remote.substring(1);
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
        } finally {
            client.shutdown();
        }
    }

    private void upload(OSS client, PrintStream logger, String base, FilePath path, boolean root)
            throws InterruptedException, IOException {
        if (path.isDirectory()) {
            for (FilePath f : path.list()) {
                upload(client, logger, base + (root ? "" : ("/" + path.getName())), f, false);
            }
            return;
        }
        uploadFile(client, logger, base + "/" + path.getName(), path);
    }

    private void uploadFile(OSS client, PrintStream logger, String key, FilePath path)
            throws InterruptedException, IOException {
        if (!path.exists()) {
            if (this.throwExceptionWhenFilePathNotExisted) {
                throw new AbortException("upload fail, file [" + path.getRemote() + "] not exists");
            }
            logger.println("file [" + path.getRemote() + "] not exists, skipped");
            return;
        }
        int maxRetries = maxRetriesAsInt();
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

    private void uploadFile0(OSS client, PrintStream logger, String key, FilePath path)
            throws InterruptedException, IOException {
        String realKey = key;
        if (realKey.startsWith("/")) {
            realKey = realKey.substring(1);
        }
        CannedAccessControlList acl = parseObjectACL(objectACL);
        try (InputStream inputStream = path.read()) {
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, realKey, inputStream);
            if (acl != null) {
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setObjectAcl(acl);
                putObjectRequest.setMetadata(metadata);
            }
            logger.println("uploading [" + path.getRemote() + "] to [" + realKey + "] with acl ["
                    + (acl == null ? "unspecified" : acl) + "]");
            client.putObject(putObjectRequest);
        }
    }

    @Symbol("aliyunOSSUpload")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @POST
        public FormValidation doCheckMaxRetries(@QueryParameter String value) {
            if (StringUtils.isEmpty(value)) {
                return FormValidation.ok();
            }
            try {
                Integer.parseInt(value);
            } catch (Exception e) {
                return FormValidation.error(Messages.OSSPublish_MaxRetiesMustBeNumbers());
            }
            return FormValidation.ok();
        }

        @POST
        public FormValidation doCheckEndpoint(@QueryParameter String value) {
            return checkValue(value, Messages.OSSPublish_MissingEndpoint());
        }

        @POST
        public FormValidation doCheckAccessKeyId(@QueryParameter String value) {
            return checkValue(value, Messages.OSSPublish_MissingAccessKeyId());
        }

        @POST
        public FormValidation doCheckAccessKeySecret(@QueryParameter String value) {
            return checkValue(value, Messages.OSSPublish_MissingAccessKeySecret());
        }

        @POST
        public FormValidation doCheckBucketName(@QueryParameter String value) {
            return checkValue(value, Messages.OSSPublish_MissingBucketName());
        }

        @POST
        public FormValidation doCheckLocalPath(@QueryParameter String value) {
            return checkBeginWithSlash(value);
        }

        @POST
        public FormValidation doCheckRemotePath(@QueryParameter String value) {
            return checkBeginWithSlash(value);
        }

        private FormValidation checkBeginWithSlash(String value) {
            if (value == null || !value.startsWith("/")) {
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

        public ListBoxModel doFillObjectACLItems(@QueryParameter final String objectACL) {
            ListBoxModel items = new ListBoxModel();

            items.add(Messages.OSSPublish_ObjectACLUnspecified(), "");
            items.add(Messages.OSSPublish_ObjectACLDefault(), CannedAccessControlList.Default.toString());
            items.add(Messages.OSSPublish_ObjectACLPrivate(), CannedAccessControlList.Private.toString());
            items.add(Messages.OSSPublish_ObjectACLPublicRead(), CannedAccessControlList.PublicRead.toString());
            items.add(Messages.OSSPublish_ObjectACLPublicReadWrite(), CannedAccessControlList.PublicReadWrite.toString());

            ListBoxModel.Option option = items.stream()
                    .filter(item -> item.value.equals(StringUtils.defaultString(objectACL)))
                    .findAny().orElse(null);

            if (option != null) {
                option.selected = true;
            } else {
                items.get(0).selected = true;
            }

            return items;
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.OSSPublish_DisplayName();
        }
    }

}
