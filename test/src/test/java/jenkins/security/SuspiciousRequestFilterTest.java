package jenkins.security;

import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import hudson.ExtensionList;
import hudson.model.UnprotectedRootAction;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.GET;

import javax.annotation.CheckForNull;
import javax.servlet.http.HttpServletResponse;
import java.net.URL;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@Issue("SECURITY-1774")
public class SuspiciousRequestFilterTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    private WebResponse get(String path) throws Exception {
        return j.createWebClient()
                .withThrowExceptionOnFailingStatusCode(false)
                .getPage(new WebRequest(new URL(j.getURL(), path)))
                .getWebResponse();
    }

    @Test
    public void denySemicolonInRequestPathByDefault() throws Exception {
        WebResponse response = get("foo/bar/..;/?baz=bruh");
        assertThat(Foo.getInstance().baz, is(nullValue()));
        assertThat(response.getStatusCode(), is(HttpServletResponse.SC_BAD_REQUEST));
        assertThat(response.getContentAsString(), containsString("Semicolons are not allowed in the request URI"));
    }

    @Test
    public void allowSemicolonsInRequestPathWhenEscapeHatchEnabled() throws Exception {
        SuspiciousRequestFilter.allowSemicolonsInPath = true;
        try {
            WebResponse response = get("foo/bar/..;/..;/cli?baz=bruh");
            assertThat(Foo.getInstance().baz, is("bruh"));
            assertThat(response.getStatusCode(), is(HttpServletResponse.SC_OK));
        } finally {
            SuspiciousRequestFilter.allowSemicolonsInPath = false;
        }
    }

    @Test
    public void allowSemicolonsInQueryParameters() throws Exception {
        WebResponse response = get("foo/bar?baz=foo;bar=baz");
        assertThat(Foo.getInstance().baz, is("foo;bar=baz"));
        assertThat(response.getStatusCode(), is(HttpServletResponse.SC_OK));
    }

    @TestExtension
    public static class Foo implements UnprotectedRootAction {

        private static Foo getInstance() {
            return ExtensionList.lookupSingleton(Foo.class);
        }

        private String baz;

        @CheckForNull
        @Override
        public String getIconFileName() {
            return null;
        }

        @CheckForNull
        @Override
        public String getDisplayName() {
            return "Pitied Foos";
        }

        @CheckForNull
        @Override
        public String getUrlName() {
            return "foo";
        }

        @GET
        public void doBar(@QueryParameter String baz) {
            this.baz = baz;
        }

        @GET
        public void doIndex(@QueryParameter String baz) {
            this.baz = "index: " + baz;
        }
    }

}
