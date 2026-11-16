package org.inurl.jenkins.plugin;

import com.sun.net.httpserver.HttpServer;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.util.ListBoxModel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.structs.describable.DescribableModel;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ObjectACLTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void pipelineArgumentsSurviveRoundTrip() throws Exception {
        DescribableModel<OSSPublisher> model = new DescribableModel<>(OSSPublisher.class);
        for (String acl : new String[] {null, "", "default", "private", "public-read", "public-read-write"}) {
            OSSPublisher publisher = publisher("http://127.0.0.1:9");
            publisher.setObjectACL(acl);
            publisher.setThrowExceptionWhenFilePathNotExisted(true);
            OSSPublisher restored = model.uninstantiate2(publisher).instantiate(OSSPublisher.class);
            assertEquals(acl == null || acl.isEmpty() ? null : acl, restored.getObjectACL());
            assertEquals("0", restored.getMaxRetries());
            assertTrue(restored.isThrowExceptionWhenFilePathNotExisted());
        }
    }

    @Test
    public void configurationRoundTripPreservesACLAndLegacyDefaults() throws Exception {
        for (String acl : new String[] {null, "default", "private", "public-read", "public-read-write"}) {
            OSSPublisher publisher;
            if (acl == null) {
                publisher = (OSSPublisher) Jenkins.XSTREAM2.fromXML(getClass().getResource("legacy-publisher.xml"));
                assertNull(publisher.getObjectACL());
            } else {
                publisher = publisher("http://127.0.0.1:9");
                publisher.setObjectACL(acl);
            }
            FreeStyleProject project = jenkins.createFreeStyleProject();
            project.getPublishersList().add(publisher);
            jenkins.configRoundtrip(project);
            assertEquals(acl, project.getPublishersList().get(OSSPublisher.class).getObjectACL());
            project.doReload();
            assertEquals(acl, project.getPublishersList().get(OSSPublisher.class).getObjectACL());
        }
    }

    @Test
    public void invalidACLIsRejectedWithoutReplacingExistingSetting() {
        OSSPublisher publisher = publisher("http://127.0.0.1:9");
        publisher.setObjectACL("private");
        for (String invalid : new String[] {"invalid", "Unknown", "authenticated-read", " private "}) {
            try {
                publisher.setObjectACL(invalid);
                fail("Expected invalid object ACL to be rejected: " + invalid);
            } catch (IllegalArgumentException expected) {
                assertEquals("private", publisher.getObjectACL());
            }
        }
    }

    @Test
    public void selectorHandlesUnsetAndExplicitValues() {
        OSSPublisher.DescriptorImpl descriptor = jenkins.jenkins.getDescriptorByType(OSSPublisher.DescriptorImpl.class);
        for (String acl : new String[] {null, "", "default", "private", "public-read", "public-read-write"}) {
            ListBoxModel options = descriptor.doFillObjectACLItems(acl);
            int selected = 0;
            for (ListBoxModel.Option option : options) {
                if (option.selected) {
                    selected++;
                    assertEquals(acl == null ? "" : acl, option.value);
                }
            }
            assertEquals(1, selected);
        }
    }

    @Test
    public void uploadRequestsSendOnlyExplicitACLHeaders() throws Exception {
        BlockingQueue<Upload> uploads = new LinkedBlockingQueue<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            ByteArrayOutputStream body = new ByteArrayOutputStream();
            try (InputStream input = exchange.getRequestBody()) {
                byte[] buffer = new byte[1024];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    body.write(buffer, 0, count);
                }
            }
            uploads.add(new Upload(exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
                    exchange.getRequestHeaders().getFirst("x-oss-object-acl"), body.toString("UTF-8")));
            exchange.getResponseHeaders().set("ETag", "\"d41d8cd98f00b204e9800998ecf8427e\"");
            exchange.getResponseHeaders().set("x-oss-request-id", "test-request");
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();
        try {
            for (String acl : new String[] {null, "", "default", "private", "public-read", "public-read-write"}) {
                FreeStyleProject project = jenkins.createFreeStyleProject();
                project.getBuildersList().add(new TestBuilder() {
                    @Override
                    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                            throws IOException, InterruptedException {
                        build.getWorkspace().child("artifact.txt").write("fixture-data", "UTF-8");
                        return true;
                    }
                });
                OSSPublisher publisher = publisher("http://127.0.0.1:" + server.getAddress().getPort());
                publisher.setObjectACL(acl);
                publisher.setThrowExceptionWhenFilePathNotExisted(true);
                project.getPublishersList().add(publisher);
                FreeStyleBuild build = project.scheduleBuild2(0).get();
                jenkins.assertBuildStatusSuccess(build);

                Upload upload = uploads.poll(5, TimeUnit.SECONDS);
                assertNotNull(upload);
                assertEquals("PUT", upload.method);
                assertTrue(upload.path.endsWith("/artifact.txt"));
                assertEquals(acl == null || acl.isEmpty() ? null : acl, upload.acl);
                assertEquals("fixture-data", upload.body);
            }
            assertTrue(uploads.isEmpty());
        } finally {
            server.stop(0);
        }
    }

    private OSSPublisher publisher(String endpoint) {
        return new OSSPublisher(endpoint, "test-key", "test-secret", "test-bucket",
                "/artifact.txt", "/artifact.txt", "0");
    }

    private static final class Upload {
        private final String method;
        private final String path;
        private final String acl;
        private final String body;

        private Upload(String method, String path, String acl, String body) {
            this.method = method;
            this.path = path;
            this.acl = acl;
            this.body = body;
        }
    }
}
