package dev.sakura.account.msa.callback;

@FunctionalInterface
public interface BrowserLoginCallback {
    void callback(final String accessToken);
}
