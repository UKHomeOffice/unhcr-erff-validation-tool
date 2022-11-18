package uk.gov.homeoffice.unhcr;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.junit.jupiter.api.Test;
import uk.gov.homeoffice.unhcr.version.GitHubVersionChecker;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class GitHubVersionCheckerTest {

    @Test
    void latestReleaseVersionTest() throws IOException {
        ComparableVersion latestVersion = GitHubVersionChecker.getLatestReleaseVersion();

        //that will fail if last version release tag was wrong
        assertThat(latestVersion.toString()).containsPattern(GitHubVersionChecker.VERSION_REGEX);

        System.out.println("Latest version: " + latestVersion);
        assertThat(
            latestVersion.compareTo(new ComparableVersion("0.0.001"))
        ).isEqualTo(1);
    }

    @Test
    void checkReleaseVersionNewerTest() throws IOException {
        //current version must be higher than release
        assertThat(GitHubVersionChecker.checkReleaseVersionNewer()).isFalse();
    }
}
