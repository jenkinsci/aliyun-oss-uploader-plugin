package org.inurl.jenkins.plugin;

import com.aliyun.oss.OSSClient;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.tasks.Shell;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertTrue;

public class OSSPublisherTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    private final String name = "Test";

    private final String endpoint = System.getenv("OSS_ENDPOINT");
    private final String accessKeyId = System.getenv("OSS_ACCESS_KEY_ID");
    private final String accessKeySecret = System.getenv("OSS_ACCESS_KEY_SECRET");
    private final String bucketName = System.getenv("OSS_BUCKET_NAME");
    private final String maxRetries = "10";

    public static void main(String[] args) {
        System.out.println(System.getenv("os").contains("Windows"));
    }

    @Test
    public void testOSS() {
        OSSClient client = new OSSClient(endpoint, accessKeyId, accessKeySecret);
        client.getBucketInfo(bucketName);
        assertTrue(true);
    }

    @Test
    public void testConfigRoundtrip() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject(name);
        String localPath = "/";
        String remotePath = "/";
        project.getPublishersList().add(new OSSPublisher(endpoint, accessKeyId, accessKeySecret, bucketName, localPath, remotePath, maxRetries));
        project = jenkins.configRoundtrip(project);
        jenkins.assertEqualDataBoundBeans(
                new OSSPublisher(endpoint, accessKeyId, accessKeySecret, bucketName, localPath, remotePath, maxRetries),
                project.getPublishersList().get(0));
    }


    @Test
    public void testBuildDir() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject(name);
        Shell shell = createFileShell();
        project.getBuildersList().add(shell);
        project.getPublishersList().add(new OSSPublisher(endpoint, accessKeyId, accessKeySecret, bucketName, "/", "/", maxRetries));
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("upload dir success", build);
    }

    @Test
    public void testBuildFile() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject(name);
        project.getPublishersList().add(new OSSPublisher(endpoint, accessKeyId, accessKeySecret, bucketName, "/n/3", "/remoteFile", maxRetries));
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("upload file success", build);
    }

    private static Shell createFileShell() {
        //language=Bash
        String del =
                "if [[ -e \"a\" ]];then rm -f a fi\n" +
                "if [[ -e \"b\" ]];then rm -f b fi\n" +
                "if [[ -e \"c\" ]];then rm -f c fi\n" +
                "if [[ -e \"n/1\" ]];then rm -f n/1 fi\n" +
                "if [[ -e \"n/2\" ]];then rm -f n/2 fi\n" +
                "if [[ -e \"n/3\" ]];then rm -f n/3 fi\n";
        //language=Bash
        String create =
                "echo a > a\n" +
                "echo b > b\n" +
                "echo c > c\n" +
                "mkdir n\n" +
                "echo 1 > n/1\n" +
                "echo 2 > n/2\n" +
                "echo 3 > n/3";
        return new Shell(del + "\n" + create);
    }


}